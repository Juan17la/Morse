package com.morse.wearos.ui.screens.send

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState

@Composable
fun SendScreen(
    onSent: () -> Unit,
    viewModel: SendViewModel = containerViewModel { SendViewModel(it.groupSession, it.wordsRepository) },
) {
    val customWords by viewModel.customWords.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    val connected = connection == ConnectionState.Connected

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(state = listState) {
            item { ListHeader { Text(stringResource(R.string.send_title)) } }
            if (!connected) {
                item { Text(stringResource(R.string.send_not_connected), textAlign = TextAlign.Center) }
            }
            items(DefaultWords.resources.size) { index ->
                val word = stringResource(DefaultWords.resources[index])
                SendChip(word = word, enabled = connected, primary = true) { viewModel.send(word, onSent) }
            }
            if (customWords.isNotEmpty()) {
                item { ListHeader { Text(stringResource(R.string.send_custom_header)) } }
                items(customWords.size) { index ->
                    val word = customWords[index]
                    SendChip(word = word, enabled = connected, primary = false) { viewModel.send(word, onSent) }
                }
            }
        }
    }
}

@Composable
private fun SendChip(word: String, enabled: Boolean, primary: Boolean, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(word) },
        secondaryLabel = { Text(MorseCodec.encode(word), fontFamily = FontFamily.Monospace) },
        colors = if (primary) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        enabled = enabled,
        onClick = onClick,
    )
}
