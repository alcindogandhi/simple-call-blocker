package com.gandhi.simplecallblocker.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gandhi.simplecallblocker.R
import com.gandhi.simplecallblocker.ui.components.PlaceholderScreen

@Composable
fun HistoryScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_history),
        description = stringResource(R.string.placeholder_history),
    )
}
