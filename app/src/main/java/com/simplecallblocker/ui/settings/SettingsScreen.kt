package com.simplecallblocker.ui.settings

import android.Manifest
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simplecallblocker.R
import com.simplecallblocker.domain.availableCountries
import com.simplecallblocker.domain.countryName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showCountryDialog by rememberSaveable { mutableStateOf(false) }

    // Permissões podem mudar fora do app; reavalia a cada retomada.
    val contactsAllowed = rememberOnResume { context.hasPermission(Manifest.permission.READ_CONTACTS) }
    val notificationsAllowed = rememberOnResume {
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }

    val openSettingsLabel = stringResource(R.string.settings_open_system)
    fun explainDenied(message: String, settingsIntent: Intent) {
        scope.launch {
            val result = snackbar.showSnackbar(message, openSettingsLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) context.startActivitySafely(settingsIntent)
        }
    }

    val contactsDeniedMessage = stringResource(R.string.settings_contacts_denied)
    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsAllowed.value = granted
        if (granted) viewModel.setBlockUnknown(true) else explainDenied(contactsDeniedMessage, context.appDetailsIntent())
    }

    val notificationsDeniedMessage = stringResource(R.string.settings_notifications_denied)
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed.value = granted
        if (granted) viewModel.setNotifyOnBlock(true)
        else explainDenied(notificationsDeniedMessage, context.notificationSettingsIntent())
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) return@Scaffold
        val settings = state.settings
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.settings_section_blocking))
            SwitchItem(
                title = stringResource(R.string.settings_block_hidden),
                summary = stringResource(R.string.settings_block_hidden_summary),
                checked = settings.blockHidden,
                onCheckedChange = viewModel::setBlockHidden,
            )
            SwitchItem(
                title = stringResource(R.string.settings_block_unknown),
                summary = stringResource(R.string.settings_block_unknown_summary),
                // Sem a permissão o serviço trata todos como contatos: a opção não tem efeito.
                checked = settings.blockUnknown && contactsAllowed.value,
                onCheckedChange = { enable ->
                    when {
                        !enable -> viewModel.setBlockUnknown(false)
                        contactsAllowed.value -> viewModel.setBlockUnknown(true)
                        else -> contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                },
            )

            SectionHeader(stringResource(R.string.settings_section_notifications))
            SwitchItem(
                title = stringResource(R.string.settings_notify),
                summary = stringResource(R.string.settings_notify_summary),
                checked = settings.notifyOnBlock && notificationsAllowed.value,
                onCheckedChange = { enable ->
                    when {
                        !enable -> viewModel.setNotifyOnBlock(false)
                        notificationsAllowed.value -> viewModel.setNotifyOnBlock(true)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> context.startActivitySafely(context.notificationSettingsIntent())
                    }
                },
            )

            SectionHeader(stringResource(R.string.settings_section_region))
            ListItem(
                modifier = Modifier.clickable { showCountryDialog = true },
                headlineContent = { Text(stringResource(R.string.settings_country)) },
                supportingContent = {
                    Text(
                        settings.countryIso?.let { countryName(it, locale) }
                            ?: stringResource(R.string.settings_country_auto, countryName(state.autoRegion, locale)),
                    )
                },
            )
            Text(
                stringResource(R.string.settings_country_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            SectionHeader(stringResource(R.string.settings_section_about))
            ListItem(
                modifier = Modifier.clickable {
                    context.startActivitySafely(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
                },
                headlineContent = { Text(stringResource(R.string.settings_screening_app)) },
                supportingContent = { Text(stringResource(R.string.settings_screening_app_summary)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(context.versionName()) },
            )
        }
    }

    if (showCountryDialog) {
        CountryDialog(
            countries = remember(locale) { availableCountries(locale) },
            selectedIso = state.settings.countryIso,
            autoRegionName = countryName(state.autoRegion, locale),
            onSelect = { viewModel.setCountry(it); showCountryDialog = false },
            onDismiss = { showCountryDialog = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column {
        HorizontalDivider(Modifier.padding(top = 8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun SwitchItem(title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun rememberOnResume(compute: () -> Boolean): MutableState<Boolean> {
    val state = remember { mutableStateOf(compute()) }
    LifecycleResumeEffect(Unit) {
        state.value = compute()
        onPauseOrDispose {}
    }
    return state
}

private fun Context.hasPermission(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

private fun Context.appDetailsIntent() =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

private fun Context.notificationSettingsIntent() =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

private fun Context.startActivitySafely(intent: Intent) {
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}

private fun Context.versionName(): String =
    packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
