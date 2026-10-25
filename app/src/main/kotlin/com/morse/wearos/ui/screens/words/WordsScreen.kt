package com.morse.wearos.ui.screens.words

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import com.morse.wearos.ui.components.SectionHeader
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Confirmation
import androidx.wear.compose.material.dialog.Dialog
import com.morse.wearos.R
import com.morse.wearos.data.AddResult
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.data.WordsRepository
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.components.rememberTextInputLauncher
import com.morse.wearos.ui.screens.send.WordChip
import com.morse.wearos.ui.theme.MorseColors

@Composable
fun WordsScreen(viewModel: WordsViewModel = containerViewModel { WordsViewModel(it.wordsRepository) }) {
    val customWords by viewModel.customWords.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<String?>(null) }
    var rejected by remember { mutableStateOf<AddResult?>(null) }

    val launchAddWord = rememberTextInputLauncher(label = stringResource(R.string.words_input_label)) { word ->
        viewModel.add(word) { result -> rejected = result }
    }

    MorseScaffold(title = stringResource(R.string.words_title), glow = MorseColors.Purple) {
        item {
            Platter(
                title = stringResource(R.string.words_add),
                icon = R.drawable.ph_plus,
                accent = true,
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
            WordChip(word = word, icon = R.drawable.ph_trash, iconTint = MorseColors.Red, onClick = { pendingDelete = word })
        }
        item { SectionHeader(stringResource(R.string.words_defaults_header)) }
        items(DefaultWords.resources.size) { index ->
            WordChip(word = stringResource(DefaultWords.resources[index]), icon = null, onClick = null)
        }
    }

    Dialog(showDialog = rejected != null, onDismissRequest = { rejected = null }) {
        Confirmation(
            onTimeout = { rejected = null },
            icon = { Icon(painterResource(R.drawable.ph_warning_circle), contentDescription = null, tint = MorseColors.Accent) },
        ) {
            Text(
                text = if (rejected == AddResult.DUPLICATE) {
                    stringResource(R.string.words_duplicate)
                } else {
                    // Blank input never gets here (the text launcher drops it), so INVALID means too long.
                    stringResource(R.string.words_too_long, WordsRepository.MAX_WORD_LENGTH)
                },
                textAlign = TextAlign.Center,
            )
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
                    Icon(painterResource(R.drawable.ph_trash), contentDescription = stringResource(R.string.action_delete))
                }
            },
        )
    }
}
