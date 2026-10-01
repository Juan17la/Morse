package com.morse.wearos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Small watches (e.g. 1.2" / 192dp) need tighter type and controls than 1.4"+ ones. */
@Composable
fun isCompactScreen(): Boolean = LocalConfiguration.current.screenWidthDp < 200

/** A share of the screen width, so margins and controls scale with the watch instead of being fixed dp. */
@Composable
fun screenPct(fraction: Float): Dp = (LocalConfiguration.current.screenWidthDp * fraction).dp
