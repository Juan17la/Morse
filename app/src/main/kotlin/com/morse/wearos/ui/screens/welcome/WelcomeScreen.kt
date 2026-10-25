package com.morse.wearos.ui.screens.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.components.rememberTextInputLauncher
import com.morse.wearos.ui.screens.incoming.Beacon
import com.morse.wearos.ui.screens.incoming.MorseVisualizer
import com.morse.wearos.ui.screens.incoming.Playback
import com.morse.wearos.ui.screens.incoming.ReplayButton
import com.morse.wearos.ui.screens.incoming.rememberPlayback
import com.morse.wearos.ui.screens.incoming.revealed
import com.morse.wearos.ui.theme.MorseColors
import kotlinx.coroutines.delay

/**
 * First launch only. The app explains itself by doing it: a word buzzes on the wrist while its
 * letters and Morse light up, then one line names what was felt. Name and Start come last.
 */
@Composable
fun WelcomeScreen(
    onStart: () -> Unit,
    viewModel: WelcomeViewModel = containerViewModel {
        WelcomeViewModel(it.identityRepository, it.settingsRepository, it.hapticPlayer)
    },
) {
    val name by viewModel.displayName.collectAsStateWithLifecycle()
    val word = stringResource(R.string.welcome_word)
    val morse = remember(word) { MorseCodec.encode(word) }
    // 0 = not played yet; a short pause first so the eyes are on the screen when the wrist buzzes.
    var playToken by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(700)
        playToken = 1
    }
    LaunchedEffect(playToken) { if (playToken > 0) viewModel.play(morse) }
    val playback = if (playToken == 0) Playback(active = -1, finished = false, dash = false) else rememberPlayback("welcome", morse, playToken)
    val compact = isCompactScreen()

    val launchNameInput = rememberTextInputLauncher(
        label = stringResource(R.string.settings_name_input_label),
        onResult = viewModel::setName,
    )

    MorseScaffold(glow = MorseColors.Accent) {
        item { Beacon(playback) }
        item {
            Text(
                text = revealed(word, morse, playback),
                style = if (compact) MaterialTheme.typography.display3 else MaterialTheme.typography.display2,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
        item { MorseVisualizer(morse, playback, dot = if (compact) 7.dp else 9.dp) }
        item {
            AnimatedVisibility(visible = playback.finished, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.welcome_felt, word),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        item { ReplayButton { playToken++ } }
        item {
            Platter(
                title = stringResource(R.string.welcome_name),
                subtitle = name,
                avatar = name,
                onClick = launchNameInput,
            )
        }
        item {
            Platter(
                title = stringResource(R.string.welcome_start),
                icon = R.drawable.ph_users_three,
                accent = true,
                onClick = onStart,
            )
        }
    }
}
