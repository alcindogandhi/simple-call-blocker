package com.simplecallblocker.screening

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.PhoneLookup

class ContactsLookup(private val context: Context) {

    /**
     * Sem a permissão READ_CONTACTS não há como saber; trata o número como contato
     * para não bloquear chamadas legítimas.
     */
    fun isContact(number: String): Boolean {
        if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return true
        }
        val uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        return context.contentResolver.query(uri, arrayOf(PhoneLookup._ID), null, null, null)
            ?.use { it.count > 0 }
            ?: true
    }
}
