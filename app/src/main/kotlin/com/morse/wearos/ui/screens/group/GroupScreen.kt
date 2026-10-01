package com.morse.wearos.ui.screens.group

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
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
import com.morse.wearos.ui.components.isCompactScreen

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
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.action_retry)) },
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

        state.lastMessage?.let { last ->
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${last.senderName}: ${last.word}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    secondaryLabel = {
                        Text(last.morse, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = onOpenLastMessage,
                )
            }
        }

        if (state.state == ConnectionState.Connected) {
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.group_send), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                    colors = ChipDefaults.primaryChipColors(),
                    onClick = onSend,
                )
            }
        }

        val members = state.group?.members.orEmpty()
        item {
            ListHeader { Text(stringResource(R.string.group_members, members.size, GroupConfig.MAX_MEMBERS)) }
        }
        items(members.size) { index ->
            MemberChip(member = members[index], isSelf = members[index].id == state.selfId)
        }

        item {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.group_leave)) },
                colors = ChipDefaults.secondaryChipColors(),
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
private fun MemberChip(member: Member, isSelf: Boolean) {
    val tags = listOfNotNull(
        if (member.isHost) stringResource(R.string.group_member_host) else null,
        if (isSelf) stringResource(R.string.group_member_you) else null,
    )
    Chip(
        modifier = Modifier.fillMaxWidth(),
        label = { Text(member.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = if (tags.isEmpty()) null else ({ Text(tags.joinToString(" · "), maxLines = 1) }),
        icon = {
            Icon(
                imageVector = if (member.isHost) Icons.Filled.Star else Icons.Filled.Person,
                contentDescription = null,
            )
        },
        colors = ChipDefaults.secondaryChipColors(),
        onClick = {},
    )
}
