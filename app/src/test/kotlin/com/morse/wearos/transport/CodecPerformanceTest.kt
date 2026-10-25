package com.morse.wearos.transport

import com.morse.wearos.core.morse.MorseCodec
import com.morse.wearos.core.morse.MorseTiming
import com.morse.wearos.domain.MorseMessage
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

/**
 * Rough per-operation cost of the hot paths on the JVM. A watch CPU is several times slower, so the
 * assertions are loose: they only catch an order-of-magnitude regression, not a precise budget.
 */
class CodecPerformanceTest {

    private val longest = "x".repeat(20)
    private val message = MorseMessage("id-1", "sender", "Sender", longest, MorseCodec.encode(longest), 1L)

    private inline fun microsPerOp(iterations: Int, block: () -> Unit): Double {
        repeat(iterations / 4) { block() } // warm up the JIT
        val start = System.nanoTime()
        repeat(iterations) { block() }
        return (System.nanoTime() - start) / 1e3 / iterations
    }

    @Test
    fun envelopeEncodeAndDecode() {
        val envelope = Envelope.Message(message)
        val line = Protocol.encode(envelope)
        val encode = microsPerOp(50_000) { Protocol.encode(envelope) }
        val decode = microsPerOp(50_000) { Protocol.decode(line) }
        SimResults.record(LOG, "envelope (20-char word, %d bytes on the wire): encode %.1fµs, decode %.1fµs per message".format(line.length + 1, encode, decode))
        assertTrue(encode < 500 && decode < 500)
    }

    @Test
    fun morseEncodingAndTiming() {
        val encode = microsPerOp(50_000) { MorseCodec.encode(longest) }
        val steps = microsPerOp(50_000) { MorseTiming.steps(message.morse) }
        val count = MorseTiming.steps(message.morse).size
        val total = MorseTiming.steps(message.morse).sumOf { it.durationMs }
        SimResults.record(LOG, "20-char word: encode %.1fµs, timing %.1fµs, %d vibration steps, %.1fs of buzzing".format(encode, steps, count, total / 1000.0))
        assertTrue(encode < 500 && steps < 500)
    }

    @Test
    fun vibrationBudgetPerWord() {
        listOf("yes", "no", "maybe", "I'm here", "wait", "x".repeat(20)).forEach { w ->
            val ms = MorseTiming.steps(MorseCodec.encode(w)).sumOf { it.durationMs }
            SimResults.record(LOG, "vibrating \"%s\" takes %.1fs".format(w.take(10) + if (w.length > 10) "…" else "", ms / 1000.0))
        }
    }

    companion object {
        private const val LOG = "codec-performance"

        @JvmStatic @BeforeClass
        fun resetLog() = SimResults.reset(LOG)
    }
}
