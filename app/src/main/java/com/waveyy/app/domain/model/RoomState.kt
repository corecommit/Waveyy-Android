package com.waveyy.app.domain.model

/** One participant in a Listen Together room. */
data class RoomUser(
    val socketId: String,
    val name: String,
    val avatarUrl: String?
)

/**
 * Listen Together room snapshot. `isHost` is intentionally absent —
 * consumers must never branch on host status (matches `room-provider.jsx`).
 */
data class RoomState(
    val roomId: String? = null,
    val users: List<RoomUser> = emptyList()
)
