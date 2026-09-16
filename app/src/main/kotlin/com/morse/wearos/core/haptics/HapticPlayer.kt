package com.morse.wearos.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.morse.wearos.core.morse.MorseTiming

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

    fun tick() {
        vibrator.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun cancel() = vibrator.cancel()

    private companion object {
        const val TAG = "HapticPlayer"
    }
}
