package com.simplecallblocker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.simplecallblocker.R
import com.simplecallblocker.domain.BlockReason
import com.simplecallblocker.domain.BlockedCall
import com.simplecallblocker.domain.NumberNormalizer
import com.simplecallblocker.screening.labelRes
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Registro do histórico pronto para exibição. */
data class BlockedCallItem(
    val id: Long,
    val number: String?,
    val displayNumber: String?,
    val reason: BlockReason,
    val ruleId: Long?,
    val timestamp: Long,
)

fun BlockedCall.toItem(normalizer: NumberNormalizer) = BlockedCallItem(
    id = id,
    number = number,
    displayNumber = number?.let(normalizer::formatForDisplay),
    reason = reason,
    ruleId = ruleId,
    timestamp = timestamp,
)

@Composable
fun BlockedCallRow(item: BlockedCallItem, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    ListItem(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        leadingContent = { Icon(item.reason.icon(), contentDescription = null) },
        headlineContent = { Text(item.displayNumber ?: stringResource(R.string.hidden_number)) },
        supportingContent = {
            Text("${stringResource(item.reason.labelRes())} · ${formatTimestamp(item.timestamp)}")
        },
    )
}

private fun BlockReason.icon(): ImageVector = when (this) {
    BlockReason.RULE_EXACT -> Icons.Filled.Call
    BlockReason.RULE_PREFIX -> Icons.AutoMirrored.Filled.List
    BlockReason.HIDDEN -> Icons.Filled.Lock
    BlockReason.UNKNOWN -> Icons.Filled.Person
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val zone = ZoneId.systemDefault()
    val dateTime = Instant.ofEpochMilli(timestamp).atZone(zone)
    val today = LocalDate.now(zone)
    val time = dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return when (dateTime.toLocalDate()) {
        today -> stringResource(R.string.time_today, time)
        today.minusDays(1) -> stringResource(R.string.time_yesterday, time)
        else -> dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
    }
}
