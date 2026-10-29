package com.simplecallblocker.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.simplecallblocker.R
import com.simplecallblocker.ui.components.PlaceholderScreen

@Composable
fun HistoryScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_history),
        description = stringResource(R.string.placeholder_history),
    )
}
