package com.morse.wearos.transport.socket

import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.domain.TransportError
import com.morse.wearos.transport.AccessCode
import com.morse.wearos.transport.GroupTransport
import com.morse.wearos.transport.PeerFinder
import com.morse.wearos.transport.TransportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** TCP host/client group; how the joiner finds the host is delegated to [PeerFinder]. */
class SocketGroupTransport(private val peerFinder: PeerFinder) : GroupTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    override val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _incoming = MutableSharedFlow<MorseMessage>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<MorseMessage> = _incoming.asSharedFlow()

    private var server: GroupHostServer? = null
    private var client: GroupClient? = null

    override suspend fun createGroup(self: Member): String {
        leave()
        _state.value = ConnectionState.Creating
        val code = AccessCode.generate()
        try {
            val host = GroupHostServer(code, self.copy(isHost = true), scope, _members, _incoming)
            host.start()
            server = host
            peerFinder.publish(code)
            _state.value = ConnectionState.Connected
            return code
        } catch (e: Exception) {
            leave()
            throw e.asTransportException()
        }
    }

    override suspend fun joinGroup(code: String, self: Member) {
        leave()
        _state.value = ConnectionState.Joining
        try {
            val address = peerFinder.resolveHost(code)
            val c = GroupClient(self.copy(isHost = false), scope, _state, _members, _incoming)
            c.connect(address, code)
            client = c
            _state.value = ConnectionState.Connected
        } catch (e: Exception) {
            leave()
            throw e.asTransportException()
        }
    }

    override suspend fun send(message: MorseMessage) {
        server?.send(message) ?: client?.send(message)
            ?: throw TransportException(TransportError.CONNECTION_LOST)
    }

    override suspend fun leave() {
        runCatching { server?.stop() }
        runCatching { client?.close() }
        server = null
        client = null
        peerFinder.stop()
        scope.coroutineContext.cancelChildren()
        _members.value = emptyList()
        _state.value = ConnectionState.Idle
    }

    private fun Exception.asTransportException(): TransportException =
        this as? TransportException ?: TransportException(TransportError.UNKNOWN, message)
}
