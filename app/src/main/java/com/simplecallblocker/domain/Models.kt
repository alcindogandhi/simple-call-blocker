package com.simplecallblocker.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RuleType { EXACT, PREFIX }

/**
 * Regra de bloqueio. O [pattern] já está normalizado por [NumberNormalizer]:
 * - EXACT: E.164 (`+5511999990000`) ou, se não for um número válido, só dígitos (`4004`).
 * - PREFIX: `+` seguido de dígitos (compara com o E.164) ou só dígitos (compara com a forma nacional).
 */
@Entity(
    tableName = "block_rules",
    indices = [Index(value = ["type", "pattern"], unique = true)],
)
data class BlockRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RuleType,
    val pattern: String,
    val label: String? = null,
    val enabled: Boolean = true,
    val createdAt: Long = 0,
)

enum class BlockReason { RULE_EXACT, RULE_PREFIX, HIDDEN, UNKNOWN }

/** Registro do histórico. [ruleId] não é chave estrangeira: o registro sobrevive à exclusão da regra. */
@Entity(tableName = "blocked_calls", indices = [Index("timestamp")])
data class BlockedCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String?,
    val timestamp: Long,
    val reason: BlockReason,
    val ruleId: Long? = null,
)

/** Dados mínimos de uma chamada, independentes de `android.telecom.Call.Details`. */
data class CallInfo(
    val rawNumber: String?,
    val isIncoming: Boolean = true,
    val isPresentationAllowed: Boolean = true,
)

/** @property countryIso país escolhido pelo usuário; null = detectar pelo chip/rede. */
data class BlockingSettings(
    val blockingEnabled: Boolean = true,
    val blockHidden: Boolean = false,
    val blockUnknown: Boolean = false,
    val notifyOnBlock: Boolean = true,
    val countryIso: String? = null,
)

sealed interface CallDecision {
    data object Allow : CallDecision
    data class Block(val reason: BlockReason, val ruleId: Long? = null) : CallDecision
}
