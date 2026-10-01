package com.morse.wearos.ui.screens.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.morse.wearos.R
import com.morse.wearos.data.TransportMode
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.transport.IS_DEMO
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.components.rememberTextInputLauncher
import com.morse.wearos.ui.theme.MorseColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = containerViewModel {
        SettingsViewModel(it.identityRepository, it.settingsRepository, it.hapticPlayer, it.assistant)
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

    MorseScaffold(title = stringResource(R.string.settings_title)) {

        item {
            Platter(
                title = stringResource(R.string.settings_name),
                subtitle = state.displayName,
                icon = R.drawable.ph_user,
                iconTint = MorseColors.Blue,
                onClick = launchNameInput,
            )
        }
        if (state.hasNameOverride) {
            item {
                Platter(
                    title = stringResource(R.string.settings_name_reset),
                    icon = R.drawable.ph_arrow_counter_clockwise,
                    iconTint = MorseColors.Grey,
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
                    colors = ToggleChipDefaults.toggleChipColors(
                        checkedStartBackgroundColor = MorseColors.Platter,
                        checkedEndBackgroundColor = MorseColors.Platter,
                        uncheckedStartBackgroundColor = MorseColors.Platter,
                        uncheckedEndBackgroundColor = MorseColors.Platter,
                    ),
                    shape = RoundedCornerShape(22.dp),
                )
            }
            if (state.transportMode == TransportMode.LAN) {
                item {
                    Platter(
                        title = stringResource(R.string.settings_lan_host),
                        subtitle = state.lanHostAddress,
                        icon = R.drawable.ph_gear_six,
                        iconTint = MorseColors.Grey,
                        onClick = launchLanHostInput,
                    )
                }
            }
        }

        item { ListHeader { Text(stringResource(R.string.settings_diagnostics_header)) } }
        item {
            Platter(
                title = stringResource(R.string.settings_test_vibration),
                icon = R.drawable.ph_vibrate,
                iconTint = MorseColors.Accent,
                onClick = viewModel::testVibration,
            )
        }
        item {
            Text(
                text = stringResource(if (state.aiOnline) R.string.settings_ai_online else R.string.settings_ai_offline),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun TransportMode.labelRes(): Int = when (this) {
    TransportMode.LAN -> R.string.transport_lan
    TransportMode.WIFI_DIRECT -> R.string.transport_wifi_direct
}
