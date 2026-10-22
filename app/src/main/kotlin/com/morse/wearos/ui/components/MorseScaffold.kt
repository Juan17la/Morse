package com.morse.wearos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.morse.wearos.ui.theme.MorseColors

/**
 * Standard list screen. Margins follow the Wear responsive guidance (a percentage of the
 * screen, wider on round faces where the corners are cut), so every size gets the same layout.
 * [title] is the watchOS-style large gradient title (left-aligned on square faces, centred on round).
 * [glow] tints a soft light that falls from the top of the face; changing it cross-fades.
 */
@Composable
fun MorseScaffold(
    title: String? = null,
    glow: Color = MorseColors.Accent,
    // Without auto-centering, start at the first item so headers sit below TimeText.
    listState: ScalingLazyListState = rememberScalingLazyListState(initialCenterItemIndex = 0),
    content: ScalingLazyListScope.() -> Unit,
) {
    val round = LocalConfiguration.current.isScreenRound
    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) },
    ) {
        TopGlow(glow)
        ScalingLazyColumn(
            state = listState,
            autoCentering = null,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            contentPadding = PaddingValues(
                start = screenPct(if (round) 0.052f else 0.02f),
                end = screenPct(if (round) 0.052f else 0.02f),
                top = screenPct(0.1f),
                // Half the face minus about half a button, so the last item can scroll up to the centre.
                bottom = (LocalConfiguration.current.screenHeightDp / 2).dp - 26.dp,
            ),
        ) {
            if (title != null) {
                item {
                    Text(
                        text = title,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.title2.copy(
                            brush = Brush.linearGradient(listOf(lerp(glow, Color.White, 0.4f), glow)),
                        ),
                        textAlign = if (round) TextAlign.Center else TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            content()
        }
    }
}

/** A soft light falling from the top of the face; colour changes cross-fade. */
@Composable
fun TopGlow(color: Color) {
    val glowColor by animateColorAsState(color, tween(600), label = "glow")
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.30f), glowColor.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width / 2, -size.height * 0.08f),
                    radius = size.width * 0.85f,
                ),
            )
        },
    )
}
