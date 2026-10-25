package com.morse.wearos.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.data.SettingsRepository
import com.morse.wearos.data.TransportMode
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupInfo
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
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsRepository.DEFAULT_TRANSPORT)

    /** Null until read from disk, so the first launch can go to the welcome without flashing Home. */
    val welcomeSeen: StateFlow<Boolean?> = settings.welcomeSeen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The group this watch is currently connected to, if any. */
    val activeGroup: StateFlow<GroupInfo?> = combine(session.state, session.group) { state, group ->
        group?.takeIf { state == ConnectionState.Connected }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createGroup() = session.createGroupAsync()
}
