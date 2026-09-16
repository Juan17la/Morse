package com.morse.wearos

import android.app.Application
import com.morse.wearos.di.AppContainer

class MorseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
