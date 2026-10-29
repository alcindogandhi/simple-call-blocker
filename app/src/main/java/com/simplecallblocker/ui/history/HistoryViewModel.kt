package com.simplecallblocker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.simplecallblocker.AppContainer
import com.simplecallblocker.appContainer
import com.simplecallblocker.domain.BlockRule
import com.simplecallblocker.ui.components.BlockedCallItem
import com.simplecallblocker.ui.components.toItem
import com.simplecallblocker.ui.rules.display
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** @property rule regra que bloqueou, se ainda existir; @property ruleDisplay como exibi-la. */
data class HistoryEntry(val call: BlockedCallItem, val rule: BlockRule?, val ruleDisplay: String?)

data class HistoryUiState(val loading: Boolean = true, val entries: List<HistoryEntry> = emptyList())

class HistoryViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<HistoryUiState> = combine(
        container.historyRepository.all,
        container.rulesRepository.rules,
        container.normalizers,
    ) { calls, rules, normalizer ->
        val rulesById = rules.associateBy { it.id }
        HistoryUiState(
            loading = false,
            entries = calls.map { call ->
                val rule = call.ruleId?.let(rulesById::get)
                HistoryEntry(call.toItem(normalizer), rule, rule?.display(normalizer))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun deleteRule(rule: BlockRule) {
        viewModelScope.launch { container.rulesRepository.delete(rule.id) }
    }

    fun restoreRule(rule: BlockRule) {
        viewModelScope.launch { container.rulesRepository.restore(rule) }
    }

    fun clear() {
        viewModelScope.launch { container.historyRepository.clear() }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { HistoryViewModel(this[APPLICATION_KEY]!!.appContainer) }
        }
    }
}
