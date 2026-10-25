package com.morse.wearos.group

import android.util.Log
import com.morse.wearos.core.haptics.HapticPlayer
import com.morse.wearos.core.haptics.HapticSignal
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.IdentityRepository
import com.morse.wearos.data.SettingsRepository
import com.morse.wearos.data.TransportMode
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupInfo
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.domain.TransportError
import com.morse.wearos.transport.GroupTransport
import com.morse.wearos.transport.TransportException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "GroupSession"
private const val HISTORY_SIZE = 10

/** App-scoped owner of the active group: swaps transports, relays state and plays haptics. */
class GroupSession(
    private val createTransport: (TransportMode) -> GroupTransport,
    private val identity: IdentityRepository,
    private val settings: SettingsRepository,
    private val haptics: HapticPlayer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var transport: GroupTransport? = null
    private var collectors: List<Job> = emptyList()

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _group = MutableStateFlow<GroupInfo?>(null)
    val group: StateFlow<GroupInfo?> = _group.asStateFlow()

    private val _lastMessage = MutableStateFlow<MorseMessage?>(null)
    val lastMessage: StateFlow<MorseMessage?> = _lastMessage.asStateFlow()

    /** Recent group messages (ours included), oldest first; context for smart replies. */
    private val _history = MutableStateFlow<List<MorseMessage>>(emptyList())
    val history: StateFlow<List<MorseMessage>> = _history.asStateFlow()

    /** Messages from other members; the local echo of our own sends is filtered out. */
    private val _received = MutableSharedFlow<MorseMessage>(extraBufferCapacity = 8)
    val received: SharedFlow<MorseMessage> = _received.asSharedFlow()

    var selfId: String? = null
        private set

    var isHost: Boolean = false
        private set

    fun createGroupAsync() {
        scope.launch { createGroup() }
    }

    private suspend fun createGroup() {
        // Set before the first suspension so the group screen, opened right after, already knows our role.
        isHost = true
        val t = attachTransport()
        val self = identity.self()
        selfId = self.id
        runCatching { t.createGroup(self) }
            .onSuccess { code -> _group.update { GroupInfo(code, t.members.value) } }
            .onFailure { fail(it) }
    }

    suspend fun joinGroup(code: String) {
        isHost = false
        val t = attachTransport()
        val self = identity.self()
        selfId = self.id
        _group.value = GroupInfo(code, emptyList())
        runCatching { t.joinGroup(code, self) }.onFailure { fail(it) }
    }

    /** Sends [word] to the group; false when there is no group or the transport failed. */
    suspend fun send(word: String): Boolean {
        val t = transport ?: return false
        val self = identity.self()
        val message = MorseMessage(
            id = UUID.randomUUID().toString(),
            senderId = self.id,
            senderName = self.name,
            word = word,
            morse = MorseCodec.encode(word),
            timestampMs = System.currentTimeMillis(),
        )
        return runCatching { t.send(message) }
            .onSuccess { haptics.signal(HapticSignal.Sent) }
            .onFailure { haptics.signal(HapticSignal.Failed); fail(it) }
            .isSuccess
    }

    suspend fun leave() {
        collectors.forEach { it.cancel() }
        collectors = emptyList()
        runCatching { transport?.leave() }
        transport = null
        _group.value = null
        _lastMessage.value = null
        _history.value = emptyList()
        _state.value = ConnectionState.Idle
    }

    fun replayLast() {
        _lastMessage.value?.let { haptics.play(it.morse) }
    }

    private suspend fun attachTransport(): GroupTransport {
        leave()
        val mode = settings.transportMode.first()
        Log.d(TAG, "attaching transport for mode $mode")
        val t = createTransport(mode)
        transport = t
        collectors = listOf(
            scope.launch { t.state.collect { signalStateChange(_state.value, it); _state.value = it } },
            scope.launch {
                t.members.collect { m ->
                    if (m.size < (_group.value?.members?.size ?: 0)) haptics.signal(HapticSignal.MemberLeft)
                    _group.update { it?.copy(members = m) }
                }
            },
            scope.launch { t.incoming.collect { onMessage(it) } },
        )
        return t
    }

    private fun signalStateChange(from: ConnectionState, to: ConnectionState) {
        when {
            to == ConnectionState.Connected && from != ConnectionState.Connected -> haptics.signal(HapticSignal.Joined)
            to is ConnectionState.Error && from == ConnectionState.Connected -> haptics.signal(HapticSignal.Lost)
        }
    }

    private fun onMessage(message: MorseMessage) {
        // History first: lastMessage observers read it to build smart-reply context.
        _history.update { (it + message).takeLast(HISTORY_SIZE) }
        _lastMessage.value = message
        if (message.senderId != selfId) {
            haptics.play(message.morse)
            _received.tryEmit(message)
        }
    }

    private fun fail(error: Throwable) {
        Log.w(TAG, "group operation failed", error)
        _state.value = ConnectionState.Error((error as? TransportException)?.error ?: TransportError.UNKNOWN)
    }
}
