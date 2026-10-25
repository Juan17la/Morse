package com.morse.wearos.transport

import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.domain.Member
import com.morse.wearos.domain.MorseMessage
import com.morse.wearos.transport.socket.SocketGroupTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Three connected watches exchanging messages. The transport is the app's real host/client code
 * over loopback, so the numbers are the app's own cost per hop; [HOP_RF_MS] is the assumed extra
 * time a Wi-Fi Direct hop adds on real hardware.
 */
class WifiDirectMessagingSimulationTest {

    private class Watch(val name: String, air: SimulatedAir) {
        val transport = SocketGroupTransport(air.radio())
        val member = Member(name.lowercase(), name)
        val received = CopyOnWriteArrayList<MorseMessage>()
        val arrivedAt = ConcurrentHashMap<String, Long>()
    }

    private val air = SimulatedAir(RadioProfile(groupCreateMs = 1L..2L, discoveryMs = 1L..2L, connectMs = 1L..2L))
    private val host = Watch("Host", air)
    private val ana = Watch("Ana", air)
    private val luis = Watch("Luis", air)
    private val all = listOf(host, ana, luis)

    private val collectors = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @After
    fun tearDown() = runBlocking {
        collectors.cancel()
        all.forEach { it.transport.leave() }
    }

    private suspend fun formGroup() {
        val code = host.transport.createGroup(host.member)
        ana.transport.joinGroup(code, ana.member)
        luis.transport.joinGroup(code, luis.member)
        withTimeout(2_000) { host.transport.members.first { it.size == 3 } }
        all.forEach { w -> collectors.launch { w.transport.incoming.collect { w.arrivedAt[it.id] = System.nanoTime(); w.received += it } } }
        delay(200) // collectors subscribe; incoming has no replay
    }

    private fun message(from: Watch, n: Int, word: String = "maybe") = MorseMessage(
        id = "${from.name}-$n", senderId = from.member.id, senderName = from.name,
        word = word, morse = MorseCodec.encode(word), timestampMs = 0,
    )

    @Test
    fun messageLatencyAcrossHops() = runBlocking {
        formGroup()
        val sentAt = ConcurrentHashMap<String, Long>()
        val latencies = mutableMapOf("host->member (1 hop)" to mutableListOf<Double>(), "member->host (1 hop)" to mutableListOf(), "member->member (2 hops)" to mutableListOf())
        val senders = listOf(host, ana, luis)
        repeat(COUNT) { i ->
            val from = senders[i % 3]
            val m = message(from, i)
            sentAt[m.id] = System.nanoTime()
            from.transport.send(m)
            delay(5)
        }
        withTimeout(5_000) { while (all.sumOf { it.received.count { m -> m.senderId != it.member.id } } < COUNT * 2) delay(10) }
        for (w in all) for (m in w.received) {
            if (m.senderId == w.member.id) continue
            val ms = (w.arrivedAt.getValue(m.id) - sentAt.getValue(m.id)) / 1e6
            val sender = senders.first { it.member.id == m.senderId }
            val key = when {
                sender === host -> "host->member (1 hop)"
                w === host -> "member->host (1 hop)"
                else -> "member->member (2 hops)"
            }
            latencies.getValue(key) += ms
        }
        val vibrate = MorseTiming.steps(MorseCodec.encode("maybe")).sumOf { it.durationMs }
        latencies.forEach { (path, values) ->
            val hops = if (path.contains("2")) 2 else 1
            SimResults.record(
                LOG,
                "%s: app p50=%.2fms p95=%.2fms p99=%.2fms | with %dx%.0fms assumed RF ≈ %.0fms | 'maybe' then takes %dms to vibrate"
                    .format(path, SimResults.percentile(values, .5), SimResults.percentile(values, .95), SimResults.percentile(values, .99),
                        hops, HOP_RF_MS, SimResults.percentile(values, .95) + hops * HOP_RF_MS, vibrate),
            )
            assertTrue("$path p95 should stay far below one dot (120 ms)", SimResults.percentile(values, .95) < 120)
        }
    }

    @Test
    fun simultaneousSendersLoseNothingAndKeepPerSenderOrder() = runBlocking {
        formGroup()
        coroutineScope {
            listOf(host, ana, luis).map { from ->
                async(Dispatchers.IO) { repeat(BURST) { n -> from.transport.send(message(from, n, "yes")) } }
            }.awaitAll()
        }
        withTimeout(10_000) { while (all.any { w -> w.received.count { it.senderId != w.member.id } < BURST * 2 }) delay(10) }
        for (w in all) {
            for (other in all.filter { it !== w }) {
                val ids = w.received.filter { it.senderId == other.member.id }.map { it.id.substringAfterLast('-').toInt() }
                assertEquals("${w.name} missed messages from ${other.name}", (0 until BURST).toList(), ids)
            }
        }
        SimResults.record(LOG, "burst: 3 senders x $BURST messages back-to-back, no loss, per-sender order preserved on every watch")
    }

    companion object {
        private const val LOG = "messaging-simulation"
        private const val COUNT = 300
        private const val BURST = 200
        private const val HOP_RF_MS = 15.0 // assumed Wi-Fi Direct per-hop latency

        @JvmStatic @BeforeClass
        fun resetLog() = SimResults.reset(LOG)
    }
}
