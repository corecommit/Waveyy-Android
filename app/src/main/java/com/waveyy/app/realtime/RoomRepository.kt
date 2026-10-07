package com.waveyy.app.realtime

import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.RoomState
import com.waveyy.app.playback.PlaybackConnection
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listen Together orchestration, porting `room-provider.jsx`.
 *
 * - Joins rooms with the user profile (Supabase display name or Guest) and
 *   keeps the participant list in [state].
 * - Broadcasts local song changes; applies remote `change-song` with a
 *   `lastReceivedSong` loop guard.
 * - Host heartbeat: emits `sync-time` every 5s while playing. Guests apply
 *   incoming time only when drift exceeds 3s and never when hosting.
 * - `isHost` stays internal and is never exposed to consumers.
 *
 * Doze trade-off: the socket lives as long as this process does. While music
 * plays, `PlaybackService` (type `mediaPlayback`) holds a foreground service
 * that keeps the process — and therefore the socket — alive. With playback
 * paused and the app backgrounded, Doze may suspend the socket until the app
 * returns; rooms tolerate this via `request-sync` on rejoin/visibility.
 */
@Singleton
class RoomRepository @Inject constructor(
    private val socket: RoomSocket,
    private val playback: PlaybackConnection,
    private val songs: SongRepository,
    private val auth: AuthRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mutableState = MutableStateFlow(RoomState())
    /** Room snapshot for the UI. */
    val state: StateFlow<RoomState> = mutableState.asStateFlow()

    private var isHost = false
    private var lastReceivedSong: String? = null
    private var heartbeat: Job? = null
    private var changeWatcher: Job? = null
    private val listeners = mutableListOf<Pair<String, Emitter.Listener>>()

    /** Joins [roomId], registering the full protocol handlers. */
    fun joinRoom(roomId: String) {
        if (mutableState.value.roomId == roomId && socket.isConnected) return
        leaveRoom(silent = true)
        socket.connect()
        mutableState.value = RoomState(roomId = roomId)
        register(RoomEvents.USER_JOINED) { onUserJoined() }
        register(RoomEvents.ROOM_USERS_UPDATE) { args ->
            val users = socket.parseUsers(args.firstOrNull())
            mutableState.value = mutableState.value.copy(users = users)
        }
        register(RoomEvents.HOST_STATUS) { args ->
            isHost = args.firstOrNull() as? Boolean ?: false
            restartHeartbeat()
        }
        register(RoomEvents.SYNC_STATE) { args ->
            (args.firstOrNull() as? JSONObject)?.let(::applySyncState)
        }
        register(RoomEvents.PLAY) { args -> onPlay((args.firstOrNull() as? Number)?.toDouble() ?: 0.0) }
        register(RoomEvents.PAUSE) { args -> onPause((args.firstOrNull() as? Number)?.toDouble() ?: 0.0) }
        register(RoomEvents.SEEK) { args -> onSeek((args.firstOrNull() as? Number)?.toDouble() ?: 0.0) }
        register(RoomEvents.CHANGE_SONG) { args ->
            (args.firstOrNull() as? String)?.let(::onChangeSong)
        }
        register(RoomEvents.SYNC_TIME) { args ->
            if (!isHost) onSyncTime((args.firstOrNull() as? Number)?.toDouble() ?: 0.0)
        }
        register(RoomEvents.REQUEST_SYNC) { emitSyncState() }
        socket.emit(RoomEvents.JOIN_ROOM, roomId, profileJson())
        socket.emit(RoomEvents.REQUEST_SYNC, roomId)
        watchLocalChanges(roomId)
        restartHeartbeat()
    }

    /** Leaves the room and tears down handlers, heartbeat, and watchers. */
    fun leaveRoom(silent: Boolean = false) {
        val roomId = mutableState.value.roomId
        heartbeat?.cancel()
        heartbeat = null
        changeWatcher?.cancel()
        changeWatcher = null
        listeners.forEach { (event, listener) -> socket.off(event) }
        listeners.clear()
        isHost = false
        lastReceivedSong = null
        mutableState.value = RoomState()
        if (!silent) Timber.d("Left room $roomId")
        if (roomId != null) socket.disconnect()
    }

    /** Emits a play event at [positionSec]. */
    fun emitPlay(positionSec: Double) {
        roomId()?.let { socket.emit(RoomEvents.PLAY, it, positionSec) }
    }

    /** Emits a pause event at [positionSec]. */
    fun emitPause(positionSec: Double) {
        roomId()?.let { socket.emit(RoomEvents.PAUSE, it, positionSec) }
    }

    private fun roomId(): String? = mutableState.value.roomId

    private fun register(event: String, handler: (Array<out Any?>) -> Unit) {
        val listener = Emitter.Listener { args -> handler(args) }
        listeners += event to listener
        socket.on(event, listener)
    }

    private fun profileJson(): JSONObject {
        val user = auth.currentUser()
        val name = user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Guest"
        return socket.profileJson(name, user?.avatarUrl)
    }

    private fun onUserJoined() {
        // A newcomer arrived; share current state so it can catch up.
        emitSyncState()
    }

    private fun emitSyncState() {
        val id = roomId() ?: return
        scope.launch(Dispatchers.Main.immediate) {
            val current = playback.state.value
            val payload = JSONObject()
                .put("music", current.mediaId ?: JSONObject.NULL)
                .put("playing", current.isPlaying)
                .put("currentTime", current.positionMs / 1000.0)
            socket.emit(RoomEvents.SYNC_STATE, id, payload)
        }
    }

    private fun applySyncState(payload: JSONObject) {
        val songId = payload.optString("music", null)?.takeIf { it.isNotBlank() } ?: return
        lastReceivedSong = songId
        scope.launch {
            resolveAndPlay(songId)
            if (payload.optBoolean("playing", false)) {
                playback.togglePlayPauseIfPaused()
            }
            val time = payload.optDouble("currentTime", Double.NaN)
            if (!time.isNaN()) playback.seekTo((time * 1000).toLong())
        }
    }

    private fun onPlay(timeSec: Double) {
        scope.launch {
            playback.seekTo((timeSec * 1000).toLong())
            playback.togglePlayPauseIfPaused()
        }
    }

    private fun onPause(timeSec: Double) {
        scope.launch {
            playback.seekTo((timeSec * 1000).toLong())
            playback.togglePlayPauseIfPlaying()
        }
    }

    private fun onSeek(timeSec: Double) {
        scope.launch { playback.seekTo((timeSec * 1000).toLong()) }
    }

    private fun onChangeSong(songId: String) {
        lastReceivedSong = songId
        scope.launch { resolveAndPlay(songId) }
    }

    private fun onSyncTime(timeSec: Double) {
        scope.launch {
            val current = playback.state.value.positionMs / 1000.0
            if (kotlin.math.abs(current - timeSec) > DRIFT_GUARD_SEC) {
                playback.seekTo((timeSec * 1000).toLong())
            }
        }
    }

    private suspend fun resolveAndPlay(songId: String) {
        val song = songs.songById(songId).first()
        if (song != null) {
            playback.playSongs(listOf(song), 0)
        } else {
            Timber.w("Room sync references unknown song $songId")
        }
    }

    private fun watchLocalChanges(roomId: String) {
        changeWatcher?.cancel()
        changeWatcher = scope.launch {
            var lastEmitted: String? = null
            playback.state.collect { state ->
                val mediaId = state.mediaId
                if (mediaId != null && mediaId != lastEmitted && mediaId != lastReceivedSong) {
                    lastEmitted = mediaId
                    socket.emit(RoomEvents.CHANGE_SONG, roomId, mediaId)
                }
            }
        }
    }

    private fun restartHeartbeat() {
        heartbeat?.cancel()
        val roomId = roomId() ?: return
        heartbeat = scope.launch {
            while (isActive) {
                delay(HEARTBEAT_MILLIS)
                val current = playback.state.value
                if (isHost && current.isPlaying) {
                    socket.emit(RoomEvents.SYNC_TIME, roomId, current.positionMs / 1000.0)
                }
            }
        }
    }

    companion object {
        private const val HEARTBEAT_MILLIS = 5000L
        private const val DRIFT_GUARD_SEC = 3.0
    }
}

/** Plays only when currently paused (room-sync helper). */
suspend fun PlaybackConnection.togglePlayPauseIfPaused() {
    if (!state.value.isPlaying) togglePlayPause()
}

/** Pauses only when currently playing (room-sync helper). */
suspend fun PlaybackConnection.togglePlayPauseIfPlaying() {
    if (state.value.isPlaying) togglePlayPause()
}
