package com.simplecallblocker.domain

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.text.Collator
import java.util.Locale

data class Country(val iso: String, val name: String, val callingCode: Int)

/** Países suportados pela libphonenumber, com nome no idioma [locale] e em ordem alfabética. */
fun availableCountries(locale: Locale): List<Country> {
    val util = PhoneNumberUtil.getInstance()
    val collator = Collator.getInstance(locale)
    return util.supportedRegions
        .map { Country(it, countryName(it, locale), util.getCountryCodeForRegion(it)) }
        .sortedWith(compareBy(collator) { it.name })
}

fun countryName(iso: String, locale: Locale): String =
    Locale.Builder().setRegion(iso).build().getDisplayCountry(locale).ifBlank { iso }
