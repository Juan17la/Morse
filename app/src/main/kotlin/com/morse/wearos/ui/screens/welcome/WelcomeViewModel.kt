package com.morse.wearos.ui.screens.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.core.haptics.HapticPlayer
import com.morse.wearos.data.IdentityRepository
import com.morse.wearos.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WelcomeViewModel(
    private val identity: IdentityRepository,
    settings: SettingsRepository,
    private val haptics: HapticPlayer,
) : ViewModel() {

    val displayName: StateFlow<String> = identity.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    init {
        // Seen once is enough: swiping the welcome away must not bring it back.
        viewModelScope.launch { settings.setWelcomeSeen() }
    }

    fun play(morse: String) = haptics.play(morse)

    fun setName(name: String) {
        viewModelScope.launch { identity.setNameOverride(name) }
    }
}
