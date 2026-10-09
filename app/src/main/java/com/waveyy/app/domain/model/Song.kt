package com.waveyy.app.domain.model

import kotlinx.serialization.Serializable

/**
 * Canonical audio stream candidate for a song, mirroring one entry of the
 * JioSaavn `downloadUrl` array after quality ranking.
 */
@Serializable
data class AudioStream(
    val url: String,
    val quality: String,
    val bitrateKbps: Int,
    val lossless: Boolean
)

/**
 * A playable song, normalized from the various JioSaavn response shapes
 * (see `SongMapper`).
 */
@Serializable
data class Song(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val imageUrl: String,
    val durationSec: Long,
    val playCount: Long,
    val streams: List<AudioStream>,
    val year: String = "",
    val source: MusicSource = MusicSource.JIOSAAVN,
    /** First `artists.primary` id from the API ("" when unknown). */
    val primaryArtistId: String = ""
)
