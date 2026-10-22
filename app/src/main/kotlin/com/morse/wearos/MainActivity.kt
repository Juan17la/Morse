package com.morse.wearos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.morse.wearos.data.AppLanguage
import com.morse.wearos.ui.navigation.MorseNavHost
import com.morse.wearos.ui.theme.MorseTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorseTheme {
                MorseNavHost()
            }
        }
    }
}
