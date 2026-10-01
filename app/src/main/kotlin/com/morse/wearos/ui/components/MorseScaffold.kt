package com.morse.wearos.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition

/**
 * Standard list screen. Margins follow the Wear responsive guidance (a percentage of the
 * screen, wider on round faces where the corners are cut), so every size gets the same layout.
 */
@Composable
fun MorseScaffold(
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
        ScalingLazyColumn(
            state = listState,
            autoCentering = null,
            contentPadding = PaddingValues(
                start = screenPct(if (round) 0.052f else 0.02f),
                end = screenPct(if (round) 0.052f else 0.02f),
                top = screenPct(0.1f),
                bottom = screenPct(0.2f),
            ),
            content = content,
        )
    }
}
