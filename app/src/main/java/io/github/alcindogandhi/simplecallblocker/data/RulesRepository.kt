package io.github.alcindogandhi.simplecallblocker.data

import io.github.alcindogandhi.simplecallblocker.data.db.BlockRuleDao
import io.github.alcindogandhi.simplecallblocker.domain.BlockRule
import io.github.alcindogandhi.simplecallblocker.domain.NumberNormalizer
import io.github.alcindogandhi.simplecallblocker.domain.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

sealed interface AddRuleResult {
    data class Added(val id: Long) : AddRuleResult
    data object Duplicate : AddRuleResult
    data object Invalid : AddRuleResult
}

class RulesRepository(
    private val dao: BlockRuleDao,
    scope: CoroutineScope,
    private val normalizer: suspend () -> NumberNormalizer,
) {
    // Cache em memória: o serviço de triagem lê daqui sem ir ao banco a cada chamada.
    private val cache = dao.observeAll().stateIn(scope, SharingStarted.Eagerly, null)

    val rules: Flow<List<BlockRule>> = cache.filterNotNull()

    /** Regras atuais; logo após o processo iniciar, espera a primeira leitura do banco. */
    suspend fun current(): List<BlockRule> = rules.first()

    suspend fun add(
        type: RuleType,
        input: String,
        label: String? = null,
        now: Long = System.currentTimeMillis(),
    ): AddRuleResult {
        val normalizer = normalizer()
        val pattern = when (type) {
            RuleType.EXACT -> normalizer.normalizeExact(input)
            RuleType.PREFIX -> normalizer.normalizePrefix(input)
        } ?: return AddRuleResult.Invalid

        val rule = BlockRule(type = type, pattern = pattern, label = label?.trim()?.ifEmpty { null }, createdAt = now)
        val id = dao.insert(rule)
        return if (id == -1L) AddRuleResult.Duplicate else AddRuleResult.Added(id)
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.delete(id)

    /** Desfaz uma exclusão, recriando a regra com o mesmo id. */
    suspend fun restore(rule: BlockRule) {
        dao.insert(rule)
    }
}
