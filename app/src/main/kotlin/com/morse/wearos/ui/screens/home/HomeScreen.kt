package com.morse.wearos.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import com.morse.wearos.domain.GroupConfig
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
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
import com.morse.wearos.ui.components.TopGlow
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.components.screenPct
import com.morse.wearos.ui.theme.MorseGradients
import com.morse.wearos.ui.permissions.rememberWifiDirectPermissionRequest
import com.morse.wearos.ui.theme.MorseColors

@Composable
fun HomeScreen(
    onOpenGroup: () -> Unit,
    onJoinGroup: () -> Unit,
    onSend: () -> Unit,
    onWords: () -> Unit,
    onSettings: () -> Unit,
    onWelcome: () -> Unit,
    viewModel: HomeViewModel = containerViewModel { HomeViewModel(it.groupSession, it.settingsRepository) },
) {
    val transportMode by viewModel.transportMode.collectAsStateWithLifecycle()
    val welcomeSeen by viewModel.welcomeSeen.collectAsStateWithLifecycle()
    val group by viewModel.activeGroup.collectAsStateWithLifecycle()
    var permissionDenied by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val context = LocalContext.current

    LaunchedEffect(welcomeSeen) { if (welcomeSeen == false) onWelcome() }

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

    val current = group
    val round = LocalConfiguration.current.isScreenRound
    Scaffold(timeText = { TimeText() }) {
        // Nothing until we know whether this is the first launch, so the welcome never flashes Home first.
        if (welcomeSeen != true) return@Scaffold
        TopGlow(if (current != null) MorseColors.Green else MorseColors.Accent)
        Column(
            // Round faces narrow sharply at the bottom; lift the shortcuts so their labels stay inside the bezel.
            modifier = Modifier.fillMaxSize().padding(top = screenPct(0.12f), bottom = screenPct(if (round) 0.08f else 0.05f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // The centre is the one thing to do now: send when in a group, otherwise create or join one.
            AnimatedContent(
                targetState = current != null,
                modifier = Modifier.weight(1f),
                transitionSpec = { (fadeIn(tween(220)) + scaleIn(initialScale = 0.85f)) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.85f)) },
                contentAlignment = Alignment.Center,
                label = "hub",
            ) { inGroup ->
                if (inGroup && current != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Orb(R.drawable.ph_paper_plane_tilt, stringResource(R.string.group_send), onClick = onSend)
                        CompactChip(
                            onClick = onOpenGroup,
                            label = {
                                Text(
                                    stringResource(R.string.home_group_chip, current.code, current.members.size, GroupConfig.MAX_MEMBERS),
                                    maxLines = 1,
                                )
                            },
                            icon = { Icon(painterResource(R.drawable.ph_users_three), contentDescription = null, tint = MorseColors.Green, modifier = Modifier.size(16.dp)) },
                            colors = ChipDefaults.secondaryChipColors(),
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Choice(R.drawable.ph_plus, stringResource(R.string.home_create_group), MorseColors.Green) {
                            guarded { viewModel.createGroup(); onOpenGroup() }
                        }
                        Choice(R.drawable.ph_sign_in, stringResource(R.string.home_join_group), MorseColors.Blue) {
                            guarded { onJoinGroup() }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                SmallAction(R.drawable.ph_chat_text, stringResource(R.string.home_my_words), MorseColors.Purple, onWords)
                SmallAction(R.drawable.ph_gear_six, stringResource(R.string.home_settings), MorseColors.Grey, onSettings)
            }
        }
    }

    Dialog(showDialog = permissionDenied, onDismissRequest = { permissionDenied = false }) {
        Alert(
            title = { Text(stringResource(R.string.permission_wifi_title), textAlign = TextAlign.Center) },
            content = { Text(stringResource(R.string.permission_wifi_rationale), textAlign = TextAlign.Center) },
            negativeButton = {
                Button(onClick = { permissionDenied = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                    Icon(painterResource(R.drawable.ph_x), contentDescription = stringResource(R.string.action_cancel))
                }
            },
            positiveButton = {
                Button(onClick = {
                    permissionDenied = false
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                    )
                }) {
                    Icon(painterResource(R.drawable.ph_gear_six), contentDescription = stringResource(R.string.permission_open_settings))
                }
            },
        )
    }
}

/** The big centre button for the one main action. */
@Composable
private fun Orb(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    val compact = isCompactScreen()
    val size = if (compact) 56.dp else 68.dp
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Box(
            Modifier
                .size(size)
                .semantics { contentDescription = label }
                .clip(CircleShape)
                .background(MorseGradients.Accent)
                .border(0.6.dp, MorseGradients.Hairline, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = Color.Black, modifier = Modifier.size(size * 0.42f))
        }
        // Small faces and large font scales need the room for the group chip; the plane and the description still say "send".
        if (!compact && LocalDensity.current.fontScale <= 1.15f) Text(label, style = MaterialTheme.typography.button, color = Color.White, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun Choice(@DrawableRes icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    val size = if (isCompactScreen()) 58.dp else 66.dp
    Column(
        Modifier.clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.size(size).clip(CircleShape).background(MorseGradients.of(tint))
                .border(0.6.dp, MorseGradients.Hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.45f))
        }
        Text(label, style = MaterialTheme.typography.caption1, color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
    }
}

@Composable
private fun SmallAction(@DrawableRes icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = 64.dp).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MorseGradients.Platter)
                .border(0.6.dp, MorseGradients.Hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.onSurfaceVariant, maxLines = 1, textAlign = TextAlign.Center)
    }
}
