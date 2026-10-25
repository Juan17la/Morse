package com.morse.wearos.ui.screens.learn

import androidx.lifecycle.ViewModel
import com.morse.wearos.core.haptics.HapticPlayer

class LearnViewModel(private val haptics: HapticPlayer) : ViewModel() {
    fun play(morse: String, unitMs: Long) = haptics.play(morse, unitMs)
}
