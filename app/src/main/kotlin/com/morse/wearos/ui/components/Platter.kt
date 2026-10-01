package com.morse.wearos.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.ui.theme.MorseColors

private val PlatterShape = RoundedCornerShape(22.dp)

/**
 * The watchOS list cell: a rounded grey platter with a white glyph in a tinted circle. [accent]
 * fills the platter with the accent colour for the screen's main action.
 */
@Composable
fun Platter(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    @DrawableRes icon: Int? = null,
    iconTint: Color = MorseColors.Accent,
    accent: Boolean = false,
    enabled: Boolean = true,
    monospaceSubtitle: Boolean = false,
) {
    Chip(
        modifier = Modifier.fillMaxWidth(),
        shape = PlatterShape,
        label = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = subtitle?.let {
            {
                Text(
                    it,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = if (monospaceSubtitle) FontFamily.Monospace else null,
                )
            }
        },
        icon = icon?.let { { PlatterIcon(it, if (accent) Color.Black else iconTint) } },
        colors = if (accent) {
            ChipDefaults.chipColors(
                backgroundColor = MorseColors.Accent,
                contentColor = Color.Black,
                secondaryContentColor = Color.Black.copy(alpha = 0.7f),
            )
        } else {
            ChipDefaults.chipColors(
                backgroundColor = MorseColors.Platter,
                contentColor = Color.White,
                secondaryContentColor = MaterialTheme.colors.onSurfaceVariant,
            )
        },
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun PlatterIcon(@DrawableRes icon: Int, tint: Color) {
    Box(
        modifier = Modifier.size(ChipDefaults.IconSize + 8.dp).clip(CircleShape).background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(ChipDefaults.IconSize - 6.dp),
            tint = if (tint == Color.Black) MorseColors.Accent else Color.White,
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
            Text("$text · ${stringResource(if (fromAi) R.string.ai_badge else R.string.ai_badge_offline)}")
        }
    }
}
