package com.morse.wearos.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.morse.wearos.MorseApp
import com.morse.wearos.core.haptics.HapticPlayer
import com.morse.wearos.data.IdentityRepository
import com.morse.wearos.data.SettingsRepository
import com.morse.wearos.data.TransportMode
import com.morse.wearos.data.WordsRepository
import com.morse.wearos.group.GroupSession
import com.morse.wearos.transport.GroupTransport
import com.morse.wearos.transport.demoTransport
import com.morse.wearos.transport.lan.LanPeerFinder
import com.morse.wearos.transport.socket.SocketGroupTransport
import com.morse.wearos.transport.wifidirect.WifiDirectPeerFinder
import kotlinx.coroutines.flow.first

class AppContainer(private val appContext: Context) {
    val hapticPlayer: HapticPlayer by lazy { HapticPlayer(appContext) }
    val wordsRepository: WordsRepository by lazy { WordsRepository(appContext) }
    val identityRepository: IdentityRepository by lazy { IdentityRepository(appContext) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }
    val groupSession: GroupSession by lazy {
        GroupSession(::transport, identityRepository, settingsRepository, hapticPlayer)
    }

    private fun transport(mode: TransportMode): GroupTransport = demoTransport(appContext) ?: when (mode) {
        TransportMode.LAN -> SocketGroupTransport(LanPeerFinder { settingsRepository.lanHostAddress.first() })
        TransportMode.WIFI_DIRECT -> SocketGroupTransport(WifiDirectPeerFinder(appContext))
    }
}

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as MorseApp).container

@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
