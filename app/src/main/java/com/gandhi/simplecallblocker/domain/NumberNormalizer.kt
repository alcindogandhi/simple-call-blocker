package com.gandhi.simplecallblocker.domain

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

/**
 * Formas de um número recebido usadas na comparação com as regras.
 *
 * @property e164 forma internacional (`+5511999990000`), ou null se o número não for possível.
 * @property localDigits formas nacionais em dígitos: o número como chegou (se não veio com `+`) e,
 * só quando o número é do país padrão, o número nacional significativo (`11999990000`) e os dígitos
 * do formato nacional, que preservam o `0` de códigos especiais (`+553031234567` → `03031234567`).
 */
data class IncomingNumber(val e164: String?, val localDigits: Set<String>)

class NumberNormalizer(defaultRegion: String?) {

    private val util = PhoneNumberUtil.getInstance()

    val region: String = defaultRegion?.uppercase()
        ?.takeIf { it in util.supportedRegions }
        ?: DEFAULT_REGION

    private val regionCountryCode = util.getCountryCodeForRegion(region)

    /** Padrão para regra exata: E.164 se o número for válido, senão só dígitos. Null se não houver dígitos. */
    fun normalizeExact(input: String): String? =
        toE164(parse(input)?.takeIf { util.isValidNumber(it) }) ?: digitsOnly(input).ifEmpty { null }

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
                add(digitsOnly(util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)))
            }
        }
        return IncomingNumber(toE164(parsed?.takeIf { util.isPossibleNumber(it) }), localDigits)
    }

    /** Forma para guardar no histórico: E.164 se o número for válido, senão como chegou. */
    fun canonical(raw: String): String =
        toE164(parse(raw)?.takeIf { util.isValidNumber(it) }) ?: raw.trim()

    /** Formato para exibição: nacional para números do país padrão, internacional para os demais. */
    fun formatForDisplay(number: String): String {
        val parsed = parse(number)?.takeIf { util.isValidNumber(it) } ?: return number
        val format = if (parsed.countryCode == regionCountryCode) {
            PhoneNumberUtil.PhoneNumberFormat.NATIONAL
        } else {
            PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
        }
        return util.format(parsed, format)
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
        parsed?.let { util.format(it, PhoneNumberUtil.PhoneNumberFormat.E164) }

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
