package com.gandhi.simplecallblocker.ui.rules

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gandhi.simplecallblocker.R
import com.gandhi.simplecallblocker.ui.components.PlaceholderScreen

@Composable
fun RulesScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_rules),
        description = stringResource(R.string.placeholder_rules),
    )
}
