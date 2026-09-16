package com.morse.wearos.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.data.SettingsRepository
import com.morse.wearos.data.TransportMode
import com.morse.wearos.group.GroupSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    private val session: GroupSession,
    settings: SettingsRepository,
) : ViewModel() {

    val transportMode: StateFlow<TransportMode> = settings.transportMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransportMode.SIMULATED)

    fun createGroup() = session.createGroupAsync()
}
