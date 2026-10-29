package com.simplecallblocker

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.simplecallblocker.data.HistoryRepository
import com.simplecallblocker.data.RulesRepository
import com.simplecallblocker.data.db.AppDatabase
import com.simplecallblocker.data.settings.RegionDetector
import com.simplecallblocker.data.settings.SettingsRepository
import com.simplecallblocker.domain.BlockingSettings
import com.simplecallblocker.domain.NumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SimpleCallBlockerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // O processo costuma ser iniciado pelo próprio CallScreeningService, que tem poucos segundos
        // para responder: pré-carrega as configurações e os metadados da libphonenumber.
        container.applicationScope.launch {
            container.normalizer().parseIncoming("+5511999990000")
        }
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
    suspend fun normalizer(): NumberNormalizer = normalizerFor(settingsRepository.settings.first())

    fun normalizerFor(settings: BlockingSettings): NumberNormalizer =
        NumberNormalizer(settings.countryIso ?: regionDetector.detect())
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SimpleCallBlockerApp).container
