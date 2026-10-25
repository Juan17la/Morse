package com.morse.wearos.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.ui.theme.MorseColors
import com.morse.wearos.ui.theme.MorseGradients
import com.morse.wearos.ui.theme.avatarColor
import com.morse.wearos.ui.theme.morseSpring
import com.morse.wearos.ui.theme.rememberReduceMotion

private val PlatterShape = RoundedCornerShape(24.dp)
private val IconSize = 30.dp

/**
 * The watchOS list cell: a softly lit platter with a glossy tinted glyph. [accent] paints it with
 * the sunrise gradient for the screen's main action. Pressing shrinks it on a spring instead of
 * a ripple,.
 * [avatar] replaces the glyph with the name's initial, for people.
 */
@Composable
fun Platter(
    title: String,
    /** Null makes a read-only row: no press feedback and no button role. */
    onClick: (() -> Unit)?,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    iconTint: Color = MorseColors.Accent,
    accent: Boolean = false,
    enabled: Boolean = true,
    monospaceSubtitle: Boolean = false,
    avatar: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduceMotion = rememberReduceMotion()
    val pressScale by animateFloatAsState(if (pressed && !reduceMotion) 0.96f else 1f, morseSpring(), label = "press")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = if (enabled) 1f else 0.38f
            }
            .clip(PlatterShape)
            .background(if (accent) MorseGradients.Accent else MorseGradients.Platter)
            .border(0.6.dp, MorseGradients.Hairline, PlatterShape)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            avatar != null -> Avatar(avatar, iconTint)
            icon != null -> PlatterIcon(icon, accent, iconTint)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.button,
                color = if (accent) Color.Black else Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.caption2,
                    color = if (accent) Color.Black.copy(alpha = 0.62f) else MaterialTheme.colors.onSurfaceVariant,
                    fontFamily = if (monospaceSubtitle) FontFamily.Monospace else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlatterIcon(@DrawableRes icon: Int, accent: Boolean, tint: Color) {
    Box(
        modifier = Modifier
            .size(IconSize)
            .clip(CircleShape)
            .background(if (accent) Brush.verticalGradient(listOf(Color(0xE6000000), Color(0xCC000000))) else MorseGradients.of(tint)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = if (accent) MorseColors.Accent else Color.White,
        )
    }
}

/** A contact-style circle with the name's initial, tinted per name unless [tint] says otherwise. */
@Composable
fun Avatar(name: String, tint: Color = avatarColor(name), size: androidx.compose.ui.unit.Dp = IconSize) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(MorseGradients.of(tint)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifEmpty { "?" },
            style = MaterialTheme.typography.button,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

/** Section header for AI output, saying whether the model or the offline fallback produced it. */
@Composable
fun AiHeader(text: String, fromAi: Boolean) {
    ListHeader {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ph_sparkle),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (fromAi) MorseColors.Purple else MaterialTheme.colors.onSurfaceVariant,
            )
            Text(
                text = "$text · ${stringResource(if (fromAi) R.string.ai_badge else R.string.ai_badge_offline)}",
                style = MaterialTheme.typography.caption1.copy(
                    brush = if (fromAi) Brush.linearGradient(listOf(MorseColors.Purple, MorseColors.Blue)) else null,
                ),
                color = if (fromAi) Color.Unspecified else MaterialTheme.colors.onSurfaceVariant,
            )
        }
    }
}

/** Grey caption above a group of platters, like an inset-grouped list header. */
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 2.dp),
        style = MaterialTheme.typography.caption1,
        color = MaterialTheme.colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
