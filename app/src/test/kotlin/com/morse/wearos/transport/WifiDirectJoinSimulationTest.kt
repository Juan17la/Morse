package com.morse.wearos.transport

import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.TransportError
import com.morse.wearos.transport.socket.SocketGroupTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

/**
 * Two or three watches forming a group through a simulated Wi-Fi Direct radio. Projected times are
 * the scaled radio phases plus the measured app-side handshake, so they show how long a person
 * would wait if the assumed radio figures in [RadioProfile] hold.
 */
class WifiDirectJoinSimulationTest {

    private class Watch(val name: String, air: SimulatedAir) {
        val radio = air.radio()
        val transport = SocketGroupTransport(radio)
        val member = Member(name.lowercase(), name)
    }

    private val watches = mutableListOf<Watch>()
    private fun watch(name: String, air: SimulatedAir) = Watch(name, air).also { watches += it }

    @After
    fun tearDown() = runBlocking { watches.forEach { it.transport.leave() } }

    private data class JoinTiming(val projectedMs: Double, val appMs: Double)

    private suspend fun timedJoin(w: Watch, code: String): JoinTiming {
        val start = System.nanoTime()
        w.transport.joinGroup(code, w.member)
        val wall = (System.nanoTime() - start) / 1e6
        val app = wall - w.radio.resolveWallMs
        return JoinTiming(projectedMs = w.radio.resolveVirtualMs + app, appMs = app)
    }

    @Test
    fun twoWatchesFormAGroup() = runBlocking {
        val projected = mutableListOf<Double>()
        val app = mutableListOf<Double>()
        repeat(RUNS) { run ->
            val air = SimulatedAir(seed = run.toLong())
            val host = watch("Host", air)
            val joiner = watch("Joiner", air)
            val code = host.transport.createGroup(host.member)
            val t = timedJoin(joiner, code)

            assertEquals(ConnectionState.Connected, joiner.transport.state.value)
            withTimeout(2_000) { host.transport.members.first { it.size == 2 } }
            projected += t.projectedMs
            app += t.appMs
            host.transport.leave(); joiner.transport.leave()
        }
        SimResults.record(
            LOG,
            "2 watches, %d runs: join p50=%.1fs p95=%.1fs max=%.1fs | app-side handshake p50=%.0fms p95=%.0fms"
                .format(RUNS, SimResults.percentile(projected, .5) / 1000, SimResults.percentile(projected, .95) / 1000,
                    projected.max() / 1000, SimResults.percentile(app, .5), SimResults.percentile(app, .95)),
        )
        assertTrue("app-side handshake should be negligible next to the radio", SimResults.percentile(app, .95) < 500)
        assertTrue("join should land within the finder's own 20 s + 30 s budget", projected.max() < 50_000)
    }

    @Test
    fun aMistypedCodeOnlyFailsAfterTheWholeDiscoveryWindow() = runBlocking {
        val air = SimulatedAir()
        val host = watch("Host", air)
        val joiner = watch("Joiner", air)
        val code = host.transport.createGroup(host.member)
        val wrong = if (code == "0000") "0001" else "0000"

        val start = System.nanoTime()
        try {
            joiner.transport.joinGroup(wrong, joiner.member)
            fail("no group advertises $wrong")
        } catch (e: TransportException) {
            assertEquals(TransportError.HOST_NOT_FOUND, e.error)
        }
        val virtualSeconds = (System.nanoTime() - start) / 1e6 * air.timeScale / 1000
        SimResults.record(LOG, "wrong code: HOST_NOT_FOUND only after %.1fs of 'Connecting…' (LAN mode answers WRONG_CODE instantly)".format(virtualSeconds))
        assertTrue(virtualSeconds > 18)
    }

    @Test
    fun threeWatchesJoiningTogetherQueueOnTheGroupOwner() = runBlocking {
        val air = SimulatedAir(seed = 7)
        val host = watch("Host", air)
        val a = watch("Ana", air)
        val b = watch("Luis", air)
        val code = host.transport.createGroup(host.member)

        val timings = coroutineScope { listOf(a, b).map { w -> async { timedJoin(w, code) } }.awaitAll() }

        withTimeout(2_000) { host.transport.members.first { it.size == 3 } }
        listOf(a, b).forEach { assertEquals(3, withTimeout(2_000) { it.transport.members.first { m -> m.size == 3 } }.size) }
        val first = timings.minOf { it.projectedMs } / 1000
        val last = timings.maxOf { it.projectedMs } / 1000
        SimResults.record(LOG, "3 watches joining at once: first connected %.1fs, last %.1fs (invitations are serialized by the group owner)".format(first, last))
        assertTrue("the second joiner waits behind the first", last > first)
    }

    @Test
    fun aFourthWatchIsToldTheGroupIsFullAfterPayingTheRadioCost() = runBlocking {
        val air = SimulatedAir(seed = 3)
        val host = watch("Host", air)
        val a = watch("Ana", air)
        val b = watch("Luis", air)
        val extra = watch("Extra", air)
        val code = host.transport.createGroup(host.member)
        a.transport.joinGroup(code, a.member)
        b.transport.joinGroup(code, b.member)

        try {
            timedJoin(extra, code)
            fail("group should be full")
        } catch (e: TransportException) {
            assertEquals(TransportError.GROUP_FULL, e.error)
        }
        SimResults.record(LOG, "4th watch: GROUP_FULL arrives only after discovery+connect (%.1fs virtual); the group is not advertised as full".format(extra.radio.resolveVirtualMs / 1000))
    }

    companion object {
        private const val LOG = "join-simulation"
        private const val RUNS = 15

        @JvmStatic @BeforeClass
        fun resetLog() = SimResults.reset(LOG)
    }
}
