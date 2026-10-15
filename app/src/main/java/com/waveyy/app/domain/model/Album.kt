package com.waveyy.app.domain.model

import kotlinx.serialization.Serializable

/** An album with its track list. */
@Serializable
data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val imageUrl: String,
    val songs: List<Song> = emptyList()
)
