package com.morse.wearos.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morse.wearos.ui.components.SectionHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import com.morse.wearos.R
import com.morse.wearos.core.haptics.HapticSignal
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.AppLanguage
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
    val context = LocalContext.current
    val language = remember { AppLanguage.get(context) }

    val launchNameInput = rememberTextInputLauncher(
        label = stringResource(R.string.settings_name_input_label),
        onResult = viewModel::setName,
    )
    val launchLanHostInput = rememberTextInputLauncher(
        label = stringResource(R.string.settings_lan_host_input_label),
        onResult = viewModel::setLanHostAddress,
    )

    MorseScaffold(title = stringResource(R.string.settings_title), glow = MorseColors.Indigo) {
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

        // Feeling one is the fastest way to understand what the app does.
        item {
            Platter(
                title = stringResource(R.string.settings_test_vibration),
                subtitle = MorseCodec.encode("SOS"),
                monospaceSubtitle = true,
                icon = R.drawable.ph_vibrate,
                iconTint = MorseColors.Accent,
                onClick = viewModel::testVibration,
            )
        }
        item { SectionHeader(stringResource(R.string.settings_signals_header)) }
        items(SIGNALS.size) { index ->
            val (signal, label, tint) = SIGNALS[index]
            Platter(
                title = stringResource(label),
                icon = R.drawable.ph_vibrate,
                iconTint = tint,
                onClick = { viewModel.feel(signal) },
            )
        }
        item { SectionHeader(stringResource(R.string.settings_language_header)) }
        items(AppLanguage.TAGS.size) { index ->
            val tag = AppLanguage.TAGS[index]
            RadioChip(stringResource(languageLabel(tag)), language == tag) {
                AppLanguage.set(context, tag)
                (context as? Activity)?.recreate()
            }
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

        // Demo builds always simulate, so the connection choice would do nothing there.
        if (!IS_DEMO) {
            item { SectionHeader(stringResource(R.string.settings_developer_header)) }
            items(TransportMode.entries.size) { index ->
                val mode = TransportMode.entries[index]
                RadioChip(stringResource(mode.labelRes()), state.transportMode == mode) { viewModel.setTransportMode(mode) }
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
    }
}

private val SIGNALS = listOf(
    Triple(HapticSignal.Joined, R.string.signal_joined, MorseColors.Green),
    Triple(HapticSignal.Sent, R.string.signal_sent, MorseColors.Blue),
    Triple(HapticSignal.Failed, R.string.signal_failed, MorseColors.Red),
    Triple(HapticSignal.MemberLeft, R.string.signal_member_left, MorseColors.Purple),
    Triple(HapticSignal.Lost, R.string.signal_lost, MorseColors.Grey),
)

@Composable
private fun RadioChip(label: String, selected: Boolean, onSelect: () -> Unit) {
    ToggleChip(
        modifier = Modifier.fillMaxWidth(),
        checked = selected,
        onCheckedChange = { if (it) onSelect() },
        label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        toggleControl = { RadioButton(selected = selected) },
        colors = ToggleChipDefaults.toggleChipColors(
            checkedStartBackgroundColor = MorseColors.Indigo.copy(alpha = 0.45f),
            checkedEndBackgroundColor = MorseColors.PlatterHigh,
            uncheckedStartBackgroundColor = MorseColors.Platter,
            uncheckedEndBackgroundColor = MorseColors.Platter,
        ),
        shape = RoundedCornerShape(24.dp),
    )
}

private fun languageLabel(tag: String): Int = when (tag) {
    "en" -> R.string.language_en
    "es" -> R.string.language_es
    else -> R.string.language_system
}

private fun TransportMode.labelRes(): Int = when (this) {
    TransportMode.LAN -> R.string.transport_lan
    TransportMode.WIFI_DIRECT -> R.string.transport_wifi_direct
}
