package com.simplecallblocker.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simplecallblocker.R
import com.simplecallblocker.data.AddRuleResult
import com.simplecallblocker.domain.BlockRule
import com.simplecallblocker.domain.RuleType
import com.simplecallblocker.ui.components.EmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(viewModel: RulesViewModel = viewModel(factory = RulesViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val deletedMessage = stringResource(R.string.rules_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val telemarketingLabel = stringResource(R.string.rules_telemarketing_label)
    val onDelete: (BlockRule) -> Unit = { rule ->
        viewModel.delete(rule)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar(deletedMessage, undoLabel, withDismissAction = true) == SnackbarResult.ActionPerformed) {
                viewModel.restore(rule)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_rules)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.rules_add)) },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            if (state.rules.isEmpty()) {
                item { EmptyState(stringResource(R.string.rules_empty)) }
            }
            if (state.suggestTelemarketing) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        AssistChip(
                            onClick = { viewModel.addTelemarketing(telemarketingLabel) },
                            label = { Text(stringResource(R.string.rules_suggest_telemarketing)) },
                            leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        )
                    }
                }
            }
            items(state.rules, key = { it.rule.id }) { item ->
                RuleRow(
                    item = item,
                    onToggle = { viewModel.setEnabled(item.rule, it) },
                    onDelete = { onDelete(item.rule) },
                )
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            preview = viewModel::preview,
            onSubmit = viewModel::add,
            onDismiss = { showAddDialog = false },
        )
    }
}

@Composable
private fun RuleRow(item: RuleItem, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val rule = item.rule
    val type = stringResource(if (rule.type == RuleType.EXACT) R.string.rule_type_exact else R.string.rule_type_prefix)
    ListItem(
        leadingContent = {
            Icon(if (rule.type == RuleType.EXACT) Icons.Filled.Call else Icons.AutoMirrored.Filled.List, null)
        },
        headlineContent = { Text(item.display) },
        supportingContent = { Text(listOfNotNull(type, rule.label).joinToString(" · ")) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = rule.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.rules_delete))
                }
            }
        },
    )
}

@Composable
private fun AddRuleDialog(
    preview: (RuleType, String) -> String?,
    onSubmit: suspend (RuleType, String, String) -> AddRuleResult,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(RuleType.EXACT) }
    var input by rememberSaveable { mutableStateOf("") }
    var label by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    val normalized = preview(type, input)
    val submit: () -> Unit = {
        scope.launch {
            when (onSubmit(type, input, label)) {
                is AddRuleResult.Added -> onDismiss()
                AddRuleResult.Duplicate -> error = R.string.rules_error_duplicate
                AddRuleResult.Invalid -> error = R.string.rules_error_invalid
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rules_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    RuleType.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = type == option,
                            onClick = { type = option; error = null },
                            shape = SegmentedButtonDefaults.itemShape(index, RuleType.entries.size),
                        ) {
                            Text(stringResource(if (option == RuleType.EXACT) R.string.rule_type_exact else R.string.rule_type_prefix))
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { value -> input = value.filter { it.isDigit() || it in ALLOWED_SYMBOLS }; error = null },
                    label = {
                        Text(stringResource(if (type == RuleType.EXACT) R.string.rules_field_number else R.string.rules_field_prefix))
                    },
                    placeholder = {
                        Text(stringResource(if (type == RuleType.EXACT) R.string.rules_hint_number else R.string.rules_hint_prefix))
                    },
                    isError = error != null || (normalized == null && input.any(Char::isDigit)),
                    supportingText = {
                        when {
                            error != null -> Text(stringResource(error!!))
                            normalized == null && input.any(Char::isDigit) -> Text(
                                stringResource(
                                    if (type == RuleType.EXACT) R.string.rules_error_exact_length
                                    else R.string.rules_error_prefix_length,
                                ),
                            )
                            normalized != null -> Text(
                                if (type == RuleType.EXACT) stringResource(R.string.rules_preview_exact, normalized)
                                else stringResource(R.string.rules_preview_prefix, normalized),
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it.take(MAX_LABEL_LENGTH) },
                    label = { Text(stringResource(R.string.rules_field_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = submit, enabled = normalized != null) { Text(stringResource(R.string.rules_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Máscara que o usuário pode digitar ou colar; o resto é descartado. */
private const val ALLOWED_SYMBOLS = "+-(). "
private const val MAX_LABEL_LENGTH = 40
