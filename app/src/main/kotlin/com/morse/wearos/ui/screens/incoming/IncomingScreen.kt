package com.morse.wearos.ui.screens.incoming

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.ui.components.isCompactScreen
import kotlinx.coroutines.delay

@Composable
fun IncomingScreen(
    viewModel: IncomingViewModel = containerViewModel { IncomingViewModel(it.groupSession) },
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    var playToken by remember { mutableIntStateOf(0) }
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 1)
    val compact = isCompactScreen()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        val current = message
        ScalingLazyColumn(state = listState, autoCentering = AutoCenteringParams(itemIndex = 0)) {
            if (current == null) {
                item { Text(stringResource(R.string.incoming_empty), textAlign = TextAlign.Center) }
                return@ScalingLazyColumn
            }
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.incoming_from, current.senderName),
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    Text(
                        text = current.word,
                        style = if (compact) MaterialTheme.typography.title1 else MaterialTheme.typography.display3,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
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
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.incoming_replay))
                }
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
