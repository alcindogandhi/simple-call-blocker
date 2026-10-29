package com.simplecallblocker.ui.history

import android.content.ClipData
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simplecallblocker.R
import com.simplecallblocker.ui.components.BlockedCallRow
import com.simplecallblocker.ui.components.EmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    val copiedMessage = stringResource(R.string.history_copied)
    val ruleRemovedMessage = stringResource(R.string.history_rule_removed)
    val undoLabel = stringResource(R.string.action_undo)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history)) },
                actions = {
                    if (state.entries.isNotEmpty()) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.history_clear))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.entries.isEmpty()) {
                item { EmptyState(stringResource(R.string.history_empty)) }
            }
            items(state.entries, key = { it.call.id }) { entry ->
                BlockedCallRow(entry.call, onClick = { selectedId = entry.call.id })
            }
        }
    }

    val selected = state.entries.firstOrNull { it.call.id == selectedId }
    if (selected != null) {
        ModalBottomSheet(onDismissRequest = { selectedId = null }) {
            Text(
                selected.call.displayNumber ?: stringResource(R.string.hidden_number),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            selected.call.number?.let { number ->
                ListItem(
                    modifier = Modifier.clickable {
                        selectedId = null
                        scope.launch {
                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(number, number)))
                            // A partir do Android 13 o próprio sistema confirma a cópia.
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) snackbar.showSnackbar(copiedMessage)
                        }
                    },
                    headlineContent = { Text(stringResource(R.string.history_copy)) },
                )
            }
            selected.rule?.let { rule ->
                ListItem(
                    modifier = Modifier.clickable {
                        selectedId = null
                        viewModel.deleteRule(rule)
                        scope.launch {
                            val result = snackbar.showSnackbar(ruleRemovedMessage, undoLabel, withDismissAction = true)
                            if (result == SnackbarResult.ActionPerformed) viewModel.restoreRule(rule)
                        }
                    },
                    headlineContent = { Text(stringResource(R.string.history_remove_rule, selected.ruleDisplay.orEmpty())) },
                    supportingContent = rule.label?.let { { Text(it) } },
                )
            }
            if (selected.call.number == null && selected.rule == null) {
                Text(
                    stringResource(R.string.history_no_actions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(pluralStringResource(R.plurals.history_clear_body, state.entries.size, state.entries.size)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clear(); confirmClear = false }) {
                    Text(stringResource(R.string.history_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
