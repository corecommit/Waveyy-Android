package com.waveyy.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI-facing playback state snapshot.
 *
 * @param mediaId current Waveyy song id, or null when idle.
 * @param isPlaying whether audio is currently playing.
 * @param positionMs current position in milliseconds.
 * @param durationMs track duration in milliseconds, or 0 when unknown.
 * @param isBuffering whether the player is buffering.
 */
data class PlaybackState(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false
)

/**
 * Hilt singleton owning the [MediaController] connection to
 * [PlaybackService]. UI layers collect [state] instead of touching the
 * controller directly.
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mutableState = MutableStateFlow(PlaybackState())
    /** Playback snapshots for the UI. */
    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var tickerStarted = false

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (player.isPlaying && !tickerStarted) startTicker(player)
        }
    }

    /**
     * Connects to the service. Safe to call repeatedly; subsequent calls are
     * no-ops while a connection exists.
     */
    suspend fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        val controller = future.await()
        controller.addListener(listener)
        publish(controller)
    }

    /** Releases the controller connection. */
    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
            controllerFuture = null
        }
        scope.cancel()
    }

    /**
     * Replaces the queue with [items] and starts playing at [startIndex].
     * Connects first when necessary.
     */
    suspend fun playItems(items: List<MediaItem>, startIndex: Int = 0) {
        if (items.isEmpty()) return
        connect()
        val controller = controllerFuture?.await() ?: return
        controller.setMediaItems(items, startIndex, 0L)
        controller.prepare()
        controller.play()
    }

    /** Toggles play/pause on the current item. */
    suspend fun togglePlayPause() {
        connect()
        val controller = controllerFuture?.await() ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    /** Seeks to [positionMs] on the current item. */
    suspend fun seekTo(positionMs: Long) {
        connect()
        controllerFuture?.await()?.seekTo(positionMs)
    }

    private fun publish(player: Player) {
        mutableState.value = PlaybackState(
            mediaId = player.currentMediaItem?.mediaId,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L),
            isBuffering = player.playbackState == Player.STATE_BUFFERING
        )
    }

    private fun startTicker(player: Player) {
        tickerStarted = true
        scope.launch {
            while (isActive) {
                if (player.isPlaying) publish(player)
                delay(TICK_MS)
                if (!player.isPlaying) tickerStarted = false
            }
        }
    }

    companion object {
        /** Position refresh cadence while playing. */
        const val TICK_MS = 500L
    }
}
