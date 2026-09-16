package com.morse.wearos.transport

import android.content.Context
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.data.TransportMode

class TransportFactory(private val context: Context) {

    fun create(mode: TransportMode): GroupTransport = when (mode) {
        TransportMode.SIMULATED -> SimulatedTransport(DefaultWords.resolve(context))
        TransportMode.LAN, TransportMode.WIFI_DIRECT -> SimulatedTransport(DefaultWords.resolve(context))
    }
}
