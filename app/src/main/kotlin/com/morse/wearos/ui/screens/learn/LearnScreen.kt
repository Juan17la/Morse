package com.morse.wearos.ui.screens.learn

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.MorseGlyphs
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.SectionHeader
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.screens.incoming.Beacon
import com.morse.wearos.ui.screens.incoming.MorseVisualizer
import com.morse.wearos.ui.screens.incoming.Playback
import com.morse.wearos.ui.screens.incoming.ReplayButton
import com.morse.wearos.ui.screens.incoming.rememberPlayback
import com.morse.wearos.ui.screens.incoming.revealed
import com.morse.wearos.ui.theme.MorseColors

/** SOS at three times the normal pace: slow enough to count the beats while the wrist buzzes. */
private const val SLOW_UNIT_MS = MorseTiming.DEFAULT_UNIT_MS * 3

/** How to read Morse by feel: one slow, narrated word, then the four timings that matter. */
@Composable
fun LearnScreen(
    viewModel: LearnViewModel = containerViewModel { LearnViewModel(it.hapticPlayer) },
) {
    val word = "SOS"
    val morse = remember { MorseCodec.encode(word) }
    var playToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(playToken) { if (playToken > 0) viewModel.play(morse, SLOW_UNIT_MS) }
    val playback = if (playToken == 0) Playback(active = -1, finished = false, dash = false) else rememberPlayback("learn", morse, playToken, SLOW_UNIT_MS)

    MorseScaffold(title = stringResource(R.string.learn_title), glow = MorseColors.Teal) {
        item {
            Text(
                text = stringResource(R.string.learn_intro),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        item { Beacon(playback) }
        item {
            Text(
                text = revealed(word, morse, playback),
                modifier = Modifier.fillMaxWidth(),
                style = if (isCompactScreen()) MaterialTheme.typography.display3 else MaterialTheme.typography.display2,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        item { MorseVisualizer(morse, playback, dot = if (isCompactScreen()) 7.dp else 9.dp) }
        item { ReplayButton { playToken++ } }
        item { SectionHeader(stringResource(R.string.learn_beats_header)) }
        item { Rule(".", R.string.learn_dot) }
        item { Rule("-", R.string.learn_dash) }
        item { Rule(null, R.string.learn_letter_gap) }
        item { Rule(null, R.string.learn_word_gap) }
    }
}

/** A drawn symbol (or a blank for pauses) beside what it means in beats. */
@Composable
private fun Rule(symbol: String?, label: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (symbol != null) MorseGlyphs(symbol, Color.White, Modifier.width(18.dp), dot = 5.dp) else Text("·", color = MorseColors.Grey, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
        Text(stringResource(label), style = MaterialTheme.typography.caption1, color = Color.White)
    }
}
