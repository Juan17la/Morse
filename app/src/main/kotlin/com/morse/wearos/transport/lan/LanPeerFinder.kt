package com.morse.wearos.transport.lan

import com.morse.wearos.domain.TransportError
import com.morse.wearos.transport.PeerFinder
import com.morse.wearos.transport.TransportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

/** Joiner connects to a fixed address (e.g. 10.0.2.2 + `adb forward` for emulators). */
class LanPeerFinder(private val hostAddress: suspend () -> String) : PeerFinder {

    override suspend fun publish(code: String) = Unit

    override suspend fun resolveHost(code: String): InetAddress = withContext(Dispatchers.IO) {
        val address = hostAddress()
        runCatching { InetAddress.getByName(address) }
            .getOrElse { throw TransportException(TransportError.HOST_NOT_FOUND, address) }
    }

    override fun stop() = Unit
}
