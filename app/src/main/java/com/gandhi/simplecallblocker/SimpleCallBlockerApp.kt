package com.gandhi.simplecallblocker

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.gandhi.simplecallblocker.data.HistoryRepository
import com.gandhi.simplecallblocker.data.RulesRepository
import com.gandhi.simplecallblocker.data.db.AppDatabase
import com.gandhi.simplecallblocker.data.settings.RegionDetector
import com.gandhi.simplecallblocker.data.settings.SettingsRepository
import com.gandhi.simplecallblocker.domain.NumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first

class SimpleCallBlockerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Injeção de dependências manual: uma instância por processo, criada na [SimpleCallBlockerApp]. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database = AppDatabase.build(appContext)

    val regionDetector = RegionDetector(appContext)

    val settingsRepository = SettingsRepository(
        PreferenceDataStoreFactory.create { appContext.preferencesDataStoreFile("settings") },
    )

    val rulesRepository = RulesRepository(database.blockRuleDao(), applicationScope, ::normalizer)

    val historyRepository = HistoryRepository(database.blockedCallDao())

    /** Normalizador para o país escolhido nas configurações ou, se automático, o detectado. */
    suspend fun normalizer(): NumberNormalizer =
        NumberNormalizer(settingsRepository.settings.first().countryIso ?: regionDetector.detect())
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SimpleCallBlockerApp).container
