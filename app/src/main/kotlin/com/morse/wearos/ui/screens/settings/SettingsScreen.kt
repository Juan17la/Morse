package com.morse.wearos.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.morse.wearos.R
import com.morse.wearos.data.TransportMode
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.transport.IS_DEMO
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.rememberTextInputLauncher

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = containerViewModel {
        SettingsViewModel(it.identityRepository, it.settingsRepository, it.hapticPlayer)
    },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val launchNameInput = rememberTextInputLauncher(
        label = stringResource(R.string.settings_name_input_label),
        onResult = viewModel::setName,
    )
    val launchLanHostInput = rememberTextInputLauncher(
        label = stringResource(R.string.settings_lan_host_input_label),
        onResult = viewModel::setLanHostAddress,
    )

    MorseScaffold {
        item { ListHeader { Text(stringResource(R.string.settings_title)) } }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.settings_name)) },
                secondaryLabel = { Text(state.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
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

        // Demo builds always simulate, so the connection choice would do nothing there.
        if (!IS_DEMO) {
            item { ListHeader { Text(stringResource(R.string.settings_transport_header)) } }
            items(TransportMode.entries.size) { index ->
                val mode = TransportMode.entries[index]
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    checked = state.transportMode == mode,
                    onCheckedChange = { if (it) viewModel.setTransportMode(mode) },
                    label = { Text(stringResource(mode.labelRes()), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    toggleControl = { RadioButton(selected = state.transportMode == mode) },
                    colors = ToggleChipDefaults.toggleChipColors(),
                )
            }
            if (state.transportMode == TransportMode.LAN) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.settings_lan_host)) },
                        secondaryLabel = { Text(state.lanHostAddress, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        colors = ChipDefaults.secondaryChipColors(),
                        onClick = launchLanHostInput,
                    )
                }
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

private fun TransportMode.labelRes(): Int = when (this) {
    TransportMode.LAN -> R.string.transport_lan
    TransportMode.WIFI_DIRECT -> R.string.transport_wifi_direct
}
