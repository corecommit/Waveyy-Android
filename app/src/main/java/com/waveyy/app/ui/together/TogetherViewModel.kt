package com.waveyy.app.ui.together

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.waveyy.app.domain.model.RoomState
import com.waveyy.app.realtime.RoomRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Listen Together screen logic: create/join/leave rooms over
 * [RoomRepository]. Room ids are 8-char uppercase alphanumeric codes
 * (excluding visually ambiguous characters) so users can read them aloud
 * and type them by hand. A deep link may carry `roomId`, which is joined
 * automatically on entry.
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

    /**
     * Joins an existing room. Accepts either a raw code (`K7NP2XRM`) or a
     * full `waveyy://room/K7NP2XRM` link — the code is extracted from the
     * link when present. Input is normalized to uppercase with
     * non-alphanumeric characters stripped.
     */
    fun joinRoom(input: String) {
        val code = normalizeCode(input)
        if (code.length >= MIN_CODE_LENGTH) rooms.joinRoom(code)
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
        /** Characters used in room codes (no I, O, 0, 1 — visually ambiguous). */
        private const val ROOM_ID_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        /** Room code length. */
        private const val ROOM_ID_LENGTH = 8

        /** Minimum accepted code length (allows older 7-char codes). */
        private const val MIN_CODE_LENGTH = 6

        /**
         * Generates a new 8-char room code. Uppercase alphanumeric, no
         * ambiguous characters so users can read them over voice chat.
         */
        fun randomRoomId(): String {
            return (1..ROOM_ID_LENGTH)
                .map { ROOM_ID_CHARS.random() }
                .joinToString("")
        }

        /**
         * Normalizes user input: extracts the last path segment from a
         * `waveyy://room/<code>` link if present, uppercases it, and strips
         * anything that isn't A-Z or 0-9.
         */
        fun normalizeCode(input: String): String {
            val trimmed = input.trim()
            val code = if (trimmed.contains("/")) {
                trimmed.substringAfterLast("/").trim()
            } else {
                trimmed
            }
            return code.uppercase().filter { it.isLetterOrDigit() }
        }
    }
}
