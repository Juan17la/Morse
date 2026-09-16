package com.morse.wearos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.morse.wearos.di.appContainer
import com.morse.wearos.group.SessionEvent
import com.morse.wearos.ui.screens.group.GroupScreen
import com.morse.wearos.ui.screens.home.HomeScreen
import com.morse.wearos.ui.screens.incoming.IncomingScreen
import com.morse.wearos.ui.screens.join.JoinScreen
import com.morse.wearos.ui.screens.send.SendScreen
import com.morse.wearos.ui.screens.settings.SettingsScreen
import com.morse.wearos.ui.screens.words.WordsScreen

@Composable
fun MorseNavHost() {
    val navController = rememberSwipeDismissableNavController()
    val session = appContainer().groupSession

    LaunchedEffect(session) {
        session.events.collect { event ->
            when (event) {
                is SessionEvent.MessageReceived -> {
                    if (navController.currentDestination?.route != Routes.INCOMING) {
                        navController.navigate(Routes.INCOMING) { launchSingleTop = true }
                    }
                }
            }
        }
    }

    SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onGroupCreated = { navController.navigate(Routes.GROUP) },
                onOpenGroup = { navController.navigate(Routes.GROUP) },
                onJoinGroup = { navController.navigate(Routes.JOIN) },
                onWords = { navController.navigate(Routes.WORDS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.JOIN) {
            JoinScreen(
                onJoined = {
                    navController.navigate(Routes.GROUP) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(Routes.GROUP) {
            GroupScreen(
                onSend = { navController.navigate(Routes.SEND) },
                onOpenLastMessage = { navController.navigate(Routes.INCOMING) },
                onLeft = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.SEND) {
            SendScreen(onSent = { navController.popBackStack() })
        }
        composable(Routes.INCOMING) { IncomingScreen() }
        composable(Routes.WORDS) { WordsScreen() }
        composable(Routes.SETTINGS) { SettingsScreen() }
    }
}
