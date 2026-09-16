package com.morse.wearos.transport

import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A group link between watches. Implementations deliver a sent message to the local
 * [incoming] flow as well as to every other member, so the UI sees one consistent stream.
 */
interface GroupTransport {
    val state: StateFlow<ConnectionState>
    val members: StateFlow<List<Member>>
    val incoming: SharedFlow<MorseMessage>

    /** Creates a group hosted by [self] and returns its access code. */
    suspend fun createGroup(self: Member): String

    suspend fun joinGroup(code: String, self: Member)

    suspend fun send(message: MorseMessage)

    suspend fun leave()
}

class TransportException(message: String) : Exception(message)
