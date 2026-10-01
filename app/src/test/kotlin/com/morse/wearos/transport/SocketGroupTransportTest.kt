package com.morse.wearos.transport

import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.GroupConfig
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.domain.TransportError
import com.morse.wearos.transport.socket.SocketGroupTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.InetAddress

class SocketGroupTransportTest {

    private class Loopback : PeerFinder {
        override suspend fun publish(code: String) = Unit
        override suspend fun resolveHost(code: String): InetAddress = InetAddress.getLoopbackAddress()
        override fun stop() = Unit
    }

    private val host = SocketGroupTransport(Loopback())
    private val joiners = List(GroupConfig.MAX_MEMBERS) { SocketGroupTransport(Loopback()) }

    private val hostMember = Member("host", "Host Watch")
    private fun joiner(i: Int) = Member("joiner$i", "Joiner $i")

    @After
    fun tearDown() = runBlocking {
        joiners.forEach { it.leave() }
        host.leave()
    }

    @Test
    fun joinWithCorrectCodeSharesMemberList() = runBlocking {
        val code = host.createGroup(hostMember)
        joiners[0].joinGroup(code, joiner(0))

        assertEquals(ConnectionState.Connected, joiners[0].state.value)
        val names = { t: GroupTransport -> t.members.value.map { it.id }.toSet() }
        withTimeout(2_000) { host.members.first { it.size == 2 } }
        assertEquals(setOf("host", "joiner0"), names(host))
        assertEquals(setOf("host", "joiner0"), names(joiners[0]))
        assertTrue(joiners[0].members.value.first { it.id == "host" }.isHost)
    }

    @Test
    fun hostCleansJoinerNames() = runBlocking {
        val code = host.createGroup(hostMember)
        joiners[0].joinGroup(code, Member("joiner0", "  " + "x".repeat(100) + "  "))

        val name = withTimeout(2_000) { host.members.first { it.size == 2 } }.first { it.id == "joiner0" }.name
        assertEquals("x".repeat(GroupConfig.MAX_NAME_LENGTH), name)
    }

    @Test
    fun wrongCodeIsRejected() = runBlocking {
        val code = host.createGroup(hostMember)
        val wrong = if (code == "0000") "0001" else "0000"
        try {
            joiners[0].joinGroup(wrong, joiner(0))
            fail("expected rejection")
        } catch (e: TransportException) {
            assertEquals(TransportError.WRONG_CODE, e.error)
        }
    }

    @Test
    fun groupLimitIsEnforced() = runBlocking {
        val code = host.createGroup(hostMember)
        for (i in 0 until GroupConfig.MAX_MEMBERS - 1) {
            joiners[i].joinGroup(code, joiner(i))
        }
        try {
            joiners[GroupConfig.MAX_MEMBERS - 1].joinGroup(code, joiner(GroupConfig.MAX_MEMBERS - 1))
            fail("expected group full")
        } catch (e: TransportException) {
            assertEquals(TransportError.GROUP_FULL, e.error)
        }
    }

    @Test
    fun messagesAreRelayedToEveryMember() = runBlocking {
        val code = host.createGroup(hostMember)
        joiners[0].joinGroup(code, joiner(0))
        joiners[1].joinGroup(code, joiner(1))

        val message = MorseMessage("m1", "joiner0", "Joiner 0", "yes", "-.-- . ...", 1L)
        val hostGot = async(Dispatchers.IO) { withTimeout(2_000) { host.incoming.first() } }
        val j1Got = async(Dispatchers.IO) { withTimeout(2_000) { joiners[1].incoming.first() } }
        val selfGot = async(Dispatchers.IO) { withTimeout(2_000) { joiners[0].incoming.first() } }
        delay(200) // let the collectors subscribe; incoming has no replay

        joiners[0].send(message)

        assertEquals(message, hostGot.await())
        assertEquals(message, j1Got.await())
        assertEquals(message, selfGot.await())
    }

    @Test
    fun leavingUpdatesRemainingMembers() = runBlocking {
        val code = host.createGroup(hostMember)
        joiners[0].joinGroup(code, joiner(0))
        joiners[1].joinGroup(code, joiner(1))
        withTimeout(2_000) { host.members.first { it.size == 3 } }

        joiners[0].leave()

        withTimeout(2_000) { host.members.first { it.size == 2 } }
        withTimeout(2_000) { joiners[1].members.first { it.size == 2 } }
        assertEquals(setOf("host", "joiner1"), joiners[1].members.value.map { it.id }.toSet())
    }
}
