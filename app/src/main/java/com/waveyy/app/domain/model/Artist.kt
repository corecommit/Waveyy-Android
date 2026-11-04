package com.waveyy.app.domain.model

/** An artist with top songs and top albums. */
data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val followerCount: Long,
    val topSongs: List<Song>,
    val topAlbums: List<Album>
)
