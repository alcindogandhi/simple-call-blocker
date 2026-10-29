package com.simplecallblocker.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.simplecallblocker.R
import com.simplecallblocker.ui.components.PlaceholderScreen

@Composable
fun SettingsScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_settings),
        description = stringResource(R.string.placeholder_settings),
    )
}
