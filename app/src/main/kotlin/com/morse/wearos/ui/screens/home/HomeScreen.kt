package com.morse.wearos.ui.screens.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import androidx.wear.compose.material.PositionIndicator
import com.morse.wearos.R

@Composable
fun HomeScreen(
    onCreateGroup: () -> Unit,
    onJoinGroup: () -> Unit,
    onWords: () -> Unit,
    onSettings: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

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
                    onClick = onCreateGroup,
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.home_join_group)) },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = onJoinGroup,
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
}
