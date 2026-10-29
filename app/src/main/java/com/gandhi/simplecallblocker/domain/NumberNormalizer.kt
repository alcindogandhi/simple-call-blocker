package com.gandhi.simplecallblocker.domain

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

/**
 * Formas de um número recebido usadas na comparação com as regras.
 *
 * @property e164 forma internacional (`+5511999990000`), ou null se o número não for possível.
 * @property localDigits formas nacionais em dígitos: o número como chegou (se não veio com `+`)
 * e o número nacional significativo (`11999990000`), este só quando o número é do país padrão.
 */
data class IncomingNumber(val e164: String?, val localDigits: Set<String>)

class NumberNormalizer(defaultRegion: String?) {

    private val util = PhoneNumberUtil.getInstance()

    val region: String = defaultRegion?.uppercase()
        ?.takeIf { it in util.supportedRegions }
        ?: DEFAULT_REGION

    private val regionCountryCode = util.getCountryCodeForRegion(region)

    /** Padrão para regra exata: E.164 quando possível, senão só dígitos. Null se não houver dígitos. */
    fun normalizeExact(input: String): String? {
        return toE164(parse(input)) ?: digitsOnly(input).ifEmpty { null }
    }

    /** Padrão para regra de prefixo: mantém o `+` inicial e descarta máscara. Null se não houver dígitos. */
    fun normalizePrefix(input: String): String? {
        val trimmed = input.trim()
        val digits = digitsOnly(trimmed).ifEmpty { return null }
        return if (trimmed.startsWith("+")) "+$digits" else digits
    }

    fun parseIncoming(raw: String): IncomingNumber {
        val trimmed = raw.trim()
        val parsed = parse(trimmed)
        val localDigits = buildSet {
            if (!trimmed.startsWith("+")) digitsOnly(trimmed).takeIf { it.isNotEmpty() }?.let(::add)
            if (parsed != null && parsed.countryCode == regionCountryCode) {
                add(util.getNationalSignificantNumber(parsed))
            }
        }
        return IncomingNumber(toE164(parsed), localDigits)
    }

    fun matches(rule: BlockRule, number: IncomingNumber): Boolean {
        val pattern = rule.pattern
        val international = pattern.startsWith("+")
        return when (rule.type) {
            RuleType.EXACT ->
                if (international) number.e164 == pattern else pattern in number.localDigits
            RuleType.PREFIX ->
                if (international) number.e164?.startsWith(pattern) == true
                else number.localDigits.any { it.startsWith(pattern) }
        }
    }

    private fun toE164(parsed: PhoneNumber?): String? =
        parsed?.takeIf { util.isPossibleNumber(it) }
            ?.let { util.format(it, PhoneNumberUtil.PhoneNumberFormat.E164) }

    private fun parse(input: String): PhoneNumber? =
        try {
            util.parse(input, region)
        } catch (_: NumberParseException) {
            null
        }

    private fun digitsOnly(input: String): String = input.filter { it in '0'..'9' }

    companion object {
        const val DEFAULT_REGION = "BR"
    }
}
