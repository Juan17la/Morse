package com.morse.wearos.core.morse

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseTimingTest {

    private val unit = 100L

    @Test
    fun singleDotIsOneUnit() {
        assertArrayEquals(longArrayOf(0, 100), MorseTiming.toVibrationPattern(".", unit))
    }

    @Test
    fun dashIsThreeUnits() {
        assertArrayEquals(longArrayOf(0, 300), MorseTiming.toVibrationPattern("-", unit))
    }

    @Test
    fun symbolsWithinLetterSeparatedByOneUnit() {
        assertArrayEquals(longArrayOf(0, 100, 100, 300), MorseTiming.toVibrationPattern(".-", unit))
    }

    @Test
    fun lettersSeparatedByThreeUnits() {
        assertArrayEquals(longArrayOf(0, 100, 300, 300), MorseTiming.toVibrationPattern(". -", unit))
    }

    @Test
    fun wordsSeparatedBySevenUnits() {
        assertArrayEquals(longArrayOf(0, 100, 700, 300), MorseTiming.toVibrationPattern(". / -", unit))
    }

    @Test
    fun patternAlternatesOffOnStartingWithZero() {
        val pattern = MorseTiming.toVibrationPattern(MorseCodec.encode("yes"), unit)
        assertEquals(0L, pattern[0])
        assertTrue(pattern.size % 2 == 0)
        pattern.drop(1).forEach { assertTrue(it > 0) }
    }

    @Test
    fun stepsPointBackToSymbolIndex() {
        val steps = MorseTiming.steps(".-", unit)
        assertEquals(listOf(0, -1, 1), steps.map { it.symbolIndex })
    }

    @Test
    fun emptyMorseProducesEmptyPattern() {
        assertEquals(0, MorseTiming.toVibrationPattern("", unit).size)
        assertEquals(0L, MorseTiming.totalDurationMs("", unit))
    }
}
