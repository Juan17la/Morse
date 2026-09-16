package com.morse.wearos.ui.screens.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupInfo
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupUiState(
    val state: ConnectionState = ConnectionState.Idle,
    val group: GroupInfo? = null,
    val lastMessage: MorseMessage? = null,
    val selfId: String? = null,
)

class GroupViewModel(private val session: GroupSession) : ViewModel() {

    val uiState: StateFlow<GroupUiState> = combine(
        session.state,
        session.group,
        session.lastMessage,
    ) { state, group, last ->
        GroupUiState(state, group, last, session.selfId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupUiState())

    fun retryCreate() = session.createGroupAsync()

    fun leave() {
        viewModelScope.launch { session.leave() }
    }
}
