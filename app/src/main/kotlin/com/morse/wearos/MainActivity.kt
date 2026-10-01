package com.morse.wearos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.morse.wearos.ui.navigation.MorseNavHost
import com.morse.wearos.ui.theme.MorseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MorseTheme {
                MorseNavHost()
            }
        }
    }
}
