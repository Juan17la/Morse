package com.morse.wearos.ui.screens.words

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.morse.wearos.R
import com.morse.wearos.data.AddResult
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.data.WordsRepository
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.rememberTextInputLauncher
import com.morse.wearos.ui.screens.send.WordChip

@Composable
fun WordsScreen(viewModel: WordsViewModel = containerViewModel { WordsViewModel(it.wordsRepository) }) {
    val customWords by viewModel.customWords.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val launchAddWord = rememberTextInputLauncher(label = stringResource(R.string.words_input_label)) { word ->
        viewModel.add(word) { result ->
            val text = if (result == AddResult.DUPLICATE) {
                context.getString(R.string.words_duplicate)
            } else {
                // Blank input never gets here (the text launcher drops it), so INVALID means too long.
                context.getString(R.string.words_too_long, WordsRepository.MAX_WORD_LENGTH)
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    MorseScaffold {
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
            item {
                Text(stringResource(R.string.words_empty), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
        }
        items(customWords.size) { index ->
            val word = customWords[index]
            WordChip(word = word, onClick = { pendingDelete = word })
        }
        item { ListHeader { Text(stringResource(R.string.words_defaults_header)) } }
        items(DefaultWords.resources.size) { index ->
            WordChip(word = stringResource(DefaultWords.resources[index]), enabled = false, onClick = {})
        }
    }

    Dialog(showDialog = pendingDelete != null, onDismissRequest = { pendingDelete = null }) {
        val word = pendingDelete ?: return@Dialog
        Alert(
            title = { Text(stringResource(R.string.words_delete_title, word), textAlign = TextAlign.Center) },
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
