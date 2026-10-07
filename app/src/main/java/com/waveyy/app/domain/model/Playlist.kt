package com.waveyy.app.domain.model

/** A user playlist (Supabase-backed, see Phase 9). */
data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val createdAt: Long
)
