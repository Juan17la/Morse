package com.morse.wearos.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Small watches (e.g. 1.2" / 192dp) need tighter type and controls than 1.4"+ ones. */
@Composable
fun isCompactScreen(): Boolean = LocalConfiguration.current.screenWidthDp < 200
