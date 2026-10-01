package com.morse.wearos.ui.screens.join

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberPickerState
import com.morse.wearos.R
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupConfig
import com.morse.wearos.ui.components.ErrorText
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.components.screenPct

@Composable
fun JoinScreen(
    onJoined: () -> Unit,
    viewModel: JoinViewModel = containerViewModel { JoinViewModel(it.groupSession) },
) {
    val connection by viewModel.state.collectAsStateWithLifecycle()
    var requested by remember { mutableStateOf(false) }
    val pickers = List(GroupConfig.CODE_LENGTH) { rememberPickerState(initialNumberOfOptions = 10) }

    LaunchedEffect(connection, requested) {
        if (requested && connection == ConnectionState.Connected) onJoined()
    }

    Scaffold(timeText = { TimeText() }) {
        Column(
            // Centred vertically so the content sits in the widest band of a round face.
            modifier = Modifier.fillMaxSize().padding(horizontal = screenPct(0.1f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.join_title),
                // The round bezel is narrowest up here; inset so long or scaled text wraps instead of clipping.
                modifier = Modifier.padding(horizontal = screenPct(0.08f)),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(4.dp))
            val current = connection
            when {
                requested && current == ConnectionState.Joining -> {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                    Text(stringResource(R.string.join_connecting), textAlign = TextAlign.Center)
                }
                requested && current is ConnectionState.Error -> {
                    ErrorText(current)
                    Spacer(Modifier.height(8.dp))
                    CompactChip(
                        label = { Text(stringResource(R.string.join_try_again)) },
                        colors = ChipDefaults.primaryChipColors(),
                        onClick = { requested = false; viewModel.cancel() },
                    )
                }
                else -> {
                    CodePickers(pickers)
                    Spacer(Modifier.height(8.dp))
                    CompactChip(
                        label = { Text(stringResource(R.string.join_action)) },
                        colors = ChipDefaults.primaryChipColors(),
                        onClick = {
                            requested = true
                            viewModel.join(pickers.joinToString("") { it.selectedOption.toString() })
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CodePickers(pickers: List<PickerState>) {
    val compact = isCompactScreen()
    val digitStyle = if (compact) MaterialTheme.typography.title1 else MaterialTheme.typography.display3
    val pickerSize = Modifier.size(width = screenPct(0.15f), height = screenPct(if (compact) 0.26f else 0.32f))
    Row(horizontalArrangement = Arrangement.spacedBy(screenPct(0.02f))) {
        pickers.forEachIndexed { index, state ->
            Picker(
                state = state,
                contentDescription = stringResource(R.string.join_digit_description, index + 1),
                modifier = pickerSize,
            ) { option ->
                Text(text = option.toString(), style = digitStyle)
            }
        }
    }
}
