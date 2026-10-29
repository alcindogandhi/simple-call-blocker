package com.gandhi.simplecallblocker.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gandhi.simplecallblocker.R
import com.gandhi.simplecallblocker.ui.components.PlaceholderScreen

@Composable
fun HomeScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_home),
        description = stringResource(R.string.placeholder_home),
    )
}
