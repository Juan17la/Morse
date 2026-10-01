package com.morse.wearos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

/** watchOS-like palette: true black, grey platters, one vivid accent. */
object MorseColors {
    val Accent = Color(0xFFFF9F0A)
    val Platter = Color(0xFF1C1C1E)
    val PlatterPressed = Color(0xFF2C2C2E)
    val Secondary = Color(0xFF8E8E93)
    val Blue = Color(0xFF0A84FF)
    val Green = Color(0xFF30D158)
    val Purple = Color(0xFFBF5AF2)
    val Grey = Color(0xFF636366)
    val Red = Color(0xFFFF453A)
}

private val colors = Colors(
    primary = MorseColors.Accent,
    primaryVariant = MorseColors.Accent,
    secondary = MorseColors.Blue,
    secondaryVariant = MorseColors.Blue,
    background = Color.Black,
    surface = MorseColors.Platter,
    error = MorseColors.Red,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = MorseColors.Secondary,
    onError = Color.Black,
)

private val typography = Typography().run {
    copy(
        display1 = display1.copy(fontWeight = FontWeight.Bold),
        display2 = display2.copy(fontWeight = FontWeight.Bold),
        display3 = display3.copy(fontWeight = FontWeight.Bold),
        title1 = title1.copy(fontWeight = FontWeight.Bold),
        title2 = title2.copy(fontWeight = FontWeight.SemiBold),
        title3 = title3.copy(fontWeight = FontWeight.SemiBold),
        button = button.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun MorseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = colors, typography = typography, content = content)
}
