package com.waveyy.app.ui.together

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.waveyy.app.domain.model.RoomState
import com.waveyy.app.realtime.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Listen Together screen logic: create/leave rooms over [RoomRepository].
 * Room ids are short random strings, matching `together/page.jsx`. A deep
 * link may carry `roomId`, which is joined automatically.
 */
@HiltViewModel
class TogetherViewModel @Inject constructor(
    private val rooms: RoomRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Room snapshot. */
    val state: StateFlow<RoomState> = rooms.state

    init {
        savedStateHandle.get<String>("roomId")?.takeIf { it.isNotBlank() }?.let(::joinRoom)
    }

    /** Creates a room and joins it. */
    fun createRoom() {
        rooms.joinRoom(randomRoomId())
    }

    /** Joins an existing room id. */
    fun joinRoom(roomId: String) {
        if (roomId.isNotBlank()) rooms.joinRoom(roomId.trim())
    }

    /** Leaves the current room. */
    fun leaveRoom() {
        rooms.leaveRoom()
    }

    /** Shareable link for the current room, or null. */
    fun roomLink(): String? {
        val id = state.value.roomId ?: return null
        return "waveyy://room/$id"
    }

    companion object {
        private const val ROOM_ID_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"
        private const val ROOM_ID_LENGTH = 7

        /** Short random room id (mirrors the web's Math.random scheme). */
        fun randomRoomId(): String {
            return (1..ROOM_ID_LENGTH)
                .map { ROOM_ID_CHARS.random() }
                .joinToString("")
        }
    }
}
