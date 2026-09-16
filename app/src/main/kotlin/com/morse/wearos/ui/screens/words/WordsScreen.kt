package com.morse.wearos.ui.screens.words

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
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
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.rememberTextInputLauncher

@Composable
fun WordsScreen(viewModel: WordsViewModel = containerViewModel { WordsViewModel(it.wordsRepository) }) {
    val customWords by viewModel.customWords.collectAsStateWithLifecycle()
    val listState = rememberScalingLazyListState()
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    val launchAddWord = rememberTextInputLauncher(
        label = stringResource(R.string.words_input_label),
        onResult = viewModel::add,
    )

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        ScalingLazyColumn(state = listState) {
            item { ListHeader { Text(stringResource(R.string.words_title)) } }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.words_add)) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    colors = ChipDefaults.primaryChipColors(),
                    onClick = launchAddWord,
                )
            }
            if (customWords.isEmpty()) {
                item { Text(stringResource(R.string.words_empty)) }
            }
            items(customWords.size) { index ->
                val word = customWords[index]
                WordChip(word = word, onClick = { pendingDelete = word })
            }
            item { ListHeader { Text(stringResource(R.string.words_defaults_header)) } }
            items(DefaultWords.resources.size) { index ->
                WordChip(word = stringResource(DefaultWords.resources[index]), onClick = null)
            }
        }
    }

    Dialog(showDialog = pendingDelete != null, onDismissRequest = { pendingDelete = null }) {
        val word = pendingDelete ?: return@Dialog
        Alert(
            title = { Text(stringResource(R.string.words_delete_title, word)) },
            negativeButton = {
                Button(onClick = { pendingDelete = null }, colors = ButtonDefaults.secondaryButtonColors()) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            positiveButton = {
                Button(onClick = { viewModel.remove(word); pendingDelete = null }) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                }
            },
        )
    }
}

@Composable
private fun WordChip(word: String, onClick: (() -> Unit)?) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(word, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = { Text(MorseCodec.encode(word), fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        colors = ChipDefaults.secondaryChipColors(),
        enabled = onClick != null,
        onClick = onClick ?: {},
    )
}
