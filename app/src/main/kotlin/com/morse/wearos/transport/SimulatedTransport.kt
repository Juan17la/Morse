package com.morse.wearos.transport

import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupConfig
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

/** In-memory transport with fake members so the whole flow can be demoed on one emulator. */
class SimulatedTransport(private val replyWords: List<String>) : GroupTransport {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _members = MutableStateFlow<List<Member>>(emptyList())
    override val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _incoming = MutableSharedFlow<MorseMessage>(extraBufferCapacity = 16)
    override val incoming: SharedFlow<MorseMessage> = _incoming.asSharedFlow()

    private val fakes = listOf(
        Member(id = "sim-ana", name = "Ana's Watch"),
        Member(id = "sim-luis", name = "Luis' Watch"),
        Member(id = "sim-maria", name = "María's Watch"),
    )

    override suspend fun createGroup(self: Member): String {
        reset()
        _state.value = ConnectionState.Creating
        val code = AccessCode.generate()
        delay(800)
        _members.value = listOf(self.copy(isHost = true))
        _state.value = ConnectionState.Connected
        scope.launch {
            for (fake in fakes) {
                if (_members.value.size >= GroupConfig.MAX_MEMBERS) break
                delay(1_500)
                _members.update { it + fake }
            }
        }
        return code
    }

    override suspend fun joinGroup(code: String, self: Member) {
        reset()
        _state.value = ConnectionState.Joining
        delay(1_200)
        _members.value = listOf(fakes[0].copy(isHost = true), self.copy(isHost = false))
        _state.value = ConnectionState.Connected
        scope.launch {
            for (fake in fakes.drop(1)) {
                if (_members.value.size >= GroupConfig.MAX_MEMBERS) break
                delay(2_000)
                _members.update { it + fake }
            }
        }
    }

    override suspend fun send(message: MorseMessage) {
        _incoming.emit(message)
        val responders = _members.value.filter { it.id.startsWith("sim-") }
        if (responders.isEmpty() || replyWords.isEmpty()) return
        scope.launch {
            delay(Random.nextLong(2_000, 4_000))
            val from = responders.random()
            val word = replyWords.random()
            _incoming.emit(
                MorseMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = from.id,
                    senderName = from.name,
                    word = word,
                    morse = MorseCodec.encode(word),
                    timestampMs = System.currentTimeMillis(),
                )
            )
        }
    }

    override suspend fun leave() {
        reset()
    }

    private fun reset() {
        scope.coroutineContext.cancelChildren()
        _members.value = emptyList()
        _state.value = ConnectionState.Idle
    }
}
