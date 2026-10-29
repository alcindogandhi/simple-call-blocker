package com.gandhi.simplecallblocker.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gandhi.simplecallblocker.domain.BlockingSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<BlockingSettings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    suspend fun setBlockingEnabled(value: Boolean) = set(Keys.BLOCKING_ENABLED, value)
    suspend fun setBlockHidden(value: Boolean) = set(Keys.BLOCK_HIDDEN, value)
    suspend fun setBlockUnknown(value: Boolean) = set(Keys.BLOCK_UNKNOWN, value)
    suspend fun setNotifyOnBlock(value: Boolean) = set(Keys.NOTIFY_ON_BLOCK, value)
    suspend fun setSkipCallLog(value: Boolean) = set(Keys.SKIP_CALL_LOG, value)

    /** [iso] no formato ISO 3166-1 alfa-2 (`BR`, `PT`); null volta para a detecção automática. */
    suspend fun setCountryIso(iso: String?) {
        dataStore.edit {
            if (iso.isNullOrBlank()) it.remove(Keys.COUNTRY_ISO) else it[Keys.COUNTRY_ISO] = iso.uppercase()
        }
    }

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }

    private fun Preferences.toSettings(): BlockingSettings {
        val defaults = BlockingSettings()
        return BlockingSettings(
            blockingEnabled = this[Keys.BLOCKING_ENABLED] ?: defaults.blockingEnabled,
            blockHidden = this[Keys.BLOCK_HIDDEN] ?: defaults.blockHidden,
            blockUnknown = this[Keys.BLOCK_UNKNOWN] ?: defaults.blockUnknown,
            notifyOnBlock = this[Keys.NOTIFY_ON_BLOCK] ?: defaults.notifyOnBlock,
            skipCallLog = this[Keys.SKIP_CALL_LOG] ?: defaults.skipCallLog,
            countryIso = this[Keys.COUNTRY_ISO],
        )
    }

    private object Keys {
        val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        val BLOCK_HIDDEN = booleanPreferencesKey("block_hidden")
        val BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown")
        val NOTIFY_ON_BLOCK = booleanPreferencesKey("notify_on_block")
        val SKIP_CALL_LOG = booleanPreferencesKey("skip_call_log")
        val COUNTRY_ISO = stringPreferencesKey("country_iso")
    }
}
