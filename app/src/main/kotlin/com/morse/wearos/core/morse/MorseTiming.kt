package com.morse.wearos.core.morse

data class MorseStep(val on: Boolean, val durationMs: Long, val symbolIndex: Int)

object MorseTiming {
    const val DEFAULT_UNIT_MS = 120L

    private const val DOT = 1
    private const val DASH = 3
    private const val SYMBOL_GAP = 1
    private const val LETTER_GAP = 3
    private const val WORD_GAP = 7

    fun steps(morse: String, unitMs: Long = DEFAULT_UNIT_MS): List<MorseStep> {
        val steps = mutableListOf<MorseStep>()
        var pendingGapUnits = 0
        var lastWasSymbol = false

        morse.forEachIndexed { index, c ->
            when (c) {
                '.', '-' -> {
                    if (lastWasSymbol) {
                        pendingGapUnits = maxOf(pendingGapUnits, SYMBOL_GAP)
                    }
                    if (steps.isNotEmpty() && pendingGapUnits > 0) {
                        steps += MorseStep(on = false, durationMs = pendingGapUnits * unitMs, symbolIndex = -1)
                    }
                    pendingGapUnits = 0
                    val units = if (c == '.') DOT else DASH
                    steps += MorseStep(on = true, durationMs = units * unitMs, symbolIndex = index)
                    lastWasSymbol = true
                }
                ' ' -> {
                    pendingGapUnits = maxOf(pendingGapUnits, LETTER_GAP)
                    lastWasSymbol = false
                }
                MorseCodec.WORD_GAP[0] -> {
                    pendingGapUnits = maxOf(pendingGapUnits, WORD_GAP)
                    lastWasSymbol = false
                }
            }
        }
        return steps
    }

    // Steps always alternate on/off starting with "on", so a leading 0 makes a valid waveform.
    fun toVibrationPattern(morse: String, unitMs: Long = DEFAULT_UNIT_MS): LongArray {
        val steps = steps(morse, unitMs)
        if (steps.isEmpty()) return LongArray(0)
        return longArrayOf(0L) + steps.map { it.durationMs }.toLongArray()
    }

    fun totalDurationMs(morse: String, unitMs: Long = DEFAULT_UNIT_MS): Long =
        steps(morse, unitMs).sumOf { it.durationMs }
}
