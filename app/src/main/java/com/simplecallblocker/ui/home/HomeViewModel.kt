package com.simplecallblocker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simplecallblocker.AppContainer
import com.simplecallblocker.appContainer
import com.simplecallblocker.ui.components.BlockedCallItem
import com.simplecallblocker.ui.components.toItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    val loading: Boolean = true,
    val blockingEnabled: Boolean = true,
    val blockedToday: Int = 0,
    val activeRules: Int = 0,
    val recent: List<BlockedCallItem> = emptyList(),
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val state: StateFlow<HomeUiState> = combine(
        container.settingsRepository.settings,
        container.historyRepository.countSince(startOfToday),
        container.rulesRepository.rules,
        container.historyRepository.recent(RECENT_LIMIT),
        container.normalizers,
    ) { settings, blockedToday, rules, recent, normalizer ->
        HomeUiState(
            loading = false,
            blockingEnabled = settings.blockingEnabled,
            blockedToday = blockedToday,
            activeRules = rules.count { it.enabled },
            recent = recent.map { it.toItem(normalizer) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setBlockingEnabled(enabled) }
    }

    companion object {
        private const val RECENT_LIMIT = 5

        val Factory = viewModelFactory {
            initializer { HomeViewModel(this[APPLICATION_KEY]!!.appContainer) }
        }
    }
}
