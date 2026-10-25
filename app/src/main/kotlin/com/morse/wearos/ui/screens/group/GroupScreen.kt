package com.morse.wearos.ui.screens.group

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.morse.wearos.ui.components.Avatar
import com.morse.wearos.ui.components.TopGlow
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupConfig
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.shortName
import com.morse.wearos.ui.components.ErrorText
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.components.screenPct
import com.morse.wearos.ui.theme.MorseColors
import com.morse.wearos.ui.theme.MorseGradients
import com.morse.wearos.ui.theme.avatarColor
import com.morse.wearos.ui.theme.morseSpring
import com.morse.wearos.ui.theme.rememberReduceMotion

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupScreen(
    onSend: () -> Unit,
    onOpenLastMessage: () -> Unit,
    onLeft: () -> Unit,
    viewModel: GroupViewModel = containerViewModel { GroupViewModel(it.groupSession) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // The host needs the code to share; someone who just typed it needs Send.
    val pager = rememberPagerState(initialPage = if (viewModel.isHost) 0 else 1) { 3 }
    var confirmLeave by remember { mutableStateOf(false) }

    val memberCount = state.group?.members?.size ?: 0
    LaunchedEffect(memberCount) {
        // Once everyone is in, the code has done its job: bring the host to Send.
        if (viewModel.isHost && memberCount >= GroupConfig.MAX_MEMBERS && pager.currentPage == 0) pager.animateScrollToPage(1)
    }

    val glow = when (state.state) {
        ConnectionState.Connected -> MorseColors.Green
        is ConnectionState.Error -> MorseColors.Red
        else -> MorseColors.Accent
    }
    Scaffold(timeText = { TimeText() }, vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) }) {
      Box(Modifier.fillMaxSize()) {
        TopGlow(glow)
        // One idea per page: the code, the messages, the people. Send is one swipe up from the code.
        VerticalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = screenPct(0.1f)).padding(top = screenPct(0.12f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when (page) {
                    0 -> CodePage(state, viewModel, onLeft, onSend)
                    1 -> MessagesPage(state, onSend, onOpenLastMessage)
                    else -> PeoplePage(state) { confirmLeave = true }
                }
            }
        }
        PageDots(count = 3, current = pager.currentPage, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp))
      }
    }

    Dialog(showDialog = confirmLeave, onDismissRequest = { confirmLeave = false }) {
        Alert(
            icon = { Icon(painterResource(R.drawable.ph_sign_out), contentDescription = null, tint = MorseColors.Red) },
            title = { Text(stringResource(R.string.group_leave_title), textAlign = TextAlign.Center) },
            content = {
                Text(
                    stringResource(if (state.isHost) R.string.group_leave_host_body else R.string.group_leave_body),
                    textAlign = TextAlign.Center,
                )
            },
            negativeButton = {
                Button(onClick = { confirmLeave = false }, colors = ButtonDefaults.secondaryButtonColors()) {
                    Icon(painterResource(R.drawable.ph_x), contentDescription = stringResource(R.string.action_cancel))
                }
            },
            positiveButton = {
                Button(onClick = { confirmLeave = false; viewModel.leave(); onLeft() }, colors = ButtonDefaults.buttonColors(backgroundColor = MorseColors.Red)) {
                    Icon(painterResource(R.drawable.ph_sign_out), contentDescription = stringResource(R.string.group_leave))
                }
            },
        )
    }
}

