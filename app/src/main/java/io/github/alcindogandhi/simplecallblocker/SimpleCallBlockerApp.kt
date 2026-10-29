package io.github.alcindogandhi.simplecallblocker

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.alcindogandhi.simplecallblocker.data.HistoryRepository
import io.github.alcindogandhi.simplecallblocker.data.RulesRepository
import io.github.alcindogandhi.simplecallblocker.data.db.AppDatabase
import io.github.alcindogandhi.simplecallblocker.data.settings.RegionDetector
import io.github.alcindogandhi.simplecallblocker.data.settings.SettingsRepository
import io.github.alcindogandhi.simplecallblocker.domain.BlockingSettings
import io.github.alcindogandhi.simplecallblocker.domain.NumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    /** Normalizador atualizado a cada mudança de país, para as telas reformatarem os números. */
    val normalizers: Flow<NumberNormalizer> = settingsRepository.settings
        .map(::normalizerFor)
        .distinctUntilChanged { old, new -> old.region == new.region }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as SimpleCallBlockerApp).container
