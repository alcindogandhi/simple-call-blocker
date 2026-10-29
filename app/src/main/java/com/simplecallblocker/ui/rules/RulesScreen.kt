package com.simplecallblocker.ui.rules

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.simplecallblocker.R
import com.simplecallblocker.ui.components.PlaceholderScreen

@Composable
fun RulesScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_rules),
        description = stringResource(R.string.placeholder_rules),
    )
}
