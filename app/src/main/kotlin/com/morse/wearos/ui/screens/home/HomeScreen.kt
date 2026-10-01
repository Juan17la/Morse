package com.morse.wearos.ui.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.morse.wearos.R
import com.morse.wearos.data.TransportMode
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.transport.IS_DEMO
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.permissions.rememberWifiDirectPermissionRequest

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

    MorseScaffold {
        item { ListHeader { Text(stringResource(R.string.app_name)) } }
        activeGroupCode?.let { code ->
            item {
                HomeChip(
                    label = stringResource(R.string.home_current_group),
                    secondary = stringResource(R.string.home_current_group_code, code),
                    primary = true,
                    onClick = onOpenGroup,
                )
            }
        }
        item {
            HomeChip(
                label = stringResource(R.string.home_create_group),
                primary = activeGroupCode == null,
                onClick = { guarded { viewModel.createGroup(); onOpenGroup() } },
            )
        }
        item { HomeChip(stringResource(R.string.home_join_group), onClick = { guarded(onJoinGroup) }) }
        item { HomeChip(stringResource(R.string.home_my_words), onClick = onWords) }
        item { HomeChip(stringResource(R.string.home_settings), onClick = onSettings) }
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

@Composable
private fun HomeChip(label: String, secondary: String? = null, primary: Boolean = false, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = secondary?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        colors = if (primary) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        onClick = onClick,
    )
}
