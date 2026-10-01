package com.morse.wearos.ui.screens.send

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.ui.components.MorseScaffold

@Composable
fun SendScreen(
    onSent: () -> Unit,
    viewModel: SendViewModel = containerViewModel { SendViewModel(it.groupSession, it.wordsRepository) },
) {
    val customWords by viewModel.customWords.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val connected = connection == ConnectionState.Connected

    MorseScaffold {
        item { ListHeader { Text(stringResource(R.string.send_title)) } }
        if (!connected) {
            item {
                Text(stringResource(R.string.send_not_connected), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
        items(DefaultWords.resources.size) { index ->
            val word = stringResource(DefaultWords.resources[index])
            WordChip(word = word, enabled = connected, primary = true) { viewModel.send(word, onSent) }
        }
        if (customWords.isNotEmpty()) {
            item { ListHeader { Text(stringResource(R.string.send_custom_header)) } }
            items(customWords.size) { index ->
                val word = customWords[index]
                WordChip(word = word, enabled = connected) { viewModel.send(word, onSent) }
            }
        }
    }
}

/** A word with its Morse underneath; shared with the Words screen. */
@Composable
fun WordChip(word: String, enabled: Boolean = true, primary: Boolean = false, onClick: () -> Unit) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(word, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = {
            Text(MorseCodec.encode(word), fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        colors = if (primary) ChipDefaults.primaryChipColors() else ChipDefaults.secondaryChipColors(),
        enabled = enabled,
        onClick = onClick,
    )
}
