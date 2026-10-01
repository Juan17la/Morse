package com.morse.wearos.ui.screens.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.morse.wearos.R
import com.morse.wearos.di.containerViewModel
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupConfig
import com.morse.wearos.domain.Member
import com.morse.wearos.ui.components.ErrorText
import com.morse.wearos.ui.components.MorseScaffold
import com.morse.wearos.ui.components.Platter
import com.morse.wearos.ui.components.isCompactScreen
import com.morse.wearos.ui.theme.MorseColors

@Composable
fun GroupScreen(
    onSend: () -> Unit,
    onOpenLastMessage: () -> Unit,
    onLeft: () -> Unit,
    viewModel: GroupViewModel = containerViewModel { GroupViewModel(it.groupSession) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MorseScaffold {
        item { AccessCodeHeader(code = state.group?.code, connection = state.state) }

        when (val connection = state.state) {
            is ConnectionState.Error -> {
                item { ErrorText(connection) }
                if (state.isHost) {
                    item {
                        Platter(
                            title = stringResource(R.string.action_retry),
                            icon = R.drawable.ph_arrow_counter_clockwise,
                            onClick = viewModel::retryCreate,
                        )
                    }
                }
            }
            ConnectionState.Creating, ConnectionState.Joining -> {
                item { CircularProgressIndicator(modifier = Modifier.padding(8.dp)) }
            }
            else -> Unit
        }

        if (state.state == ConnectionState.Connected) {
            item {
                Platter(
                    title = stringResource(R.string.group_send),
                    icon = R.drawable.ph_paper_plane_tilt,
                    accent = true,
                    onClick = onSend,
                )
            }
        }

        state.lastMessage?.let { last ->
            item {
                Platter(
                    title = "${last.senderName}: ${last.word}",
                    subtitle = last.morse,
                    monospaceSubtitle = true,
                    icon = R.drawable.ph_vibrate,
                    iconTint = MorseColors.Blue,
                    onClick = onOpenLastMessage,
                )
            }
        }

        val members = state.group?.members.orEmpty()
        item {
            ListHeader { Text(stringResource(R.string.group_members, members.size, GroupConfig.MAX_MEMBERS)) }
        }
        items(members.size) { index ->
            MemberPlatter(member = members[index], isSelf = members[index].id == state.selfId)
        }

        item {
            Platter(
                title = stringResource(R.string.group_leave),
                icon = R.drawable.ph_sign_out,
                iconTint = MorseColors.Red,
                onClick = { viewModel.leave(); onLeft() },
            )
        }
    }
}

@Composable
private fun AccessCodeHeader(code: String?, connection: ConnectionState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.group_access_code),
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.onSurfaceVariant,
        )
        Text(
            text = code ?: "-".repeat(GroupConfig.CODE_LENGTH),
            style = if (isCompactScreen()) MaterialTheme.typography.display3 else MaterialTheme.typography.display2,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 4.sp,
            maxLines = 1,
            color = if (connection == ConnectionState.Connected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun MemberPlatter(member: Member, isSelf: Boolean) {
    val tags = listOfNotNull(
        if (member.isHost) stringResource(R.string.group_member_host) else null,
        if (isSelf) stringResource(R.string.group_member_you) else null,
    )
    Platter(
        title = member.name,
        subtitle = tags.takeIf { it.isNotEmpty() }?.joinToString(" · "),
        icon = if (member.isHost) R.drawable.ph_crown else R.drawable.ph_user,
        iconTint = if (member.isHost) MorseColors.Accent else MorseColors.Grey,
        onClick = {},
    )
}
