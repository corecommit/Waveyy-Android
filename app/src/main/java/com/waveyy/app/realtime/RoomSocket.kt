package com.waveyy.app.realtime

import com.waveyy.app.BuildConfig
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Socket.IO event names of the Listen Together protocol (see `server.js`). */
object RoomEvents {
    const val JOIN_ROOM = "join-room"
    const val USER_JOINED = "user-joined"
    const val ROOM_USERS_UPDATE = "room-users-update"
    const val HOST_STATUS = "host-status"
    const val SYNC_STATE = "sync-state"
    const val PLAY = "play"
    const val PAUSE = "pause"
    const val SEEK = "seek"
    const val CHANGE_SONG = "change-song"
    const val SYNC_TIME = "sync-time"
    const val REQUEST_SYNC = "request-sync"

    /** All server-to-client events, for bulk unregistration on leave. */
    val inbound = listOf(
        USER_JOINED, ROOM_USERS_UPDATE, HOST_STATUS, SYNC_STATE, PLAY,
        PAUSE, SEEK, CHANGE_SONG, SYNC_TIME, REQUEST_SYNC
    )
}

/**
 * Thin wrapper around `socket.io-client` speaking the room protocol from
 * `server.js` / `socket.js`. Connection failures (e.g. placeholder
 * `SOCKET_URL`) surface as log lines only — never crashes.
 */
@Singleton
class RoomSocket @Inject constructor() {

    private var socket: Socket? = null

    /** True while the underlying socket is connected. */
    val isConnected: Boolean
        get() = socket?.connected() == true

    /** Connects to `BuildConfig.SOCKET_URL` (idempotent). */
    fun connect() {
        if (socket != null) return
        runCatching {
            val options = IO.Options().apply {
                path = "/socket.io"
                transports = arrayOf("websocket", "polling")
            }
            socket = IO.socket(BuildConfig.SOCKET_URL, options).apply {
                on(Socket.EVENT_CONNECT) { Timber.d("RoomSocket connected") }
                on(Socket.EVENT_CONNECT_ERROR) { args ->
                    Timber.w("RoomSocket connect error: ${args.firstOrNull()}")
                }
                on(Socket.EVENT_DISCONNECT) { Timber.d("RoomSocket disconnected") }
                connect()
            }
        }.onFailure { Timber.w("RoomSocket failed to start: ${it.message}") }
    }

    /** Disconnects and drops all listeners. */
    fun disconnect() {
        runCatching {
            socket?.let { current ->
                RoomEvents.inbound.forEach(current::off)
                current.disconnect()
                current.close()
            }
        }
        socket = null
    }

    /** Registers an inbound handler. */
    fun on(event: String, listener: Emitter.Listener): RoomSocket {
        socket?.on(event, listener)
        return this
    }

    /** Removes all handlers for [event]. */
    fun off(event: String) {
        socket?.off(event)
    }

    /** Emits an outbound event (no-op while disconnected). */
    fun emit(event: String, vararg args: Any?) {
        if (!isConnected) return
        runCatching { socket?.emit(event, *args) }
    }

    /** Builds the `join-room` profile payload. */
    fun profileJson(name: String, avatarUrl: String?): JSONObject {
        return JSONObject()
            .put("name", name)
            .put("avatar", avatarUrl ?: JSONObject.NULL)
    }

    /** Parses a `room-users-update` array into participants. */
    fun parseUsers(payload: Any?): List<com.waveyy.app.domain.model.RoomUser> {
        val array = payload as? JSONArray ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val entry = array.optJSONObject(index) ?: return@mapNotNull null
            val user = entry.optJSONObject("user") ?: JSONObject()
            com.waveyy.app.domain.model.RoomUser(
                socketId = entry.optString("id"),
                name = user.optString("name", "Guest").ifBlank { "Guest" },
                avatarUrl = user.optString("avatar", null)?.takeIf { it.isNotBlank() }
            )
        }
    }
}
