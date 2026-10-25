package com.morse.wearos.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography

/** watchOS palette: true black, layered grey platters, vivid system tints. */
object MorseColors {
    val Accent = Color(0xFFFF9F0A)
    val AccentDeep = Color(0xFFFF6A00)
    val Platter = Color(0xFF1C1C1E)
    val PlatterHigh = Color(0xFF2C2C2E)
    val PlatterPressed = Color(0xFF3A3A3C)
    val Secondary = Color(0xFF8E8E93)
    val Blue = Color(0xFF0A84FF)
    val Teal = Color(0xFF64D2FF)
    val Green = Color(0xFF30D158)
    val Purple = Color(0xFFBF5AF2)
    val Indigo = Color(0xFF5E5CE6)
    val Pink = Color(0xFFFF375F)
    val Grey = Color(0xFF636366)
    val Red = Color(0xFFFF453A)

    /** Avatar tints, picked per name so each member keeps a colour across screens. */
    val Avatars = listOf(Blue, Green, Purple, Pink, Teal, Indigo, Accent)
}

object MorseGradients {
    /** The sunrise used for the main action on every screen. */
    val Accent = Brush.linearGradient(listOf(Color(0xFFFFC145), MorseColors.Accent, MorseColors.AccentDeep))
    val Platter = Brush.verticalGradient(listOf(MorseColors.PlatterHigh, MorseColors.Platter))
    val Hairline = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.02f)))

    /** A glossy version of a flat tint: lighter at the top, slightly deeper at the bottom. */
    fun of(tint: Color): Brush = Brush.verticalGradient(
        listOf(lerp(tint, Color.White, 0.28f), tint, lerp(tint, Color.Black, 0.18f)),
    )
}

/** One bouncy-but-settled spring for presses, entrances and value changes, like UIKit's default. */
fun <T> morseSpring() = spring<T>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

fun avatarColor(name: String): Color = MorseColors.Avatars[Math.floorMod(name.hashCode(), MorseColors.Avatars.size)]

private val colors = Colors(
    primary = MorseColors.Accent,
    primaryVariant = MorseColors.AccentDeep,
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

// SF-like rhythm: heavy titles with tight tracking, rounded-feeling semibold labels.
private val typography = Typography().run {
    copy(
        display1 = display1.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        display2 = display2.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        display3 = display3.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        title1 = title1.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
        title2 = title2.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        title3 = title3.copy(fontWeight = FontWeight.SemiBold),
        button = button.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
        caption1 = caption1.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.sp),
    )
}

@Composable
fun MorseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = colors, typography = typography, content = content)
}

/** True when the system "Remove animations" (animator scale 0) is on; decorative motion should then be skipped. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
