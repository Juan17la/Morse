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
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.join_title),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            when {
                requested && connection == ConnectionState.Joining -> {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                    Text(stringResource(R.string.join_connecting))
                }
                requested && connection is ConnectionState.Error -> {
                    Text(
                        text = (connection as ConnectionState.Error).message,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.error,
                    )
                    Spacer(Modifier.height(8.dp))
                    Chip(
                        label = { Text(stringResource(R.string.action_retry)) },
                        colors = ChipDefaults.primaryChipColors(),
                        onClick = { requested = false; viewModel.cancel() },
                    )
                }
                else -> {
                    CodePickers(pickers)
                    Spacer(Modifier.height(8.dp))
                    Chip(
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
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        pickers.forEachIndexed { index, state ->
            Picker(
                state = state,
                contentDescription = stringResource(R.string.join_digit_description, index + 1),
                modifier = Modifier.size(width = 32.dp, height = 84.dp),
            ) { option ->
                Text(text = option.toString(), style = MaterialTheme.typography.display3)
            }
        }
    }
}
