package com.gandhi.simplecallblocker.data.settings

import android.content.Context
import android.telephony.TelephonyManager

/** Detecta o país pelo chip (preferido, por ser o país "de casa" mesmo em roaming) ou pela rede. */
class RegionDetector(private val context: Context) {

    fun detect(): String? {
        val telephony = context.getSystemService(TelephonyManager::class.java) ?: return null
        return listOf(telephony.simCountryIso, telephony.networkCountryIso)
            .firstOrNull { !it.isNullOrBlank() }
            ?.uppercase()
    }
}