@Composable
private fun CodePage(state: GroupUiState, viewModel: GroupViewModel, onLeft: () -> Unit, onSend: () -> Unit) {
    AccessCodeHeader(code = state.group?.code, connection = state.state)
    when (val connection = state.state) {
        is ConnectionState.Error -> {
            ErrorText(connection)
            Spacer(Modifier.height(8.dp))
            if (state.isHost) {
                PillButton(stringResource(R.string.action_retry), R.drawable.ph_arrow_counter_clockwise, MorseColors.Accent, viewModel::retryCreate)
            } else {
                PillButton(stringResource(R.string.group_start_over), R.drawable.ph_arrow_counter_clockwise, MorseColors.Accent) { viewModel.leave(); onLeft() }
            }
        }
        ConnectionState.Creating, ConnectionState.Joining -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        else -> Text(
            // The count is what the host is waiting on while this page is up.
            stringResource(R.string.group_share_hint) + " · " +
                stringResource(R.string.group_members, state.group?.members.orEmpty().size, GroupConfig.MAX_MEMBERS),
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    if (state.state == ConnectionState.Connected) {
        Spacer(Modifier.height(10.dp))
        PillButton(stringResource(R.string.group_send), R.drawable.ph_paper_plane_tilt, MorseColors.Accent, onSend)
    }
}

@Composable
private fun PeoplePage(state: GroupUiState, onLeave: () -> Unit) {
    val members = state.group?.members.orEmpty()
    // No header: three rows plus Leave already fill a round face; the rows say who is here.
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        members.forEach { MemberRow(it, isSelf = it.id == state.selfId) }
    }
    Spacer(Modifier.height(10.dp))
    PillButton(stringResource(R.string.group_leave), R.drawable.ph_sign_out, MorseColors.Red, onLeave)
}

@Composable
private fun MessagesPage(state: GroupUiState, onSend: () -> Unit, onOpenLastMessage: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Platter(
            title = stringResource(R.string.group_send),
            icon = R.drawable.ph_paper_plane_tilt,
            accent = true,
            enabled = state.state == ConnectionState.Connected,
            onClick = onSend,
        )
        state.lastMessage?.let { last ->
            Platter(
                title = "${shortName(last.senderName)}: ${last.word}",
                morse = last.morse,
                icon = R.drawable.ph_vibrate,
                iconTint = MorseColors.Blue,
                onClick = onOpenLastMessage,
            )
        }
    }
}

@Composable
private fun PillButton(text: String, @DrawableRes icon: Int, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(MorseGradients.Platter).border(0.6.dp, MorseGradients.Hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick).heightIn(min = 40.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.button, color = Color.White, maxLines = 1)
    }
}

@Composable
private fun PageDots(count: Int, current: Int, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        repeat(count) { i ->
            Box(Modifier.size(if (i == current) 8.dp else 5.dp).clip(CircleShape).background(if (i == current) Color.White else MorseColors.Grey))
        }
    }
}

@Composable
private fun AccessCodeHeader(code: String?, connection: ConnectionState) {
    val connected = connection == ConnectionState.Connected
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatusDot(connection)
            Text(
                text = stringResource(R.string.group_access_code).uppercase(),
                style = MaterialTheme.typography.caption2,
                letterSpacing = 1.sp,
                color = MaterialTheme.colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        val digits = (code ?: "").padEnd(GroupConfig.CODE_LENGTH, '-')
        val tileWidth = screenPct(if (isCompactScreen()) 0.13f else 0.14f)
        Row(horizontalArrangement = Arrangement.spacedBy(screenPct(0.02f))) {
            digits.forEach { digit -> CodeTile(digit, connected, tileWidth) }
        }
    }
}

/** One digit of the access code on a glass tile; new digits roll up into place. */
@Composable
private fun CodeTile(digit: Char, lit: Boolean, width: Dp) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .size(width = width, height = width * 1.3f)
            .clip(shape)
            .background(MorseGradients.Platter)
            .border(0.6.dp, MorseGradients.Hairline, shape),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                (slideInVertically(morseSpring()) { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
            },
            label = "digit",
        ) { d ->
            Text(
                text = d.toString(),
                style = (if (isCompactScreen()) MaterialTheme.typography.title1 else MaterialTheme.typography.display3).copy(
                    brush = if (lit) MorseGradients.Accent else null,
                ),
                color = if (lit) Color.Unspecified else MaterialTheme.colors.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
            )
        }
    }
}

/** Green when connected, red on error, a breathing orange while creating or joining. */
@Composable
private fun StatusDot(connection: ConnectionState) {
    val color by animateColorAsState(
        when (connection) {
            ConnectionState.Connected -> MorseColors.Green
            is ConnectionState.Error -> MorseColors.Red
            else -> MorseColors.Accent
        },
        label = "status",
    )
    val breathing = !rememberReduceMotion() && (connection == ConnectionState.Creating || connection == ConnectionState.Joining)
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulse",
    )
    Box(
        Modifier
            .size(7.dp)
            .graphicsLayer { alpha = if (breathing) pulse else 1f }
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun MemberRow(member: Member, isSelf: Boolean) {
    val tags = listOfNotNull(
        if (member.isHost) stringResource(R.string.group_member_host) else null,
        if (isSelf) stringResource(R.string.group_member_you) else null,
    )
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(MorseGradients.Platter).border(0.6.dp, MorseGradients.Hairline, CircleShape)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Avatar(member.name, size = 24.dp)
        Column {
            Text(shortName(member.name, max = 12), style = MaterialTheme.typography.button, color = Color.White, maxLines = 1)
            if (tags.isNotEmpty()) Text(tags.joinToString(" · "), style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.onSurfaceVariant, maxLines = 1)
        }
    }
}
