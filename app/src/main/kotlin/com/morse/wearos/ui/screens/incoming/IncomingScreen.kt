package com.morse.wearos.ui.screens.incoming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.morse.wearos.R
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.di.containerViewModel
import kotlinx.coroutines.delay

@Composable
fun IncomingScreen(
    viewModel: IncomingViewModel = containerViewModel { IncomingViewModel(it.groupSession) },
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    var playToken by remember { mutableIntStateOf(0) }

    Scaffold(timeText = { TimeText() }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val current = message
            if (current == null) {
                Text(stringResource(R.string.incoming_empty), textAlign = TextAlign.Center)
                return@Column
            }
            Text(
                text = stringResource(R.string.incoming_from, current.senderName),
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                text = current.word,
                style = MaterialTheme.typography.display3,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            MorseVisualizer(morse = current.morse, playToken = playToken, messageId = current.id)
            Spacer(Modifier.height(10.dp))
            Button(onClick = { viewModel.replay(); playToken++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.incoming_replay))
            }
        }
    }
}

@Composable
private fun MorseVisualizer(morse: String, playToken: Int, messageId: String) {
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
    Text(
        text = buildAnnotatedString {
            morse.forEachIndexed { index, c ->
                val style = when {
                    index == activeIndex -> SpanStyle(color = primary, fontWeight = FontWeight.Bold)
                    finished -> SpanStyle(color = MaterialTheme.colors.onSurface)
                    else -> SpanStyle(color = dim)
                }
                withStyle(style) { append(c) }
            }
        },
        fontFamily = FontFamily.Monospace,
        fontSize = 18.sp,
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center,
    )
}
