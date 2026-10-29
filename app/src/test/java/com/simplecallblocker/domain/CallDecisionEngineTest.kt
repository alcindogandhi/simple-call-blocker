package com.simplecallblocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CallDecisionEngineTest {

    private val normalizer = NumberNormalizer("BR")
    private val engine = CallDecisionEngine(normalizer)

    private val exactRule = BlockRule(id = 1, type = RuleType.EXACT, pattern = "+5511999990000")
    private val telemarketingRule = BlockRule(id = 2, type = RuleType.PREFIX, pattern = "0303")
    private val usaRule = BlockRule(id = 3, type = RuleType.PREFIX, pattern = "+1")
    private val rules = listOf(exactRule, telemarketingRule, usaRule)

    private val defaults = BlockingSettings()
    private val notAContact: (String) -> Boolean = { false }

    private fun decide(
        number: String?,
        settings: BlockingSettings = defaults,
        rules: List<BlockRule> = this.rules,
        isIncoming: Boolean = true,
        isPresentationAllowed: Boolean = true,
        isContact: (String) -> Boolean = notAContact,
    ) = engine.decide(CallInfo(number, isIncoming, isPresentationAllowed), rules, settings, isContact)

    @Test
    fun `numero exato bloqueia em qualquer formato`() {
        listOf("+5511999990000", "11999990000", "011999990000", "(11) 99999-0000").forEach {
            assertEquals(it, CallDecision.Block(BlockReason.RULE_EXACT, 1), decide(it))
        }
    }

    @Test
    fun `numero diferente e permitido`() {
        assertEquals(CallDecision.Allow, decide("11888880000"))
    }

    @Test
    fun `regra desativada e ignorada`() {
        val disabled = listOf(exactRule.copy(enabled = false))
        assertEquals(CallDecision.Allow, decide("11999990000", rules = disabled))
    }

    @Test
    fun `prefixo 0303 bloqueia telemarketing nacional ou internacional`() {
        assertEquals(CallDecision.Block(BlockReason.RULE_PREFIX, 2), decide("03031234567"))
        assertEquals(CallDecision.Block(BlockReason.RULE_PREFIX, 2), decide("+553031234567"))
    }

    @Test
    fun `numero 0303 cadastrado como exato bloqueia`() {
        val rule = BlockRule(id = 5, type = RuleType.EXACT, pattern = normalizer.normalizeExact("0303 123 4567")!!)
        assertEquals(CallDecision.Block(BlockReason.RULE_EXACT, 5), decide("03031234567", rules = listOf(rule)))
    }

    @Test
    fun `prefixo de DDI bloqueia so o pais`() {
        assertEquals(CallDecision.Block(BlockReason.RULE_PREFIX, 3), decide("+12125550100"))
        assertEquals(CallDecision.Allow, decide("+5521988887777"))
    }

    @Test
    fun `prefixo de DDD casa com numero vindo com ou sem DDI`() {
        val ddd = listOf(BlockRule(id = 4, type = RuleType.PREFIX, pattern = "21"))
        assertEquals(CallDecision.Block(BlockReason.RULE_PREFIX, 4), decide("+5521988887777", rules = ddd))
        assertEquals(CallDecision.Block(BlockReason.RULE_PREFIX, 4), decide("21988887777", rules = ddd))
    }

    @Test
    fun `regra exata tem prioridade sobre prefixo`() {
        val overlapping = listOf(
            BlockRule(id = 10, type = RuleType.PREFIX, pattern = "+5511"),
            exactRule,
        )
        assertEquals(CallDecision.Block(BlockReason.RULE_EXACT, 1), decide("11999990000", rules = overlapping))
    }

    @Test
    fun `numero oculto bloqueado so com a opcao ativa`() {
        val blockHidden = defaults.copy(blockHidden = true)
        assertEquals(CallDecision.Allow, decide(null))
        assertEquals(CallDecision.Block(BlockReason.HIDDEN), decide(null, blockHidden))
        assertEquals(CallDecision.Block(BlockReason.HIDDEN), decide("", blockHidden))
        assertEquals(
            CallDecision.Block(BlockReason.HIDDEN),
            decide("11888880000", blockHidden, isPresentationAllowed = false),
        )
    }

    @Test
    fun `desconhecido bloqueado e contato permitido`() {
        val blockUnknown = defaults.copy(blockUnknown = true)
        assertEquals(CallDecision.Block(BlockReason.UNKNOWN), decide("11888880000", blockUnknown))
        assertEquals(CallDecision.Allow, decide("11888880000", blockUnknown, isContact = { true }))
    }

    @Test
    fun `regra bloqueia mesmo se o numero for contato`() {
        val blockUnknown = defaults.copy(blockUnknown = true)
        assertEquals(
            CallDecision.Block(BlockReason.RULE_EXACT, 1),
            decide("11999990000", blockUnknown, isContact = { true }),
        )
    }

    @Test
    fun `contatos nao sao consultados sem bloquear desconhecidos`() {
        var consulted = false
        decide("11888880000", isContact = { consulted = true; true })
        assertFalse(consulted)
    }

    @Test
    fun `chamada de saida e permitida`() {
        assertEquals(CallDecision.Allow, decide("11999990000", isIncoming = false))
    }

    @Test
    fun `bloqueio geral desligado permite tudo`() {
        val off = BlockingSettings(blockingEnabled = false, blockHidden = true, blockUnknown = true)
        assertEquals(CallDecision.Allow, decide("11999990000", off))
        assertEquals(CallDecision.Allow, decide(null, off))
    }

    @Test
    fun `erro durante a decisao permite a chamada`() {
        val blockUnknown = defaults.copy(blockUnknown = true)
        assertEquals(
            CallDecision.Allow,
            decide("11888880000", blockUnknown, isContact = { error("falha ao ler contatos") }),
        )
    }
}
