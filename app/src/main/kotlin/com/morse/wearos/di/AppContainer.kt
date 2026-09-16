package com.morse.wearos.di

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.morse.wearos.MorseApp
import com.morse.wearos.core.haptics.HapticPlayer
import com.morse.wearos.data.WordsRepository

class AppContainer(private val appContext: Context) {
    val hapticPlayer: HapticPlayer by lazy { HapticPlayer(appContext) }
    val wordsRepository: WordsRepository by lazy { WordsRepository(appContext) }
}

@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as MorseApp).container

@Composable
inline fun <reified VM : ViewModel> containerViewModel(
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = appContainer()
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
