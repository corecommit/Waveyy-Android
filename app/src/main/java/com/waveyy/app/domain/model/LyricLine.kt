package com.waveyy.app.domain.model

/** One synced lyric line. */
data class LyricLine(
    val timeMs: Long,
    val text: String
)

/** Cached lyric payload (synced LRC + plain text). */
data class CachedLyrics(
    val synced: String,
    val plain: String
)
