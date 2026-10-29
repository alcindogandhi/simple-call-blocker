package com.simplecallblocker.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.simplecallblocker.R
import com.simplecallblocker.ui.components.PlaceholderScreen

@Composable
fun HomeScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_home),
        description = stringResource(R.string.placeholder_home),
    )
}
