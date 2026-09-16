package com.morse.wearos.transport

import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ProtocolTest {

    private val member = Member(id = "abc", name = "Ana's Watch", isHost = true)

    @Test
    fun helloRoundTrips() {
        val envelope = Envelope.Hello(code = "4821", member = member)
        assertEquals(envelope, Protocol.decode(Protocol.encode(envelope)))
    }

    @Test
    fun messageRoundTrips() {
        val message = MorseMessage("m1", "abc", "Ana", "yes", "-.-- . ...", 1234L)
        val envelope = Envelope.Message(message)
        assertEquals(envelope, Protocol.decode(Protocol.encode(envelope)))
    }

    @Test
    fun rejectedReasonRoundTrips() {
        val envelope = Envelope.Rejected(Envelope.Rejected.Reason.GROUP_FULL)
        assertEquals(envelope, Protocol.decode(Protocol.encode(envelope)))
    }

    @Test
    fun encodedLineHasNoNewline() {
        val line = Protocol.encode(Envelope.Welcome("0001", listOf(member)))
        assertFalse(line.contains('\n'))
    }
}
