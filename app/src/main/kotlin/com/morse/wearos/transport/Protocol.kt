package com.morse.wearos.transport

import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
sealed class Envelope {
    @Serializable
    @SerialName("hello")
    data class Hello(val code: String, val member: Member) : Envelope()

    @Serializable
    @SerialName("welcome")
    data class Welcome(val code: String, val members: List<Member>) : Envelope()

    @Serializable
    @SerialName("rejected")
    data class Rejected(val reason: Reason) : Envelope() {
        @Serializable
        enum class Reason { WRONG_CODE, GROUP_FULL }
    }

    @Serializable
    @SerialName("members")
    data class MembersUpdate(val members: List<Member>) : Envelope()

    @Serializable
    @SerialName("message")
    data class Message(val message: MorseMessage) : Envelope()

    @Serializable
    @SerialName("bye")
    data class Bye(val memberId: String) : Envelope()
}

/** Newline-delimited JSON: one [Envelope] per line. */
object Protocol {
    const val PORT = 8765

    private val json = Json { ignoreUnknownKeys = true; classDiscriminator = "type" }

    fun encode(envelope: Envelope): String = json.encodeToString(envelope)

    fun decode(line: String): Envelope = json.decodeFromString(line)
}
