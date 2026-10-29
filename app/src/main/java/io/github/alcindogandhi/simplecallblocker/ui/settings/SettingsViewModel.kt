package io.github.alcindogandhi.simplecallblocker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.alcindogandhi.simplecallblocker.AppContainer
import io.github.alcindogandhi.simplecallblocker.appContainer
import io.github.alcindogandhi.simplecallblocker.domain.BlockingSettings
import io.github.alcindogandhi.simplecallblocker.domain.NumberNormalizer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** @property autoRegion país usado no modo automático (detectado ou o padrão BR). */
data class SettingsUiState(
    val loading: Boolean = true,
    val settings: BlockingSettings = BlockingSettings(),
    val autoRegion: String = NumberNormalizer.DEFAULT_REGION,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<SettingsUiState> = container.settingsRepository.settings
        .map { SettingsUiState(false, it, NumberNormalizer(container.regionDetector.detect()).region) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val settings get() = container.settingsRepository

    fun setBlockHidden(value: Boolean) = launch { settings.setBlockHidden(value) }
    fun setBlockUnknown(value: Boolean) = launch { settings.setBlockUnknown(value) }
    fun setNotifyOnBlock(value: Boolean) = launch { settings.setNotifyOnBlock(value) }

    /** null volta para a detecção automática. */
    fun setCountry(iso: String?) = launch { settings.setCountryIso(iso) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY]!!.appContainer) }
        }
    }
}
