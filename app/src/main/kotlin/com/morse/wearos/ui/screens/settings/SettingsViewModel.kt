package com.morse.wearos.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morse.wearos.core.haptics.HapticPlayer
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.IdentityRepository
import com.morse.wearos.data.SettingsRepository
import com.morse.wearos.data.TransportMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val displayName: String = "",
    val hasNameOverride: Boolean = false,
    val transportMode: TransportMode = SettingsRepository.DEFAULT_TRANSPORT,
    val lanHostAddress: String = SettingsRepository.DEFAULT_LAN_HOST,
)

class SettingsViewModel(
    private val identity: IdentityRepository,
    private val settings: SettingsRepository,
    private val haptics: HapticPlayer,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        identity.displayName,
        identity.hasNameOverride,
        settings.transportMode,
        settings.lanHostAddress,
    ) { name, hasOverride, mode, lanHost ->
        SettingsUiState(name, hasOverride, mode, lanHost)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setName(name: String) {
        viewModelScope.launch { identity.setNameOverride(name) }
    }

    fun resetName() {
        viewModelScope.launch { identity.setNameOverride(null) }
    }

    fun setTransportMode(mode: TransportMode) {
        viewModelScope.launch { settings.setTransportMode(mode) }
    }

    fun setLanHostAddress(address: String) {
        viewModelScope.launch { settings.setLanHostAddress(address) }
    }

    fun testVibration() {
        haptics.play(MorseCodec.encode("SOS"))
    }
}
