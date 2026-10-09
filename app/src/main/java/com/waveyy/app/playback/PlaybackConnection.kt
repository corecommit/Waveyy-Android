package com.waveyy.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
import timber.log.Timber
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
 * @param repeatMode Media3 repeat mode (OFF/ONE/ALL).
 * @param shuffleEnabled whether shuffle is on.
 * @param error last player error message, or null. Surfaced once via
 * Snackbar; preserved across publishes until cleared.
 * @param mediaIndex controller queue position (drives displayed metadata).
 */
data class PlaybackState(
    val mediaId: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val error: String? = null,
    val mediaIndex: Int = 0
)

/**
 * Hilt singleton owning the [MediaController] connection to
 * [PlaybackService]. UI layers collect [state] instead of touching the
 * controller directly.
 */
@Singleton
class PlaybackConnection @Inject constructor(
    @ApplicationContext private val context: Context,
    private val queueManager: QueueManager
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mutableState = MutableStateFlow(PlaybackState())
    /** Playback snapshots for the UI. */
    val state: StateFlow<PlaybackState> = mutableState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var tickerStarted = false

    init {
        // Reconnect at startup so a running PlaybackService (e.g. after a
        // swipe-away with music playing) repopulates state: connect() reads
        // currentMediaItem/isPlaying into the StateFlow. No player is created
        // here — everything goes through the MediaController.
        scope.launch {
            runCatching { connect() }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
            if (player.isPlaying && !tickerStarted) startTicker(player)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            controller?.let(::publish)
        }

        override fun onPlayerError(error: PlaybackException) {
            Timber.e(error, "Playback failed id=${controller?.currentMediaItem?.mediaId}")
            controller?.let(::publish)
            mutableState.value = mutableState.value.copy(
                error = error.message?.takeIf { it.isNotBlank() }
                    ?: "Unable to play this song. Try again."
            )
        }
    }

    /**
     * Connects to the service. Safe to call repeatedly; subsequent calls are
     * no-ops while a connection exists. A fresh connection starts with
     * repeat OFF and shuffle OFF (auto-advance through the queue).
     */
    suspend fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        val controller = future.await()
        this.controller = controller
        controller.repeatMode = Player.REPEAT_MODE_OFF
        controller.shuffleModeEnabled = false
        controller.addListener(listener)
        publish(controller)
    }

    /** Releases the controller connection. */
    fun release() {
        controllerFuture?.let {
            MediaController.releaseFuture(it)
            controllerFuture = null
        }
        controller = null
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

    /** Pauses playback (swipe-down on the mini-player). */
    suspend fun pause() {
        connect()
        controllerFuture?.await()?.pause()
    }

    /** Seeks to [positionMs] on the current item. */
    suspend fun seekTo(positionMs: Long) {
        connect()
        controllerFuture?.await()?.seekTo(positionMs)
    }

    /** Plays Waveyy [songs] from [startIndex], mapping through [QueueManager]. */
    suspend fun playSongs(songs: List<com.waveyy.app.domain.model.Song>, startIndex: Int = 0) {
        val items = queueManager.toMediaItems(songs)
        if (items.isEmpty()) return
        playItems(items, startIndex.coerceIn(items.indices))
    }

    /** Resolves a tap source into an index-aligned queue (pure). */
    fun resolveSource(source: QueueSource, startIndex: Int = 0): ResolvedQueue? {
        return queueManager.setQueueFromSource(source, startIndex)
    }

    /** Plays a tap source (see [QueueSource] variants). */
    suspend fun playSource(source: QueueSource, startIndex: Int = 0) {
        val resolved = resolveSource(source, startIndex) ?: return
        playItems(resolved.items, resolved.index)
    }

    /** Advances to the next queue item. */
    suspend fun next() {
        connect()
        controllerFuture?.await()?.seekToNext()
    }

    /** Returns to the previous queue item. */
    suspend fun previous() {
        connect()
        controllerFuture?.await()?.seekToPrevious()
    }

    /** Swaps the controller item at [index] with [song]'s fresh streams. */
    suspend fun replaceItem(index: Int, song: com.waveyy.app.domain.model.Song) {
        connect()
        val controller = controllerFuture?.await() ?: return
        if (index < 0 || index >= controller.mediaItemCount) return
        val item = queueManager.toMediaItem(song) ?: return
        controller.replaceMediaItem(index, item)
    }

    /** Cycles repeat mode OFF -> ALL -> ONE. */
    suspend fun cycleRepeatMode() {
        connect()
        val controller = controllerFuture?.await() ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        publish(controller)
    }

    /** Toggles shuffle. */
    suspend fun toggleShuffle() {
        connect()
        val controller = controllerFuture?.await() ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        publish(controller)
    }

    /** Moves a queue item. */
    suspend fun moveItem(from: Int, to: Int) {
        connect()
        controllerFuture?.await()?.moveMediaItem(from, to)
    }

    /** Removes a queue item. */
    suspend fun removeItem(index: Int) {
        connect()
        controllerFuture?.await()?.removeMediaItem(index)
    }

    /** Clears the queue. */
    suspend fun clearQueue() {
        connect()
        controllerFuture?.await()?.clearMediaItems()
    }

    /** Appends one Waveyy song to the controller queue. */
    suspend fun enqueueSong(song: com.waveyy.app.domain.model.Song) {
        connect()
        val item = queueManager.toMediaItem(song) ?: return
        controllerFuture?.await()?.addMediaItem(item)
    }

    /** Inserts one Waveyy song to play next (right after the current item). */
    suspend fun playNextSong(song: com.waveyy.app.domain.model.Song) {
        connect()
        val controller = controllerFuture?.await() ?: return
        val item = queueManager.toMediaItem(song) ?: return
        val at = (controller.currentMediaItemIndex + 1).coerceAtLeast(0)
        if (at >= controller.mediaItemCount) {
            controller.addMediaItem(item)
        } else {
            controller.addMediaItem(at, item)
        }
    }

    /** Sets playback speed (pitch stays at system default). */
    suspend fun setPlaybackSpeed(speed: Float) {
        connect()
        controllerFuture?.await()?.setPlaybackSpeed(speed.coerceIn(0.5f, 2.0f))
    }

    private fun publish(player: Player) {
        val previousError = mutableState.value.error
        Timber.tag("PlaybackConn").d(
            "publish: index=%d/%d id=%s playing=%s",
            player.currentMediaItemIndex,
            player.mediaItemCount,
            player.currentMediaItem?.mediaId,
            player.isPlaying
        )
        mutableState.value = PlaybackState(
            mediaId = player.currentMediaItem?.mediaId,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition,
            durationMs = player.duration.coerceAtLeast(0L),
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            repeatMode = player.repeatMode,
            shuffleEnabled = player.shuffleModeEnabled,
            error = previousError,
            mediaIndex = player.currentMediaItemIndex.coerceAtLeast(0)
        )
    }

    /** Clears a surfaced player error (after the Snackbar is shown). */
    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    /** Re-reads controller state (call on UI start to resync after reopen). */
    fun refreshState() {
        controller?.let(::publish)
    }

    /**
     * Snapshots the controller queue as domain songs (metadata only, no
     * streams). Used to rebuild the UI queue when the activity-scoped queue
     * was lost (swipe-away) while the service kept playing.
     */
    fun currentQueueSongs(): List<com.waveyy.app.domain.model.Song> {
        val controller = controller ?: return emptyList()
        return (0 until controller.mediaItemCount).mapNotNull { index ->
            val item = runCatching { controller.getMediaItemAt(index) }.getOrNull()
                ?: return@mapNotNull null
            val meta = item.mediaMetadata
            com.waveyy.app.domain.model.Song(
                id = item.mediaId,
                name = (meta.title ?: "").toString(),
                artist = (meta.artist ?: "").toString(),
                album = (meta.albumTitle ?: "").toString(),
                imageUrl = meta.artworkUri?.toString().orEmpty(),
                durationSec = 0L,
                playCount = 0L,
                streams = emptyList()
            ).takeIf { it.id.isNotBlank() }
        }
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
