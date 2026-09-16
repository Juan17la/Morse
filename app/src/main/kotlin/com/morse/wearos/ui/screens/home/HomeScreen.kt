package com.morse.wearos.ui.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.morse.wearos.R
import com.morse.wearos.data.TransportMode
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.permissions.rememberWifiDirectPermissionRequest

@Composable
fun HomeScreen(
    onGroupCreated: () -> Unit,
    onJoinGroup: () -> Unit,
    onWords: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = containerViewModel { HomeViewModel(it.groupSession, it.settingsRepository) },
) {
    val transportMode by viewModel.transportMode.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val requestPermission = rememberWifiDirectPermissionRequest(
        onGranted = { pendingAction?.invoke(); pendingAction = null },
        onDenied = { pendingAction = null; permissionDenied = true },
    )

    fun guarded(action: () -> Unit) {
        if (transportMode == TransportMode.WIFI_DIRECT) {
            pendingAction = action
            requestPermission()
        } else {
            action()
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(state = listState) {
            item { ListHeader { Text(stringResource(R.string.app_name)) } }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_create_group)) },
                    colors = ChipDefaults.primaryChipColors(),
                    onClick = { guarded { viewModel.createGroup(); onGroupCreated() } },
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_join_group)) },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = { guarded(onJoinGroup) },
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_my_words)) },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = onWords,
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_settings)) },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = onSettings,
                )
            }
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
