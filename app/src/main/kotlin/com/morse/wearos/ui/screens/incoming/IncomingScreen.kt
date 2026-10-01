package com.morse.wearos.ui.screens.incoming

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.ui.components.AiHeader
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.screens.send.WordChip
import com.morse.wearos.ui.theme.MorseColors
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
    val current = message

    MorseScaffold {
        if (current == null) {
            item {
                Text(stringResource(R.string.incoming_empty), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            }
            return@MorseScaffold
        }
        item {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.incoming_from, current.senderName),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = current.word,
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
            MorseVisualizer(
                morse = current.morse,
                playToken = playToken,
                messageId = current.id,
                fontSize = if (compact) 15.sp else 18.sp,
            )
        }
        item {
            Button(
                modifier = Modifier.padding(top = 4.dp),
                onClick = { viewModel.replay(); playToken++ },
            ) {
                Icon(painterResource(R.drawable.ph_arrow_counter_clockwise), contentDescription = stringResource(R.string.incoming_replay))
            }
        }
        if (connection == ConnectionState.Connected) {
            smartReplies(replies) { viewModel.reply(it, onReplied) }
        }
    }
}

private fun ScalingLazyListScope.smartReplies(replies: Replies, onReply: (String) -> Unit) {
    when (replies) {
        Replies.None -> Unit
        Replies.Loading -> item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
        is Replies.Ready -> {
            item { AiHeader(stringResource(R.string.incoming_quick_replies), replies.fromAi) }
            items(replies.options.size) { index ->
                val word = replies.options[index]
                WordChip(word = word, icon = R.drawable.ph_sparkle, iconTint = MorseColors.Purple) { onReply(word) }
            }
        }
    }
}

@Composable
private fun MorseVisualizer(morse: String, playToken: Int, messageId: String, fontSize: TextUnit) {
    var activeIndex by remember(messageId) { mutableIntStateOf(-1) }
    var finished by remember(messageId) { mutableStateOf(false) }

    LaunchedEffect(messageId, playToken) {
        finished = false
        for (step in MorseTiming.steps(morse)) {
            activeIndex = if (step.on) step.symbolIndex else -1
            delay(step.durationMs)
        }
        activeIndex = -1
        finished = true
    }

    val primary = MaterialTheme.colors.primary
    val dim = MaterialTheme.colors.onSurfaceVariant
    val done = MaterialTheme.colors.onSurface
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = buildAnnotatedString {
            morse.forEachIndexed { index, c ->
                val style = when {
                    index == activeIndex -> SpanStyle(color = primary, fontWeight = FontWeight.Bold)
                    finished -> SpanStyle(color = done)
                    else -> SpanStyle(color = dim)
                }
                withStyle(style) { append(c) }
            }
        },
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center,
    )
}
