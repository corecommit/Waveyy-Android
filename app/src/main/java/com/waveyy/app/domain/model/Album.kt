package com.waveyy.app.domain.model

/** An album with its track list. */
data class Album(
    val id: String,
    val name: String,
    val artist: String,
    val imageUrl: String,
    val songs: List<Song>
)
