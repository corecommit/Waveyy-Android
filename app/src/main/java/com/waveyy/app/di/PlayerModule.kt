package com.waveyy.app.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import com.waveyy.app.playback.QueueManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

/**
 * Player graph, scoped to the service lifetime. Provides the shared
 * [ExoPlayer] consumed by `PlaybackService`.
 */
@Module
@InstallIn(ServiceComponent::class)
object PlayerModule {

    /** Service-scoped player with Waveyy audio attributes. */
    @Provides
    @ServiceScoped
    fun providePlayer(
        @ApplicationContext context: Context,
        queueManager: QueueManager
    ): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setAudioAttributes(queueManager.audioAttributes(), true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }
}
