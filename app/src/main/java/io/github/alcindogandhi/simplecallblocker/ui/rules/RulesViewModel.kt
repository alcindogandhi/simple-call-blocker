package io.github.alcindogandhi.simplecallblocker.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.alcindogandhi.simplecallblocker.AppContainer
import io.github.alcindogandhi.simplecallblocker.appContainer
import io.github.alcindogandhi.simplecallblocker.data.AddRuleResult
import io.github.alcindogandhi.simplecallblocker.domain.BlockRule
import io.github.alcindogandhi.simplecallblocker.domain.NumberNormalizer
import io.github.alcindogandhi.simplecallblocker.domain.RuleType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RuleItem(val rule: BlockRule, val display: String)

data class RulesUiState(
    val loading: Boolean = true,
    val rules: List<RuleItem> = emptyList(),
    /** Sugere o prefixo 0303 (telemarketing no Brasil) enquanto ele não estiver cadastrado. */
    val suggestTelemarketing: Boolean = false,
)

class RulesViewModel(private val container: AppContainer) : ViewModel() {

    private val normalizer: StateFlow<NumberNormalizer?> =
        container.normalizers.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val state: StateFlow<RulesUiState> = combine(container.rulesRepository.rules, container.normalizers) { rules, normalizer ->
        RulesUiState(
            loading = false,
            rules = rules.map { RuleItem(it, it.display(normalizer)) },
            suggestTelemarketing = normalizer.region == "BR" &&
                rules.none { it.type == RuleType.PREFIX && it.pattern == TELEMARKETING_PREFIX },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RulesUiState())

    /** Como a entrada será gravada, para o usuário conferir antes de confirmar. Null se inválida. */
    fun preview(type: RuleType, input: String): String? {
        val normalizer = normalizer.value ?: return null
        return when (type) {
            RuleType.EXACT -> normalizer.normalizeExact(input)?.let(normalizer::formatForDisplay)
            RuleType.PREFIX -> normalizer.normalizePrefix(input)
        }
    }

    fun exampleNumber(): String? = normalizer.value?.exampleNumber()

    suspend fun add(type: RuleType, input: String, label: String): AddRuleResult =
        container.rulesRepository.add(type, input, label)

    fun addTelemarketing(label: String) {
        viewModelScope.launch { container.rulesRepository.add(RuleType.PREFIX, TELEMARKETING_PREFIX, label) }
    }

    fun setEnabled(rule: BlockRule, enabled: Boolean) {
        viewModelScope.launch { container.rulesRepository.setEnabled(rule.id, enabled) }
    }

    fun delete(rule: BlockRule) {
        viewModelScope.launch { container.rulesRepository.delete(rule.id) }
    }

    fun restore(rule: BlockRule) {
        viewModelScope.launch { container.rulesRepository.restore(rule) }
    }

    companion object {
        private const val TELEMARKETING_PREFIX = "0303"

        val Factory = viewModelFactory {
            initializer { RulesViewModel(this[APPLICATION_KEY]!!.appContainer) }
        }
    }
}

fun BlockRule.display(normalizer: NumberNormalizer): String = when (type) {
    RuleType.EXACT -> normalizer.formatForDisplay(pattern)
    RuleType.PREFIX -> pattern
}
