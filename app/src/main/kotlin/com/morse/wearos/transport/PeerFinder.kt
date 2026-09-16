package com.morse.wearos.transport

import java.net.InetAddress

/** Resolves how a joiner reaches the host; the socket layer is the same for every finder. */
interface PeerFinder {
    /** Host side: make the group discoverable under [code]. */
    suspend fun publish(code: String)

    /** Joiner side: find the host advertising [code]. */
    suspend fun resolveHost(code: String): InetAddress

    fun stop()
}
