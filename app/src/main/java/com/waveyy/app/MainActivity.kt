package com.waveyy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.waveyy.app.ui.navigation.WaveyyNavHost
import com.waveyy.app.ui.theme.WaveyyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. All destinations render inside [WaveyyNavHost].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaveyyTheme {
                WaveyyNavHost()
            }
        }
    }
}
