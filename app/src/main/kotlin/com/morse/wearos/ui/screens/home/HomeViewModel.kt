package com.morse.wearos.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.data.SettingsRepository
import com.morse.wearos.data.TransportMode
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val session: GroupSession,
    settings: SettingsRepository,
) : ViewModel() {

    val transportMode: StateFlow<TransportMode> = settings.transportMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransportMode.SIMULATED)

    /** Access code of the group this watch is currently connected to, if any. */
    val activeGroupCode: StateFlow<String?> = combine(session.state, session.group) { state, group ->
        group?.code?.takeIf { state == ConnectionState.Connected }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createGroup() = session.createGroupAsync()
}
