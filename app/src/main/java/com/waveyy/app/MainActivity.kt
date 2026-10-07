package com.waveyy.app

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.waveyy.app.ui.navigation.WaveyyNavHost
import com.waveyy.app.ui.theme.WaveyyTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Single-activity host. All destinations render inside [WaveyyNavHost].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val startMs = SystemClock.elapsedRealtime()
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Timber.tag(WAVEYY_TIMING).d("onCreate in %dms", SystemClock.elapsedRealtime() - startMs)
        setContent {
            WaveyyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WaveyyNavHost()
                }
            }
        }
    }

    companion object {
        /** Logcat tag for cold-start timing (`adb logcat -s WaveyyTiming`). */
        const val WAVEYY_TIMING = "WaveyyTiming"
    }
}
