package com.morse.wearos.transport

import android.content.Context
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.data.SettingsRepository
import com.morse.wearos.data.TransportMode
import com.morse.wearos.transport.lan.LanPeerFinder
import com.morse.wearos.transport.socket.SocketGroupTransport
import com.morse.wearos.transport.wifidirect.WifiDirectPeerFinder
import kotlinx.coroutines.flow.first

class TransportFactory(
    private val context: Context,
    private val settings: SettingsRepository,
) {
    fun create(mode: TransportMode): GroupTransport = when (mode) {
        TransportMode.SIMULATED -> SimulatedTransport(DefaultWords.resolve(context))
        TransportMode.LAN -> SocketGroupTransport(LanPeerFinder { settings.lanHostAddress.first() })
        TransportMode.WIFI_DIRECT -> SocketGroupTransport(WifiDirectPeerFinder(context))
    }
}
