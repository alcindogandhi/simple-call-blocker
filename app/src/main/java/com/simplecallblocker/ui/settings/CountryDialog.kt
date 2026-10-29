package com.simplecallblocker.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.simplecallblocker.R
import com.simplecallblocker.domain.Country
import java.text.Normalizer

/** Lista pesquisável de países; a primeira opção (null) é a detecção automática. */
@Composable
fun CountryDialog(
    countries: List<Country>,
    selectedIso: String?,
    autoRegionName: String,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, countries) {
        val needle = query.normalizedForSearch()
        if (needle.isEmpty()) countries
        else countries.filter { it.name.normalizedForSearch().contains(needle) || "+${it.callingCode}".startsWith(needle) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.fillMaxHeight(0.85f)) {
            Column {
                // Fechar fica no topo: um botão no rodapé ficaria escondido atrás do teclado.
                Row(
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.settings_country),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.settings_country_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
                HorizontalDivider()
                LazyColumn {
                    if (query.isBlank()) {
                        item {
                            CountryOption(
                                title = stringResource(R.string.settings_country_auto_option),
                                subtitle = stringResource(R.string.settings_country_auto_detail, autoRegionName),
                                selected = selectedIso == null,
                                onClick = { onSelect(null) },
                            )
                        }
                    }
                    items(filtered, key = { it.iso }) { country ->
                        CountryOption(
                            title = country.name,
                            subtitle = "+${country.callingCode}",
                            selected = selectedIso == country.iso,
                            onClick = { onSelect(country.iso) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
    )
}

private fun String.normalizedForSearch(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
