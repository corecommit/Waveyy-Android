package com.waveyy.app.domain.model

import kotlinx.serialization.Serializable

/** An artist with top songs and top albums. */
@Serializable
data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val followerCount: Long = 0L,
    val topSongs: List<Song> = emptyList(),
    val topAlbums: List<Album> = emptyList()
)
