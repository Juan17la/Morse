package com.morse.wearos.ui.screens.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JoinViewModel(private val session: GroupSession) : ViewModel() {

    val state: StateFlow<ConnectionState> = session.state

    fun join(code: String) {
        viewModelScope.launch { session.joinGroup(code) }
    }

    fun cancel() {
        viewModelScope.launch { session.leave() }
    }
}
