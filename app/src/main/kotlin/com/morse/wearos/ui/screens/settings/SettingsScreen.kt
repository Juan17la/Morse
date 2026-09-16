package com.morse.wearos.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.morse.wearos.R
import com.morse.wearos.data.TransportMode
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.rememberTextInputLauncher

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = containerViewModel {
        SettingsViewModel(it.identityRepository, it.settingsRepository, it.hapticPlayer)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()

    val launchNameInput = rememberTextInputLauncher(
        label = stringResource(R.string.settings_name_input_label),
        onResult = viewModel::setName,
    )
    val launchLanHostInput = rememberTextInputLauncher(
        label = stringResource(R.string.settings_lan_host_input_label),
        onResult = viewModel::setLanHostAddress,
    )

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(state = listState) {
            item { ListHeader { Text(stringResource(R.string.settings_title)) } }

            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_name)) },
                    secondaryLabel = { Text(state.displayName) },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = launchNameInput,
                )
            }
            if (state.hasNameOverride) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_name_reset)) },
                        colors = ChipDefaults.secondaryChipColors(),
                        onClick = viewModel::resetName,
                    )
                }
            }

            item { ListHeader { Text(stringResource(R.string.settings_transport_header)) } }
            items(TransportMode.entries.size) { index ->
                val mode = TransportMode.entries[index]
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = state.transportMode == mode,
                    onCheckedChange = { if (it) viewModel.setTransportMode(mode) },
                    label = { Text(stringResource(mode.labelRes())) },
                    toggleControl = {
                        RadioButton(selected = state.transportMode == mode)
                    },
                    colors = ToggleChipDefaults.toggleChipColors(),
                )
            }
            if (state.transportMode == TransportMode.LAN) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_lan_host)) },
                        secondaryLabel = { Text(state.lanHostAddress) },
                        colors = ChipDefaults.secondaryChipColors(),
                        onClick = launchLanHostInput,
                    )
                }
            }

            item { ListHeader { Text(stringResource(R.string.settings_diagnostics_header)) } }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.settings_test_vibration)) },
                    colors = ChipDefaults.primaryChipColors(),
                    onClick = viewModel::testVibration,
                )
            }
        }
    }
}

private fun TransportMode.labelRes(): Int = when (this) {
    TransportMode.SIMULATED -> R.string.transport_simulated
    TransportMode.LAN -> R.string.transport_lan
    TransportMode.WIFI_DIRECT -> R.string.transport_wifi_direct
}
