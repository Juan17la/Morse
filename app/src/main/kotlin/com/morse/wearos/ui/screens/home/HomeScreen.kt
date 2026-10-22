package com.morse.wearos.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import com.morse.wearos.R
import com.morse.wearos.data.TransportMode
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.transport.IS_DEMO
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.TopGlow
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.components.screenPct
import com.morse.wearos.ui.theme.MorseGradients
import com.morse.wearos.ui.theme.morseSpring
import com.morse.wearos.ui.permissions.rememberWifiDirectPermissionRequest
import com.morse.wearos.ui.theme.MorseColors

@Composable
fun HomeScreen(
    onOpenGroup: () -> Unit,
    onJoinGroup: () -> Unit,
    onSend: () -> Unit,
    onWords: () -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = containerViewModel { HomeViewModel(it.groupSession, it.settingsRepository) },
) {
    val transportMode by viewModel.transportMode.collectAsStateWithLifecycle()
    val activeGroupCode by viewModel.activeGroupCode.collectAsStateWithLifecycle()
    var choosing by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val requestPermission = rememberWifiDirectPermissionRequest(
        onGranted = { pendingAction?.invoke(); pendingAction = null },
        onDenied = { pendingAction = null; permissionDenied = true },
    )

    fun guarded(action: () -> Unit) {
        if (transportMode == TransportMode.WIFI_DIRECT && !IS_DEMO) {
            pendingAction = action
            requestPermission()
        } else {
            action()
        }
    }

    val inGroup = activeGroupCode != null
    BackHandler(enabled = choosing) { choosing = false }

    Scaffold(timeText = { TimeText() }) {
        TopGlow(if (inGroup) MorseColors.Green else MorseColors.Accent)
        Column(
            modifier = Modifier.fillMaxSize().padding(top = screenPct(0.13f), bottom = screenPct(0.06f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // The centre changes with the situation: send, start, or pick create / join.
            AnimatedContent(
                targetState = if (inGroup) HubState.Send else if (choosing) HubState.Choose else HubState.Start,
                modifier = Modifier.weight(1f),
                transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.85f)) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)) },
                contentAlignment = Alignment.Center,
                label = "hub",
            ) { hub ->
                when (hub) {
                    HubState.Send -> Orb(R.drawable.ph_paper_plane_tilt, stringResource(R.string.group_send), onClick = onSend)
                    HubState.Start -> Orb(R.drawable.ph_users_three, stringResource(R.string.home_start), onClick = { choosing = true })
                    HubState.Choose -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Choice(R.drawable.ph_plus, stringResource(R.string.home_create_group), MorseColors.Green) {
                            guarded { choosing = false; viewModel.createGroup(); onOpenGroup() }
                        }
                        Choice(R.drawable.ph_sign_in, stringResource(R.string.home_join_group), MorseColors.Blue) {
                            guarded { choosing = false; onJoinGroup() }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                SmallAction(R.drawable.ph_chat_text, stringResource(R.string.home_my_words), MorseColors.Purple, onWords)
                if (inGroup) SmallAction(R.drawable.ph_users_three, stringResource(R.string.home_current_group), MorseColors.Green, onOpenGroup)
                SmallAction(R.drawable.ph_gear_six, stringResource(R.string.home_settings), MorseColors.Grey, onSettings)
            }
        }
    }

    Dialog(showDialog = permissionDenied, onDismissRequest = { permissionDenied = false }) {
        Alert(
            title = { Text(stringResource(R.string.permission_wifi_title)) },
            content = { Text(stringResource(R.string.permission_wifi_rationale)) },
            positiveButton = {
                Button(onClick = { permissionDenied = false }) { Text(stringResource(R.string.action_ok)) }
            },
            negativeButton = {},
        )
    }
}

private enum class HubState { Send, Start, Choose }

/** The big centre button; two rings ripple out of it like a signal leaving the wrist. */
@Composable
private fun Orb(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    val size = if (isCompactScreen()) 76.dp else 88.dp
    val t by rememberInfiniteTransition(label = "ripple").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart), label = "ripple",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(size * 1.5f).drawBehind {
                for (phase in listOf(t, (t + 0.5f) % 1f)) {
                    val r = (size.toPx() / 2) * (1f + 0.5f * phase)
                    drawCircle(MorseColors.Accent.copy(alpha = 0.4f * (1f - phase)), r, style = Stroke(2.dp.toPx()))
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(size).clip(CircleShape).background(MorseGradients.Accent)
                    .border(0.6.dp, MorseGradients.Hairline, CircleShape)
                    .clickable(role = Role.Button, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), contentDescription = label, tint = Color.Black, modifier = Modifier.size(size * 0.42f))
            }
        }
        Text(label, style = MaterialTheme.typography.button, color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun Choice(@DrawableRes icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    val size = if (isCompactScreen()) 58.dp else 66.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(size).clip(CircleShape).background(MorseGradients.of(tint))
                .border(0.6.dp, MorseGradients.Hairline, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = label, tint = Color.White, modifier = Modifier.size(size * 0.45f))
        }
        Text(label, style = MaterialTheme.typography.caption2, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun SmallAction(@DrawableRes icon: Int, description: String, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(MorseGradients.Platter)
            .border(0.6.dp, MorseGradients.Hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}
