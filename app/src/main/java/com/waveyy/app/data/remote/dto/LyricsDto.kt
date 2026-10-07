package com.waveyy.app.data.remote.dto

import kotlinx.serialization.Serializable

/** lrclib response shape forwarded by our `/api/lyrics` proxy. */
@Serializable
data class LyricsDto(
    val syncedLyrics: String? = null,
    val plainLyrics: String? = null
)

/**
 * Spotify-recommendations proxy response. The route maps every Spotify pick
 * back onto a JioSaavn song, so the payload is a song list.
 */
@Serializable
data class SpotifyRecResponse(
    val success: Boolean = false,
    val data: List<SongDto> = emptyList()
)
