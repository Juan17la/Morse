package com.morse.wearos.transport.socket

import android.util.Log
import com.morse.wearos.domain.GroupConfig
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.domain.cleanMemberName
import com.morse.wearos.transport.Envelope
import com.morse.wearos.transport.Protocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.ServerSocket

/** Star-topology host: validates joins, keeps the member list and relays every message. */
class GroupHostServer(
    private val code: String,
    private val self: Member,
    private val scope: CoroutineScope,
    private val members: MutableStateFlow<List<Member>>,
    private val incoming: MutableSharedFlow<MorseMessage>,
) {
    private class Client(val member: Member, val socket: EnvelopeSocket)

    private var serverSocket: ServerSocket? = null
    private val clients = mutableListOf<Client>()
    private val clientsLock = Mutex()

    suspend fun start() = withContext(Dispatchers.IO) {
        val server = ServerSocket(Protocol.PORT)
        serverSocket = server
        members.value = listOf(self)
        scope.launch(Dispatchers.IO) { acceptLoop(server) }
    }

    suspend fun send(message: MorseMessage) {
        incoming.emit(message)
        broadcast(Envelope.Message(message), exceptId = null)
    }

    suspend fun stop() {
        val snapshot = clientsLock.withLock { clients.toList().also { clients.clear() } }
        snapshot.forEach { client ->
            runCatching { client.socket.write(Envelope.Bye(self.id)) }
            client.socket.close()
        }
        runCatching { serverSocket?.close() }
        serverSocket = null
        members.value = emptyList()
    }

    private suspend fun acceptLoop(server: ServerSocket) {
        while (!server.isClosed) {
            val raw = runCatching { server.accept() }.getOrNull() ?: break
            scope.launch(Dispatchers.IO) { handshake(EnvelopeSocket(raw)) }
        }
    }

    private suspend fun handshake(socket: EnvelopeSocket) {
        val hello = runCatching { socket.read() }.getOrNull() as? Envelope.Hello
        if (hello == null) {
            socket.close(); return
        }
        if (hello.code != code) {
            socket.write(Envelope.Rejected(Envelope.Rejected.Reason.WRONG_CODE))
            socket.close(); return
        }
        val member = hello.member.copy(name = cleanMemberName(hello.member.name), isHost = false)
        val accepted = clientsLock.withLock {
            if (clients.size + 1 >= GroupConfig.MAX_MEMBERS) {
                false
            } else {
                clients.removeAll { it.member.id == member.id }
                clients += Client(member, socket)
                true
            }
        }
        if (!accepted) {
            socket.write(Envelope.Rejected(Envelope.Rejected.Reason.GROUP_FULL))
            socket.close(); return
        }
        val current = refreshMembers()
        socket.write(Envelope.Welcome(code, current))
        broadcast(Envelope.MembersUpdate(current), exceptId = member.id)
        Log.d(TAG, "member joined ${member.name} from ${socket.remoteAddress}")
        readLoop(member, socket)
    }

    private suspend fun readLoop(member: Member, socket: EnvelopeSocket) {
        try {
            while (true) {
                when (val envelope = socket.read() ?: break) {
                    is Envelope.Message -> {
                        incoming.emit(envelope.message)
                        broadcast(envelope, exceptId = member.id)
                    }
                    is Envelope.Bye -> break
                    else -> Unit
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "client ${member.name} dropped: ${e.message}")
        } finally {
            socket.close()
            clientsLock.withLock { clients.removeAll { it.member.id == member.id } }
            broadcast(Envelope.MembersUpdate(refreshMembers()), exceptId = null)
        }
    }

    private suspend fun refreshMembers(): List<Member> {
        val list = clientsLock.withLock { listOf(self) + clients.map { it.member } }
        members.update { list }
        return list
    }

    private suspend fun broadcast(envelope: Envelope, exceptId: String?) {
        val targets = clientsLock.withLock { clients.filter { it.member.id != exceptId } }
        targets.forEach { client ->
            runCatching { client.socket.write(envelope) }
                .onFailure { Log.d(TAG, "write to ${client.member.name} failed: ${it.message}") }
        }
    }

    private companion object {
        const val TAG = "GroupHostServer"
    }
}
