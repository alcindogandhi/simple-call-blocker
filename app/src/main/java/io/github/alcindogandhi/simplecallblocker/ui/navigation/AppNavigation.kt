package io.github.alcindogandhi.simplecallblocker.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.alcindogandhi.simplecallblocker.R
import io.github.alcindogandhi.simplecallblocker.ui.history.HistoryScreen
import io.github.alcindogandhi.simplecallblocker.ui.home.HomeScreen
import io.github.alcindogandhi.simplecallblocker.ui.rules.RulesScreen
import io.github.alcindogandhi.simplecallblocker.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object RulesRoute
@Serializable data object HistoryRoute
@Serializable data object SettingsRoute

private enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    Home(HomeRoute, Icons.Filled.Home, R.string.nav_home),
    Rules(RulesRoute, Icons.AutoMirrored.Filled.List, R.string.nav_rules),
    History(HistoryRoute, Icons.Filled.DateRange, R.string.nav_history),
    Settings(SettingsRoute, Icons.Filled.Settings, R.string.nav_settings),
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy
                        ?.any { it.hasRoute(destination.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navController.navigateTopLevel(destination.route) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            // Cada tela tem seu próprio Scaffold: evita somar de novo as margens da barra de navegação.
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            composable<HomeRoute> { HomeScreen(onSeeAllHistory = { navController.navigateTopLevel(HistoryRoute) }) }
            composable<RulesRoute> { RulesScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

/** Troca de aba preservando o estado de cada uma, sem empilhar destinos. */
private fun NavController.navigateTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
