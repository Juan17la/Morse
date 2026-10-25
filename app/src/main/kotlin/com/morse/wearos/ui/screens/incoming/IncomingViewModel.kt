package com.morse.wearos.ui.screens.incoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.ai.MorseAssistant
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest

/** Smart replies for the shown message; [Loading] while the model is asked, [None] for our own messages. */
sealed interface Replies {
    data object None : Replies
    data object Loading : Replies
    data class Ready(val options: List<String>, val fromAi: Boolean) : Replies
}

class IncomingViewModel(
    private val session: GroupSession,
    private val assistant: MorseAssistant,
) : ViewModel() {

    val message: StateFlow<MorseMessage?> = session.lastMessage

    val connection: StateFlow<ConnectionState> = session.state

    @OptIn(ExperimentalCoroutinesApi::class)
    val replies: StateFlow<Replies> = session.lastMessage
        .distinctUntilChangedBy { it?.id }
        .transformLatest { m ->
            if (m == null || m.senderId == session.selfId) {
                emit(Replies.None)
            } else {
                emit(Replies.Loading)
                val result = assistant.replies(m, session.history.value)
                emit(Replies.Ready(result.options, result.fromAi))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Replies.None)

    val selfId: String? get() = session.selfId

    fun replay() = session.replayLast()

    suspend fun reply(word: String): Boolean = session.send(word)
}
