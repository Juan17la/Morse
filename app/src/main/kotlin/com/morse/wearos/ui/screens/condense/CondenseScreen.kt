package com.morse.wearos.ui.screens.condense

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import com.morse.wearos.ui.theme.MorseGradients
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
import com.morse.wearos.ui.components.SectionHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.AiHeader
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.rememberSendWithFeedback
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
    val send = rememberSendWithFeedback(viewModel::send, onSent)

    MorseScaffold(title = stringResource(R.string.condense_title), glow = MorseColors.Purple) {
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
            item { WordChip(word = suggestion.text, accent = true) { send(suggestion.text) } }
            item { BuzzTime(before = state.originalSeconds, after = state.suggestionSeconds) }
        }

        val original = state.originalSendable
        if (original != null && original != suggestion?.text) {
            item { SectionHeader(stringResource(R.string.condense_as_dictated)) }
            item { WordChip(word = original) { send(original) } }
        }
    }
}

/** What shortening buys, shown as time on the wrist: the dictated length against the short one. */
@Composable
private fun BuzzTime(before: Int, after: Int) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(Unit) { grow.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val longest = maxOf(before, after, 1)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (before > after) {
            Text(
                text = stringResource(R.string.condense_saved, before - after),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.title3,
                color = MorseColors.Green,
                textAlign = TextAlign.Center,
            )
        }
        BuzzBar(before, longest, grow.value, SolidColor(MorseColors.Grey))
        BuzzBar(after, longest, grow.value, MorseGradients.Accent)
    }
}

@Composable
private fun BuzzBar(seconds: Int, longest: Int, progress: Float, brush: Brush) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.weight(1f)) {
            Box(
                Modifier
                    .fillMaxWidth((seconds.toFloat() / longest * progress).coerceAtLeast(0.03f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(brush),
            )
        }
        Text(
            text = stringResource(R.string.condense_seconds, seconds),
            modifier = Modifier.width(30.dp),
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}
