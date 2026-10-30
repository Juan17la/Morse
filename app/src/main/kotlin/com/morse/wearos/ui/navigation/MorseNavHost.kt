package com.morse.wearos.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.wear.compose.navigation.currentBackStackEntryAsState
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.ui.components.PulseBorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.morse.wearos.di.appContainer
import com.morse.wearos.ui.screens.condense.CondenseScreen
import com.morse.wearos.ui.screens.group.GroupScreen
import com.morse.wearos.ui.screens.home.HomeScreen
import com.morse.wearos.ui.screens.incoming.IncomingScreen
import com.morse.wearos.ui.screens.join.JoinScreen
import com.morse.wearos.ui.screens.send.SendScreen
import com.morse.wearos.ui.screens.settings.SettingsScreen
import com.morse.wearos.ui.screens.learn.LearnScreen
import com.morse.wearos.ui.screens.welcome.WelcomeScreen
import com.morse.wearos.ui.screens.words.WordsScreen

@Composable
fun MorseNavHost() {
    val navController = rememberSwipeDismissableNavController()
    val session = appContainer().groupSession

    val route by navController.currentBackStackEntryAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // The full message page only takes over on the first page, on itself, or when the app is out of sight.
    val takeover by rememberUpdatedState(route?.destination?.route.let { it == Routes.HOME || it == Routes.INCOMING })
    var buzzing by remember { mutableStateOf(false) }
    var edgePulse by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(session) {
        session.received.collect { message ->
            if (takeover || !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                navController.navigate(Routes.INCOMING) { launchSingleTop = true }
            } else {
                // Don't pull the user out of what they're doing: mirror the buzz on the screen edge instead.
                // A newer message replaces the older pulse so two loops never fight over the border.
                edgePulse?.cancel()
                edgePulse = launch {
                    for (step in MorseTiming.steps(message.morse)) {
                        buzzing = step.on
                        delay(step.durationMs)
                    }
                    buzzing = false
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
    SwipeDismissableNavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenGroup = { navController.navigate(Routes.GROUP) },
                onJoinGroup = { navController.navigate(Routes.JOIN) },
                onSend = { navController.navigate(Routes.SEND) },
                onWords = { navController.navigate(Routes.WORDS) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onWelcome = { navController.navigate(Routes.WELCOME) { launchSingleTop = true } },
            )
        }
        composable(Routes.WELCOME) { WelcomeScreen(onStart = { navController.popBackStack() }) }
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
            SendScreen(
                onSent = { navController.popBackStack() },
                onDictated = { navController.navigate(Routes.condense(it)) },
                onFindGroup = { navController.popBackStack(Routes.HOME, inclusive = false) },
            )
        }
        composable(Routes.CONDENSE) { entry ->
            CondenseScreen(
                text = entry.arguments?.getString(Routes.CONDENSE_ARG).orEmpty(),
                // Back to wherever Send was opened from (Home or Group); Group may not be on the stack.
                onSent = { navController.popBackStack(Routes.SEND, inclusive = true) },
            )
        }
        composable(Routes.INCOMING) { IncomingScreen(onReplied = { navController.popBackStack() }) }
        composable(Routes.WORDS) { WordsScreen() }
        composable(Routes.SETTINGS) { SettingsScreen(onLearn = { navController.navigate(Routes.LEARN) }) }
        composable(Routes.LEARN) { LearnScreen() }
    }
    PulseBorder(buzzing)
    }
}
