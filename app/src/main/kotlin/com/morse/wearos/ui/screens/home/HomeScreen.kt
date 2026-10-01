package com.morse.wearos.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.morse.wearos.R
import com.morse.wearos.data.TransportMode
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.transport.IS_DEMO
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.permissions.rememberWifiDirectPermissionRequest
import com.morse.wearos.ui.theme.MorseColors

@Composable
fun HomeScreen(
    onOpenGroup: () -> Unit,
    onJoinGroup: () -> Unit,
    onWords: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = containerViewModel { HomeViewModel(it.groupSession, it.settingsRepository) },
) {
    val transportMode by viewModel.transportMode.collectAsStateWithLifecycle()
    val activeGroupCode by viewModel.activeGroupCode.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val requestPermission = rememberWifiDirectPermissionRequest(
        onGranted = { pendingAction?.invoke(); pendingAction = null },
        onDenied = { pendingAction = null; permissionDenied = true },
    )

    fun guarded(action: () -> Unit) {
        if (transportMode == TransportMode.WIFI_DIRECT && !IS_DEMO) {
            pendingAction = action
            requestPermission()
        } else {
            action()
        }
    }

    MorseScaffold(title = stringResource(R.string.app_name)) {
        activeGroupCode?.let { code ->
            item {
                Platter(
                    title = stringResource(R.string.home_current_group),
                    subtitle = stringResource(R.string.home_current_group_code, code),
                    icon = R.drawable.ph_users_three,
                    accent = true,
                    onClick = onOpenGroup,
                )
            }
        }
        item {
            Platter(
                title = stringResource(R.string.home_create_group),
                icon = R.drawable.ph_plus,
                iconTint = MorseColors.Green,
                onClick = { guarded { viewModel.createGroup(); onOpenGroup() } },
            )
        }
        item {
            Platter(
                title = stringResource(R.string.home_join_group),
                icon = R.drawable.ph_sign_in,
                iconTint = MorseColors.Blue,
                onClick = { guarded(onJoinGroup) },
            )
        }
        item {
            Platter(
                title = stringResource(R.string.home_my_words),
                icon = R.drawable.ph_chat_text,
                iconTint = MorseColors.Purple,
                onClick = onWords,
            )
        }
        item {
            Platter(
                title = stringResource(R.string.home_settings),
                icon = R.drawable.ph_gear_six,
                iconTint = MorseColors.Grey,
                onClick = onSettings,
            )
        }
    }

    Dialog(showDialog = permissionDenied, onDismissRequest = { permissionDenied = false }) {
        Alert(
            title = { Text(stringResource(R.string.permission_wifi_title)) },
            content = { Text(stringResource(R.string.permission_wifi_rationale)) },
            positiveButton = {
                Button(onClick = { permissionDenied = false }) { Text(stringResource(R.string.action_ok)) }
            },
            negativeButton = {},
        )
    }
}
