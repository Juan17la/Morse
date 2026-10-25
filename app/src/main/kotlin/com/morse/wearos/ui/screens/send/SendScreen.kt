package com.morse.wearos.ui.screens.send

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.morse.wearos.ui.components.SectionHeader
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.data.DefaultWords
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.components.rememberSendWithFeedback
import com.morse.wearos.ui.components.rememberTextInputLauncher
import com.morse.wearos.ui.theme.MorseColors

@Composable
fun SendScreen(
    onSent: () -> Unit,
    onDictated: (String) -> Unit,
    onFindGroup: () -> Unit,
    viewModel: SendViewModel = containerViewModel { SendViewModel(it.groupSession, it.wordsRepository) },
) {
    val customWords by viewModel.customWords.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val connected = connection == ConnectionState.Connected
    val send = rememberSendWithFeedback(viewModel::send, onSent)

    // Wear's RemoteInput offers voice and keyboard; the result is condensed on the next screen.
    val launchDictation = rememberTextInputLauncher(
        label = stringResource(R.string.send_dictate_label),
        onResult = onDictated,
    )

    MorseScaffold(title = stringResource(R.string.send_title), glow = MorseColors.Blue) {
        if (!connected) {
            item {
                Platter(
                    title = stringResource(R.string.send_not_connected),
                    subtitle = stringResource(R.string.send_not_connected_hint),
                    icon = R.drawable.ph_users_three,
                    iconTint = MorseColors.Green,
                    onClick = onFindGroup,
                )
            }
        }
        item {
            Platter(
                title = stringResource(R.string.send_dictate),
                subtitle = stringResource(R.string.send_dictate_hint),
                icon = R.drawable.ph_microphone,
                accent = true,
                enabled = connected,
                onClick = launchDictation,
            )
        }
        item { SectionHeader(stringResource(R.string.send_quick_header)) }
        items(DefaultWords.resources.size) { index ->
            val word = stringResource(DefaultWords.resources[index])
            WordChip(word = word, enabled = connected) { send(word) }
        }
        if (customWords.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.send_custom_header)) }
            items(customWords.size) { index ->
                val word = customWords[index]
                WordChip(word = word, enabled = connected) { send(word) }
            }
        }
    }
}

/** A word with its Morse underneath; shared with the Words, Incoming and Condense screens. */
@Composable
fun WordChip(
    word: String,
    enabled: Boolean = true,
    accent: Boolean = false,
    @DrawableRes icon: Int? = R.drawable.ph_paper_plane_tilt,
    iconTint: Color = MorseColors.Blue,
    onClick: (() -> Unit)?,
) {
    Platter(
        title = word,
        morse = MorseCodec.encode(word),
        icon = icon,
        iconTint = iconTint,
        accent = accent,
        enabled = enabled,
        onClick = onClick,
    )
}
