package com.waveyy.app.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.waveyy.app.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground playback service hosting the ExoPlayer instance behind a
 * Media3 [MediaSession]. Media3 manages the persistent notification and the
 * foreground-service lifecycle automatically while audio plays.
 *
 * The player itself is provided by [com.waveyy.app.di.PlayerModule] so queue
 * construction stays testable and out of the service body.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        session = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return session
    }

    override fun onDestroy() {
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }
}
