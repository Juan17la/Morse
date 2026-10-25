package com.morse.wearos.ui.screens.incoming

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.shortName
import com.morse.wearos.ui.components.AiHeader
import com.morse.wearos.ui.components.Avatar
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.components.rememberSendWithFeedback
import com.morse.wearos.ui.screens.send.WordChip
import com.morse.wearos.ui.theme.MorseColors
import com.morse.wearos.ui.theme.MorseGradients
import com.morse.wearos.ui.theme.avatarColor
import com.morse.wearos.ui.theme.morseSpring
import com.morse.wearos.ui.theme.rememberReduceMotion
import kotlinx.coroutines.delay

@Composable
fun IncomingScreen(
    onReplied: () -> Unit,
    viewModel: IncomingViewModel = containerViewModel { IncomingViewModel(it.groupSession, it.assistant) },
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val replies by viewModel.replies.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    var playToken by remember { mutableIntStateOf(0) }
    val compact = isCompactScreen()
    val reduceMotion = rememberReduceMotion()
    val current = message
    val playback = rememberPlayback(current?.id, current?.morse.orEmpty(), playToken)
    val reply = rememberSendWithFeedback(viewModel::reply, onReplied)

    MorseScaffold(glow = current?.senderName?.let(::avatarColor) ?: MorseColors.Blue) {
        if (current == null) {
            item {
                Text(stringResource(R.string.incoming_empty), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.title3, textAlign = TextAlign.Center)
            }
            item {
                Text(
                    stringResource(R.string.incoming_empty_hint),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            return@MorseScaffold
        }
        item {
            // Keyed by message so each new arrival pops in on a spring.
            val pop = remember(current.id) { Animatable(if (reduceMotion) 1f else 0.6f) }
            LaunchedEffect(current.id) { pop.animateTo(1f, morseSpring()) }
            Column(
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Avatar(current.senderName, size = 20.dp)
                    Text(
                        text = stringResource(R.string.incoming_from, shortName(current.senderName)),
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Beacon(playback)
                Text(
                    text = revealed(current.word, current.morse, playback),
                    // Custom words run up to 20 chars; only short ones get the display size.
                    style = if (compact || current.word.length > 8) MaterialTheme.typography.title1 else MaterialTheme.typography.display3,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        item {
            MorseVisualizer(current.morse, playback, dot = if (compact) 6.dp else 8.dp)
        }
        item { ReplayButton { viewModel.replay(); playToken++ } }
        if (connection == ConnectionState.Connected) {
            if (current.senderId != viewModel.selfId) {
                item {
                    Platter(
                        title = stringResource(R.string.incoming_pass_on, current.word),
                        morse = current.morse,
                        icon = R.drawable.ph_paper_plane_tilt,
                        iconTint = MorseColors.Green,
                        onClick = { reply(current.word) },
                    )
                }
            }
            smartReplies(replies, reply)
        }
    }
}

private fun ScalingLazyListScope.smartReplies(replies: Replies, onReply: (String) -> Unit) {
    when (replies) {
        Replies.None -> Unit
        Replies.Loading -> item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(stringResource(R.string.incoming_replies_loading), style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.onSurfaceVariant)
            }
        }
        is Replies.Ready -> {
            item { AiHeader(stringResource(R.string.incoming_quick_replies), replies.fromAi) }
            items(replies.options.size) { index ->
                val word = replies.options[index]
                WordChip(word = word, icon = R.drawable.ph_sparkle, iconTint = MorseColors.Purple) { onReply(word) }
            }
        }
    }
}

/** The round "feel it again" button under a played word. */
@Composable
internal fun ReplayButton(onClick: () -> Unit) {
    val size = if (isCompactScreen()) 48.dp else 52.dp
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .size(size)
            .clip(CircleShape)
            .background(MorseGradients.Accent)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ph_arrow_counter_clockwise),
            contentDescription = stringResource(R.string.incoming_replay),
            tint = Color.Black,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** Where the vibration is right now: [active] is the symbol index in the Morse string, -1 in a gap. */
internal class Playback(val active: Int, val finished: Boolean, val dash: Boolean)

@Composable
internal fun rememberPlayback(messageId: String?, morse: String, playToken: Int, unitMs: Long = MorseTiming.DEFAULT_UNIT_MS): Playback {
    var active by remember(messageId) { mutableIntStateOf(-1) }
    var finished by remember(messageId) { mutableStateOf(false) }
    LaunchedEffect(messageId, playToken) {
        finished = false
        for (step in MorseTiming.steps(morse, unitMs)) {
            active = if (step.on) step.symbolIndex else -1
            delay(step.durationMs)
        }
        active = -1
        finished = true
    }
    return Playback(active, finished, dash = morse.getOrNull(active) == '-')
}

/** One big symbol that lights exactly while the wrist buzzes, named in plain words. */
@Composable
internal fun Beacon(playback: Playback) {
    val on = playback.active >= 0
    val glow by animateFloatAsState(if (on) 1f else 0f, morseSpring(), label = "beacon")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)) {
        Box(
            Modifier
                .size(width = if (playback.dash) 44.dp else 16.dp, height = 16.dp)
                .graphicsLayer { alpha = 0.25f + 0.75f * glow; scaleX = 0.9f + 0.1f * glow; scaleY = 0.9f + 0.1f * glow }
                .clip(CircleShape)
                .background(MorseGradients.Accent),
        )
        Text(
            text = when {
                !on -> " "
                playback.dash -> stringResource(R.string.incoming_long)
                else -> stringResource(R.string.incoming_short)
            },
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
    }
}

/** The word fills in as it is felt: played letters white, the current one in the accent, the rest dim. */
@Composable
internal fun revealed(word: String, morse: String, playback: Playback) = buildAnnotatedString {
    // Letter groups in the Morse string line up one-to-one with the word's encodable characters.
    val starts = remember(morse) { Regex("[.-]+").findAll(morse).map { it.range.first }.toList() }
    val current = if (playback.active >= 0) starts.indexOfLast { it <= playback.active } else -1
    var ordinal = 0
    word.forEach { c ->
        val color = if (!MorseCodec.isEncodable(c)) Color.White else {
            val k = ordinal++
            when {
                playback.finished || k < current -> Color.White
                k == current -> MorseColors.Accent
                else -> MorseColors.Grey
            }
        }
        withStyle(SpanStyle(color = color)) { append(c) }
    }
}

/**
 * The Morse as lit pills: dots are circles, dashes are capsules, letters wrap as units and
 * words are split by a thin bar. The symbol vibrating right now swells and glows in the accent.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MorseVisualizer(morse: String, playback: Playback, dot: Dp) {
    // Letters keep each symbol's index in the string, which is what MorseStep.symbolIndex points at.
    val letters = remember(morse) {
        Regex("[.-]+|/").findAll(morse).map { it.range.first to it.value }.toList()
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(dot * 1.8f, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(dot * 1.5f),
    ) {
        letters.forEach { (start, letter) ->
            if (letter == "/") {
                Box(Modifier.size(width = 1.5.dp, height = dot * 1.6f).background(MorseColors.Grey, CircleShape))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(dot / 2), verticalAlignment = Alignment.CenterVertically) {
                    letter.forEachIndexed { i, symbol ->
                        MorseSymbol(dash = symbol == '-', active = start + i == playback.active, finished = playback.finished, dot = dot)
                    }
                }
            }
        }
    }
}

@Composable
private fun MorseSymbol(dash: Boolean, active: Boolean, finished: Boolean, dot: Dp) {
    val glow by animateFloatAsState(if (active) 1f else 0f, morseSpring(), label = "symbol")
    val idle = if (finished) Color.White else MorseColors.Grey
    Box(
        Modifier
            .graphicsLayer {
                val s = 1f + 0.35f * glow
                scaleX = s
                scaleY = s
                alpha = 0.55f + 0.45f * maxOf(glow, if (finished) 1f else 0f)
            }
            .size(width = if (dash) dot * 3 else dot, height = dot)
            .clip(CircleShape)
            .background(if (glow > 0.01f) MorseGradients.Accent else SolidColor(idle)),
    )
}
