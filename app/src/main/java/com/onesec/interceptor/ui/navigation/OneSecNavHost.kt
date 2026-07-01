package com.onesec.interceptor.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onesec.interceptor.ui.appselection.AppSelectionScreen
import com.onesec.interceptor.ui.onboarding.OnboardingScreen
import com.onesec.interceptor.ui.settings.SettingsScreen
import com.onesec.interceptor.ui.stats.StatsScreen

@Composable
fun OneSecNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.STATUS.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.STATUS.route) { OnboardingScreen() }
            composable(Destination.APPS.route) { AppSelectionScreen() }
            composable(Destination.STATS.route) { StatsScreen() }
            composable(Destination.SETTINGS.route) { SettingsScreen() }
        }
    }
}
