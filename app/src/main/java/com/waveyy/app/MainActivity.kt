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
import androidx.lifecycle.lifecycleScope
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.playback.PlaybackConnection
import com.waveyy.app.ui.navigation.WaveyyNavHost
import com.waveyy.app.ui.theme.WaveyyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Single-activity host. All destinations render inside [WaveyyNavHost].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playbackConnection: PlaybackConnection
    @Inject lateinit var userPrefs: UserPrefs

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

    /**
     * Persists the current song id + position on background. On next cold
     * start, PlayerViewModel reads the pair and resumes if the same song
     * is still in the restored queue.
     *
     * Skipped when the position is under 5s so a tap-and-close doesn't
     * leave a stale near-zero checkpoint that would prevent restart.
     */
    override fun onStop() {
        super.onStop()
        val snapshot = playbackConnection.state.value
        val id = snapshot.mediaId ?: return
        val pos = snapshot.positionMs
        if (pos < RESUME_MIN_POSITION_MS) return
        lifecycleScope.launch {
            userPrefs.setLastPlayed(id, pos)
        }
    }

    companion object {
        /** Logcat tag for cold-start timing (`adb logcat -s WaveyyTiming`). */
        const val WAVEYY_TIMING = "WaveyyTiming"
    }
}

private const val RESUME_MIN_POSITION_MS = 5_000L
