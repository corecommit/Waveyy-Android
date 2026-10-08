package com.waveyy.app.data.remote.ytmusic.dto

/**
 * Parsed YouTube Music track (search result or player metadata).
 * The raw InnerTube shapes are traversed defensively in
 * [com.waveyy.app.data.remote.ytmusic.YtMusicApi] — these carry only
 * what playback needs.
 */
data class YtTrack(
    /** 11-char video id. */
    val videoId: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnailUrl: String
)

/**
 * Resolved playable stream.
 */
data class YtStream(
    val url: String,
    val bitrate: Long,
    val mimeType: String
)
