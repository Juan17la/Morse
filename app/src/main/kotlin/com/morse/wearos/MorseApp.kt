package com.morse.wearos

import android.app.Application
import android.content.Context
import com.morse.wearos.data.AppLanguage
import com.morse.wearos.di.AppContainer
import com.morse.wearos.domain.ConnectionState
import com.morse.wearos.group.GroupService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MorseApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Connected only ever starts from a user tap, so the app is in the foreground when the service starts.
        MainScope().launch {
            container.groupSession.state
                .map { it == ConnectionState.Connected }
                .distinctUntilChanged()
                .collect { GroupService.setRunning(this@MorseApp, it) }
        }
    }
}
