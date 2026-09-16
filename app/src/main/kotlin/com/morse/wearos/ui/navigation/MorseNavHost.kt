package com.morse.wearos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.morse.wearos.ui.components.PlaceholderScreen
import com.morse.wearos.ui.screens.home.HomeScreen

@Composable
fun MorseNavHost() {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onCreateGroup = { navController.navigate(Routes.GROUP) },
                onJoinGroup = { navController.navigate(Routes.JOIN) },
                onWords = { navController.navigate(Routes.WORDS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.JOIN) { PlaceholderScreen("Join") }
        composable(Routes.GROUP) { PlaceholderScreen("Group") }
        composable(Routes.SEND) { PlaceholderScreen("Send") }
        composable(Routes.INCOMING) { PlaceholderScreen("Incoming") }
        composable(Routes.WORDS) { PlaceholderScreen("Words") }
        composable(Routes.SETTINGS) { PlaceholderScreen("Settings") }
    }
}
