package com.morse.wearos.transport

import com.morse.wearos.domain.TransportError
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * How slow each Wi-Fi Direct phase is. These are ASSUMED figures for a Wear OS watch pair, taken from
 * typical Android P2P behaviour, not measurements from hardware. Replace them with numbers logged on
 * real watches to tighten the simulation.
 */
data class RadioProfile(
    val groupCreateMs: LongRange = 600L..1_500L,
    val discoveryMs: LongRange = 3_000L..9_000L,
    val connectMs: LongRange = 2_000L..6_000L,
    /** Mirrors WifiDirectPeerFinder.DISCOVERY_TIMEOUT_MS. */
    val discoveryTimeoutMs: Long = 20_000L,
)

/**
 * The air between simulated watches: which groups are advertising, and a [Radio] per watch that
 * behaves like WifiDirectPeerFinder (publish, one-shot discovery with a timeout, connect). Radio
 * delays run [timeScale] times faster than real so a suite takes seconds; the TCP layer behind it
 * is the app's real host/client code over loopback and is not scaled.
 */
class SimulatedAir(
    val profile: RadioProfile = RadioProfile(),
    seed: Long = 42,
    val timeScale: Int = 50,
) {
    private val random = Random(seed)
    private val published = ConcurrentHashMap<String, Long>()
    /** A group owner negotiates one invitation at a time. */
    private val groupOwnerBusy = Mutex()

    private fun sample(range: LongRange): Long = synchronized(random) { random.nextLong(range.first, range.last + 1) }
    private suspend fun wait(virtualMs: Long) = delay(maxOf(1L, virtualMs / timeScale))

    fun radio(): Radio = Radio()

    inner class Radio : PeerFinder {
        private var code: String? = null

        /** Wall time (ms) spent inside resolveHost, i.e. the part that must be scaled back up. */
        @Volatile var resolveWallMs = 0.0
            private set

        val resolveVirtualMs: Double get() = resolveWallMs * timeScale

        override suspend fun publish(code: String) {
            wait(sample(profile.groupCreateMs))
            this.code = code
            published[code] = System.nanoTime()
        }

        override suspend fun resolveHost(code: String): InetAddress {
            val start = System.nanoTime()
            try {
                val earliest = start + sample(profile.discoveryMs) * 1_000_000 / timeScale
                val deadline = start + profile.discoveryTimeoutMs * 1_000_000 / timeScale
                while (true) {
                    val now = System.nanoTime()
                    if (now >= earliest && published.containsKey(code)) break
                    if (now >= deadline) throw TransportException(TransportError.HOST_NOT_FOUND)
                    delay(1)
                }
                groupOwnerBusy.withLock { wait(sample(profile.connectMs)) }
                return InetAddress.getLoopbackAddress()
            } finally {
                resolveWallMs = (System.nanoTime() - start) / 1e6
            }
        }

        override fun stop() {
            code?.let { published.remove(it) }
            code = null
        }
    }
}

/** Appends findings to build/sim-results/<name>.txt so the report can quote real runs. */
object SimResults {
    private val dir = File("build/sim-results").apply { mkdirs() }

    fun record(name: String, line: String) {
        println("[$name] $line")
        File(dir, "$name.txt").appendText(line + "\n")
    }

    fun reset(name: String) {
        File(dir, "$name.txt").delete()
    }

    fun percentile(values: List<Double>, p: Double): Double {
        if (values.isEmpty()) return Double.NaN
        val sorted = values.sorted()
        return sorted[((sorted.size - 1) * p).toInt()]
    }
}
