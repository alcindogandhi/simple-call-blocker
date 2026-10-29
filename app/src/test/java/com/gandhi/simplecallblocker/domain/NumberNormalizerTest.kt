package com.gandhi.simplecallblocker.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberNormalizerTest {

    private val normalizer = NumberNormalizer("BR")

    @Test
    fun `numero exato em varios formatos vira E164`() {
        listOf(
            "11999990000",
            "(11) 99999-0000",
            "+55 11 99999-0000",
            "011 99999 0000",
        ).forEach { input ->
            assertEquals(input, "+5511999990000", normalizer.normalizeExact(input))
        }
    }

    @Test
    fun `numero estrangeiro com DDI vira E164`() {
        assertEquals("+12125550100", normalizer.normalizeExact("+1 (212) 555-0100"))
    }

    @Test
    fun `numero que nao e valido fica so com digitos`() {
        assertEquals("4004", normalizer.normalizeExact("4004"))
    }

    @Test
    fun `codigo especial 0303 e reconhecido como numero brasileiro`() {
        assertEquals("+553031234567", normalizer.normalizeExact("0303 123-4567"))
        assertEquals("0303 123 4567", normalizer.formatForDisplay("+553031234567"))
    }

    @Test
    fun `forma do historico e E164 so para numeros validos`() {
        assertEquals("+5511999990000", normalizer.canonical("011999990000"))
        assertEquals("4004", normalizer.canonical(" 4004 "))
    }

    @Test
    fun `entrada sem digitos e invalida`() {
        assertNull(normalizer.normalizeExact(""))
        assertNull(normalizer.normalizeExact("abc"))
        assertNull(normalizer.normalizePrefix("  "))
        assertNull(normalizer.normalizePrefix("+"))
    }

    @Test
    fun `prefixo descarta mascara e preserva o mais`() {
        assertEquals("0303", normalizer.normalizePrefix("03-03"))
        assertEquals("+5511", normalizer.normalizePrefix(" +55 (11) "))
    }

    @Test
    fun `numero recebido nacional tem E164 e formas locais`() {
        val number = normalizer.parseIncoming("011999990000")
        assertEquals("+5511999990000", number.e164)
        assertEquals(setOf("011999990000", "11999990000"), number.localDigits)

        val special = normalizer.parseIncoming("+553031234567")
        assertEquals(setOf("3031234567", "03031234567"), special.localDigits)
    }

    @Test
    fun `numero recebido internacional do pais padrao tem numero nacional`() {
        val number = normalizer.parseIncoming("+5511999990000")
        assertEquals("+5511999990000", number.e164)
        assertEquals(setOf("11999990000"), number.localDigits)
    }

    @Test
    fun `numero recebido estrangeiro nao tem formas locais`() {
        val number = normalizer.parseIncoming("+12125550100")
        assertEquals("+12125550100", number.e164)
        assertTrue(number.localDigits.isEmpty())
    }

    @Test
    fun `prefixo nacional nao casa com numero estrangeiro`() {
        val rio = BlockRule(type = RuleType.PREFIX, pattern = "21")
        assertFalse(normalizer.matches(rio, normalizer.parseIncoming("+12125550100")))
        assertTrue(normalizer.matches(rio, normalizer.parseIncoming("+5521988887777")))
    }

    @Test
    fun `exibicao nacional para o pais padrao e internacional para os demais`() {
        assertEquals("(11) 99999-0000", normalizer.formatForDisplay("+5511999990000"))
        assertEquals("+1 212-555-0100", normalizer.formatForDisplay("+12125550100"))
        assertEquals("4004", normalizer.formatForDisplay("4004"))
    }

    @Test
    fun `regiao invalida usa BR`() {
        assertEquals("BR", NumberNormalizer("zz").region)
        assertEquals("BR", NumberNormalizer(null).region)
        assertEquals("US", NumberNormalizer("us").region)
    }
}
