package com.simplecallblocker.domain

/**
 * Decide se uma chamada deve ser bloqueada. Ordem de avaliação (SPEC §3):
 * saída → bloqueio desligado → oculto → regra exata → prefixo → desconhecido.
 *
 * Qualquer exceção resulta em [CallDecision.Allow]: nunca perder uma chamada legítima por bug.
 */
class CallDecisionEngine(private val normalizer: NumberNormalizer) {

    /**
     * @param isContact consultado só quando "bloquear desconhecidos" está ativo e nenhuma regra casou,
     * pois exige acesso aos contatos.
     */
    fun decide(
        call: CallInfo,
        rules: List<BlockRule>,
        settings: BlockingSettings,
        isContact: (String) -> Boolean,
    ): CallDecision =
        try {
            evaluate(call, rules, settings, isContact)
        } catch (_: Exception) {
            CallDecision.Allow
        }

    private fun evaluate(
        call: CallInfo,
        rules: List<BlockRule>,
        settings: BlockingSettings,
        isContact: (String) -> Boolean,
    ): CallDecision {
        if (!call.isIncoming || !settings.blockingEnabled) return CallDecision.Allow

        val raw = call.rawNumber?.takeIf { it.isNotBlank() }
        if (raw == null || !call.isPresentationAllowed) {
            // Quem só aceita contatos também recusa números ocultos, que nunca podem ser conferidos.
            val blockHidden = settings.blockHidden || settings.blockUnknown
            return if (blockHidden) CallDecision.Block(BlockReason.HIDDEN) else CallDecision.Allow
        }

        val number = normalizer.parseIncoming(raw)
        val activeRules = rules.filter { it.enabled }

        activeRules.firstOrNull { it.type == RuleType.EXACT && normalizer.matches(it, number) }
            ?.let { return CallDecision.Block(BlockReason.RULE_EXACT, it.id) }

        activeRules.firstOrNull { it.type == RuleType.PREFIX && normalizer.matches(it, number) }
            ?.let { return CallDecision.Block(BlockReason.RULE_PREFIX, it.id) }

        if (settings.blockUnknown && !isContact(raw)) return CallDecision.Block(BlockReason.UNKNOWN)

        return CallDecision.Allow
    }
}
