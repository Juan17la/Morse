package com.morse.wearos.ui.screens.condense

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.AiHeader
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.screens.send.WordChip
import com.morse.wearos.ui.theme.MorseColors

/** Shows dictated text next to its AI-condensed version and how much vibration time that saves. */
@Composable
fun CondenseScreen(
    text: String,
    onSent: () -> Unit,
    viewModel: CondenseViewModel = containerViewModel { CondenseViewModel(it.groupSession, it.assistant, text) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MorseScaffold(title = stringResource(R.string.condense_title)) {
        item {
            Text(
                text = "“${state.original}”",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val suggestion = state.suggestion
        if (suggestion == null) {
            item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
        } else {
            item { AiHeader(stringResource(R.string.condense_shortened), suggestion.fromAi) }
            item { WordChip(word = suggestion.text, accent = true) { viewModel.send(suggestion.text, onSent) } }
            item {
                Text(
                    text = stringResource(R.string.condense_seconds, state.originalSeconds, state.suggestionSeconds),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.caption2,
                    color = MorseColors.Green,
                    textAlign = TextAlign.Center,
                )
            }
        }

        val original = state.originalSendable
        if (original != null && original != suggestion?.text) {
            item { ListHeader { Text(stringResource(R.string.condense_as_dictated)) } }
            item { WordChip(word = original) { viewModel.send(original, onSent) } }
        }
    }
}
