package com.gandhi.simplecallblocker.domain

enum class RuleType { EXACT, PREFIX }

/**
 * Regra de bloqueio. O [pattern] já está normalizado por [NumberNormalizer]:
 * - EXACT: E.164 (`+5511999990000`) ou, se não for um número possível, só dígitos (`4004`).
 * - PREFIX: `+` seguido de dígitos (compara com o E.164) ou só dígitos (compara com a forma nacional).
 */
data class BlockRule(
    val id: Long = 0,
    val type: RuleType,
    val pattern: String,
    val label: String? = null,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
)

enum class BlockReason { RULE_EXACT, RULE_PREFIX, HIDDEN, UNKNOWN }

/** Dados mínimos de uma chamada, independentes de `android.telecom.Call.Details`. */
data class CallInfo(
    val rawNumber: String?,
    val isIncoming: Boolean = true,
    val isPresentationAllowed: Boolean = true,
)

data class BlockingSettings(
    val blockingEnabled: Boolean = true,
    val blockHidden: Boolean = false,
    val blockUnknown: Boolean = false,
    val notifyOnBlock: Boolean = true,
    val skipCallLog: Boolean = false,
)

sealed interface CallDecision {
    data object Allow : CallDecision
    data class Block(val reason: BlockReason, val ruleId: Long? = null) : CallDecision
}
