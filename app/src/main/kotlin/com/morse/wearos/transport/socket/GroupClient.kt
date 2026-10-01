package com.morse.wearos.transport.socket

import android.util.Log
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.domain.TransportError
import com.morse.wearos.transport.Envelope
import com.morse.wearos.transport.Protocol
import com.morse.wearos.transport.TransportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class GroupClient(
    private val self: Member,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<ConnectionState>,
    private val members: MutableStateFlow<List<Member>>,
    private val incoming: MutableSharedFlow<MorseMessage>,
) {
    private var socket: EnvelopeSocket? = null

    suspend fun connect(host: InetAddress, code: String) = withContext(Dispatchers.IO) {
        val raw = Socket()
        try {
            raw.connect(InetSocketAddress(host, Protocol.PORT), CONNECT_TIMEOUT_MS)
        } catch (e: Exception) {
            throw TransportException(TransportError.HOST_UNREACHABLE, host.hostAddress)
        }
        val envelopeSocket = EnvelopeSocket(raw)
        socket = envelopeSocket
        envelopeSocket.write(Envelope.Hello(code, self))
        when (val reply = envelopeSocket.read()) {
            is Envelope.Welcome -> members.value = reply.members
            is Envelope.Rejected -> {
                envelopeSocket.close()
                throw TransportException(
                    when (reply.reason) {
                        Envelope.Rejected.Reason.WRONG_CODE -> TransportError.WRONG_CODE
                        Envelope.Rejected.Reason.GROUP_FULL -> TransportError.GROUP_FULL
                    }
                )
            }
            else -> {
                envelopeSocket.close()
                throw TransportException(TransportError.CONNECTION_LOST)
            }
        }
        scope.launch(Dispatchers.IO) { readLoop(envelopeSocket) }
    }

    suspend fun send(message: MorseMessage) {
        incoming.emit(message)
        val s = socket ?: throw TransportException(TransportError.CONNECTION_LOST)
        withContext(Dispatchers.IO) { s.write(Envelope.Message(message)) }
    }

    suspend fun close() {
        val s = socket ?: return
        socket = null
        withContext(Dispatchers.IO) {
            runCatching { s.write(Envelope.Bye(self.id)) }
            s.close()
        }
    }

    private suspend fun readLoop(s: EnvelopeSocket) {
        try {
            while (true) {
                when (val envelope = s.read() ?: break) {
                    is Envelope.MembersUpdate -> members.value = envelope.members
                    is Envelope.Message -> incoming.emit(envelope.message)
                    is Envelope.Bye -> break
                    else -> Unit
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "read loop ended: ${e.message}")
        }
        // Only report if we did not close the socket ourselves via close().
        if (socket === s) {
            socket = null
            s.close()
            members.value = emptyList()
            state.value = ConnectionState.Error(TransportError.CONNECTION_LOST)
        }
    }

    private companion object {
        const val TAG = "GroupClient"
        const val CONNECT_TIMEOUT_MS = 5_000
    }
}
