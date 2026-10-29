package io.github.alcindogandhi.simplecallblocker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.alcindogandhi.simplecallblocker.screening.ScreeningRole
import io.github.alcindogandhi.simplecallblocker.screening.ScreeningRoleStatus
import io.github.alcindogandhi.simplecallblocker.ui.navigation.AppNavigation
import io.github.alcindogandhi.simplecallblocker.ui.onboarding.OnboardingScreen

/** Mostra o onboarding enquanto o app não tiver o papel de triagem; ele pode ser revogado a qualquer momento. */
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val role = remember { ScreeningRole(context) }
    var status by remember { mutableStateOf(role.status()) }

    LifecycleResumeEffect(role) {
        status = role.status()
        onPauseOrDispose {}
    }

    if (status == ScreeningRoleStatus.HELD) {
        AppNavigation()
    } else {
        OnboardingScreen(
            unavailable = status == ScreeningRoleStatus.UNAVAILABLE,
            requestIntent = role::requestIntent,
            onRequestFinished = { status = role.status() },
        )
    }
}
