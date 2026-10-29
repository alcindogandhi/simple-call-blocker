package io.github.alcindogandhi.simplecallblocker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.alcindogandhi.simplecallblocker.R
import io.github.alcindogandhi.simplecallblocker.ui.components.BlockedCallRow
import io.github.alcindogandhi.simplecallblocker.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onSeeAllHistory: () -> Unit, viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { padding ->
        if (state.loading) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                ProtectionCard(
                    enabled = state.blockingEnabled,
                    onToggle = viewModel::setBlockingEnabled,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            item {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard(stringResource(R.string.home_blocked_today), state.blockedToday, Modifier.weight(1f))
                    StatCard(stringResource(R.string.home_active_rules), state.activeRules, Modifier.weight(1f))
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.home_recent),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.recent.isNotEmpty()) {
                        TextButton(onClick = onSeeAllHistory) { Text(stringResource(R.string.home_see_all)) }
                    }
                }
            }
            if (state.recent.isEmpty()) {
                item { EmptyState(stringResource(R.string.home_recent_empty)) }
            }
            items(state.recent, key = { it.id }) { BlockedCallRow(it) }
        }
    }
}

@Composable
private fun ProtectionCard(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = if (enabled) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Card(modifier = modifier.fillMaxWidth(), colors = colors) {
        Row(
            modifier = Modifier
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onToggle)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (enabled) R.string.home_protection_on else R.string.home_protection_off),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(if (enabled) R.string.home_protection_on_body else R.string.home_protection_off_body),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Switch(checked = enabled, onCheckedChange = null, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
