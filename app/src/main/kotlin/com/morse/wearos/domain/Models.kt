package com.morse.wearos.domain

import kotlinx.serialization.Serializable

object GroupConfig {
    const val MAX_MEMBERS = 3
    const val CODE_LENGTH = 4
    const val MAX_NAME_LENGTH = 20
}

/** Collapses whitespace and caps the length; names also arrive from other watches, so never trust them. */
fun cleanMemberName(raw: String): String =
    raw.trim().replace(Regex("\\s+"), " ").take(GroupConfig.MAX_NAME_LENGTH).trim().ifEmpty { "?" }

@Serializable
data class Member(
    val id: String,
    val name: String,
    val isHost: Boolean = false,
)

data class GroupInfo(
    val code: String,
    val members: List<Member>,
)

@Serializable
data class MorseMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val word: String,
    val morse: String,
    val timestampMs: Long,
)

enum class TransportError {
    WRONG_CODE,
    GROUP_FULL,
    HOST_NOT_FOUND,
    HOST_UNREACHABLE,
    CONNECTION_LOST,
    NOT_SUPPORTED,
    PERMISSION_DENIED,
    UNKNOWN,
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Creating : ConnectionState
    data object Joining : ConnectionState
    data object Connected : ConnectionState
    data class Error(val error: TransportError) : ConnectionState
}
