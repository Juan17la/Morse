package com.morse.wearos.transport

import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.transport.socket.SocketGroupTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * What the group does when the link misbehaves the way a real Wi-Fi Direct link does: a watch
 * walks out of range without closing its socket, reconnects, or a stray device pokes the port.
 *
 * The `finding_*` tests pin down CURRENT behaviour that is a weakness. They pass today; when one
 * starts failing, the weakness was fixed and the test should be flipped into a regression test.
 */
class TransportResilienceTest {

    private class Loopback : PeerFinder {
        override suspend fun publish(code: String) = Unit
        override suspend fun resolveHost(code: String): InetAddress = InetAddress.getLoopbackAddress()
        override fun stop() = Unit
    }

    private val host = SocketGroupTransport(Loopback())
    private val client = SocketGroupTransport(Loopback())
    private val hostMember = Member("host", "Host")
    private val raw = mutableListOf<Socket>()
    private var fakeHost: ServerSocket? = null

    @After
    fun tearDown() = runBlocking {
        raw.forEach { runCatching { it.close() } }
        fakeHost?.close()
        client.leave()
        host.leave()
    }

    private fun rawSocket(): Socket = Socket(InetAddress.getLoopbackAddress(), Protocol.PORT).also { raw += it }

    private fun Socket.send(e: Envelope) {
        getOutputStream().write((Protocol.encode(e) + "\n").toByteArray()); getOutputStream().flush()
    }

    private fun Socket.readEnvelope(timeoutMs: Int): Envelope? {
        soTimeout = timeoutMs
        return getInputStream().bufferedReader().readLine()?.let(Protocol::decode)
    }

    @Test
    fun finding_clientNeverNoticesAHostThatWentSilent() = runBlocking {
        // A host that completes the handshake and then stops talking, like a watch that left range.
        val server = ServerSocket(Protocol.PORT).also { fakeHost = it }
        Thread {
            val s = server.accept(); raw += s
            val hello = s.readEnvelope(2_000) as Envelope.Hello
            s.send(Envelope.Welcome(hello.code, listOf(Member("h", "Fake host", true), hello.member)))
        }.start()

        client.joinGroup("1234", Member("c", "Client"))
        delay(SILENCE_MS)

        assertEquals("no heartbeat or read timeout: the client still reports Connected", ConnectionState.Connected, client.state.value)
        SimResults.record(LOG, "silent host: client still 'Connected' after ${SILENCE_MS / 1000}s of silence (no heartbeat, no SO_TIMEOUT, no TCP keepalive)")
    }

    @Test
    fun finding_hostKeepsAMemberThatWentSilent() = runBlocking {
        val code = host.createGroup(hostMember)
        val s = rawSocket()
        s.send(Envelope.Hello(code, Member("ghost", "Ghost")))
        assertTrue(s.readEnvelope(2_000) is Envelope.Welcome)
        withTimeout(2_000) { host.members.first { it.size == 2 } }

        delay(SILENCE_MS) // the ghost neither sends Bye nor closes

        assertEquals("ghost still listed: a full group can stay blocked by a dead watch", 2, host.members.value.size)
        SimResults.record(LOG, "silent member: host still lists it after ${SILENCE_MS / 1000}s, so it still holds one of the 3 seats")
    }

    @Test
    fun finding_rejoiningWithTheSameIdIsDroppedWhenTheOldSocketDies() = runBlocking {
        val code = host.createGroup(hostMember)
        val old = rawSocket()
        old.send(Envelope.Hello(code, Member("ana", "Ana")))
        assertTrue(old.readEnvelope(2_000) is Envelope.Welcome)
        withTimeout(2_000) { host.members.first { it.size == 2 } }

        // Ana's Wi-Fi blipped; she reconnects on a fresh socket while the host still holds the old one.
        val fresh = rawSocket()
        fresh.send(Envelope.Hello(code, Member("ana", "Ana")))
        assertTrue(fresh.readEnvelope(2_000) is Envelope.Welcome)

        old.close() // the host finally notices the stale socket
        delay(500)

        val ids = host.members.value.map { it.id }
        assertEquals("the live session was evicted by the dead one's cleanup", listOf("host"), ids)
        SimResults.record(LOG, "reconnect: after a stale socket closes, the host removes the rejoined member too (members=$ids) while her new socket is open")
    }

    @Test
    fun finding_idleConnectionsCanStarveNewJoins() = runBlocking {
        val code = host.createGroup(hostMember)
        // Stand-ins for anything that connects to port 8765 and says nothing; each parks a blocking read.
        repeat(IDLE_SOCKETS) { rawSocket() }
        delay(300)

        val legit = rawSocket()
        legit.send(Envelope.Hello(code, Member("late", "Late")))
        val started = System.nanoTime()
        val reply = try {
            withContext(Dispatchers.IO.limitedParallelism(1)) { legit.readEnvelope(3_000) }
        } catch (e: SocketTimeoutException) {
            null
        }
        val waited = (System.nanoTime() - started) / 1_000_000
        SimResults.record(LOG, "$IDLE_SOCKETS idle sockets: a real join got ${if (reply == null) "NO answer within ${waited}ms (handshake threads exhausted)" else "answered in ${waited}ms"}")
        assertTrue("with the IO pool exhausted by blocking reads, the handshake never runs", reply == null)
    }

    @Test
    fun finding_aReaderThatStopsReadingBlocksTheHost() = runBlocking {
        val code = host.createGroup(hostMember)
        val stalled = rawSocket()
        stalled.send(Envelope.Hello(code, Member("slow", "Slow")))
        assertTrue(stalled.readEnvelope(2_000) is Envelope.Welcome)
        withTimeout(2_000) { host.members.first { it.size == 2 } }

        // From here the peer never reads again. A blocking write cannot be cancelled by a coroutine
        // timeout, so flood from a plain thread and watch its progress instead.
        val word = "x".repeat(20)
        val sent = java.util.concurrent.atomic.AtomicInteger()
        val flood = Thread {
            runBlocking {
                while (sent.get() < MAX_FLOOD) {
                    host.send(MorseMessage("m${sent.get()}", "host", "Host", word, "-..-", 0)); sent.incrementAndGet()
                }
            }
        }.apply { isDaemon = true; start() }
        var last = -1
        var idleMs = 0
        while (flood.isAlive && idleMs < STALL_MS) {
            delay(100)
            if (sent.get() == last) idleMs += 100 else { idleMs = 0; last = sent.get() }
        }
        val stalledNow = flood.isAlive
        SimResults.record(LOG, "non-reading peer: host send ${if (stalledNow) "BLOCKED after ${sent.get()} messages (~${sent.get() * 190 / 1024} KiB) with no timeout, starving the relay to every other watch" else "never blocked in $MAX_FLOOD messages"}")
        stalled.close() // unblocks the writer so teardown can finish
        flood.join(5_000)
    }

    companion object {
        private const val LOG = "resilience"
        private const val SILENCE_MS = 3_000L
        private const val IDLE_SOCKETS = 100
        private const val STALL_MS = 1_500
        private const val MAX_FLOOD = 60_000

        @JvmStatic @BeforeClass
        fun resetLog() = SimResults.reset(LOG)
    }
}
