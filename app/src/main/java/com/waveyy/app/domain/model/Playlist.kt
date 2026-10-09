package com.waveyy.app.domain.model

import kotlinx.serialization.Serializable

/** A user playlist (Supabase-backed, see Phase 9). */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val createdAt: Long
)
