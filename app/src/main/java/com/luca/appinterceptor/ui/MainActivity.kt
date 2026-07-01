package com.luca.appinterceptor.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.luca.appinterceptor.data.Graph
import com.luca.appinterceptor.ui.apps.AppSelectionScreen
import com.luca.appinterceptor.ui.home.HomeScreen
import com.luca.appinterceptor.ui.onboarding.OnboardingScreen
import com.luca.appinterceptor.ui.stats.StatsScreen
import com.luca.appinterceptor.ui.theme.InterceptorTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            InterceptorTheme {
                AppNav()
            }
        }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    // Einmaliger kleiner DataStore-Read beim Start, um das Ziel zu bestimmen
    val startDestination = remember {
        runBlocking { if (Graph.settings.onboardingDone.first()) "home" else "onboarding" }
    }
    NavHost(navController = nav, startDestination = startDestination) {
        composable("home") {
            HomeScreen(
                onOpenApps = { nav.navigate("apps") },
                onOpenStats = { nav.navigate("stats") },
                onOpenOnboarding = { nav.navigate("onboarding") },
            )
        }
        composable("onboarding") {
            OnboardingScreen(
                onFinished = {
                    nav.navigate("home") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("apps") { AppSelectionScreen(onBack = { nav.popBackStack() }) }
        composable("stats") { StatsScreen(onBack = { nav.popBackStack() }) }
    }
}
