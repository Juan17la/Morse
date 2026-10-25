package com.morse.wearos.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.morse.wearos.core.morse.MorseTiming

/**
 * Feel-only cues for system events. Each has a rhythm that no Morse word produces
 * (varied strength, not dots and dashes), so they are never mistaken for a message.
 */
enum class HapticSignal(val timings: LongArray, val amplitudes: IntArray) {
    Sent(longArrayOf(0, 40), intArrayOf(0, 255)),
    Joined(longArrayOf(0, 50, 70, 110), intArrayOf(0, 110, 0, 255)),
    Failed(longArrayOf(0, 45, 55, 45, 55, 45), intArrayOf(0, 255, 0, 255, 0, 255)),
    MemberLeft(longArrayOf(0, 60, 80, 220), intArrayOf(0, 255, 0, 90)),
    Lost(longArrayOf(0, 320, 120, 320), intArrayOf(0, 200, 0, 200)),
}

class HapticPlayer(context: Context) {
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun play(morse: String, unitMs: Long = MorseTiming.DEFAULT_UNIT_MS) {
        val pattern = MorseTiming.toVibrationPattern(morse, unitMs)
        if (pattern.isEmpty()) return
        Log.d(TAG, "play \"$morse\" -> ${pattern.joinToString(",")}")
        vibrator.cancel()
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }

    fun signal(signal: HapticSignal) {
        vibrator.cancel()
        vibrator.vibrate(VibrationEffect.createWaveform(signal.timings, signal.amplitudes, -1))
    }

    fun cancel() = vibrator.cancel()

    private companion object {
        const val TAG = "HapticPlayer"
    }
}
