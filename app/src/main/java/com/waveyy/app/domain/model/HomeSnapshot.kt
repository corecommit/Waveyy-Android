package com.waveyy.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Cached snapshot of Home's four network-sourced rails. Written after
 * every successful load; read on cold start to skip the skeleton and
 * skip the network entirely when still fresh.
 *
 * recentlyPlayed is intentionally NOT included — it comes from Room and
 * is always available synchronously.
 */
@Serializable
data class HomeSnapshot(
    val quickPicks: List<Song> = emptyList(),
    val madeForYou: List<Song> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    /** Epoch millis when the snapshot was captured. */
    val timestampMs: Long
)
