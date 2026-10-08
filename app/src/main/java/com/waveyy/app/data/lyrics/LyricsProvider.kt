package com.waveyy.app.data.lyrics

/** One lyrics source. Implementations must be keyless-safe: self-disable when unusable. */
interface LyricsProvider {
    /** Stable id used in settings (`lrclib`, `netease`, ...). */
    val name: String
    /** False when the provider cannot run (e.g. missing API key). */
    val enabled: Boolean
    /** Returns lyrics or null when this provider has nothing. Never throws. */
    suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult?
}

/** A single provider's answer. */
sealed class LyricsResult {
    /** Timestamped LRC payload. */
    data class Synced(val lrc: String) : LyricsResult()
    /** Untimed lyrics text. */
    data class Plain(val text: String) : LyricsResult()
    /** Track has no vocals. */
    object Instrumental : LyricsResult()
}
