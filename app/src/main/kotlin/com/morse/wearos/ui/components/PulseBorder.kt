package com.morse.wearos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.morse.wearos.ui.theme.MorseColors

/** A green ring around the screen edge that lights while the wrist buzzes; passes touches through. */
@Composable
fun PulseBorder(on: Boolean) {
    val alpha by animateFloatAsState(if (on) 1f else 0f, tween(if (on) 40 else 90), label = "pulseBorder")
    val round = LocalConfiguration.current.isScreenRound
    if (alpha <= 0.01f) return
    Box(
        Modifier.fillMaxSize().drawBehind {
            // Wide faint strokes under a narrow bright one read as a glow.
            for ((width, a) in listOf(16.dp to 0.12f, 10.dp to 0.22f, 5.dp to 0.9f)) {
                val w = width.toPx()
                val stroke = Stroke(w)
                val color = MorseColors.Green.copy(alpha = a * alpha)
                if (round) {
                    drawCircle(color, radius = size.minDimension / 2 - w / 2, style = stroke)
                } else {
                    drawRoundRect(color, Offset(w / 2, w / 2), Size(size.width - w, size.height - w), CornerRadius(24.dp.toPx()), stroke)
                }
            }
        },
    )
}
