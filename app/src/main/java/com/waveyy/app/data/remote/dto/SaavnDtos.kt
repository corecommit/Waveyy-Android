package com.waveyy.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One image rendition from the JioSaavn `image` array. */
@Serializable
data class SaavnImage(
    val url: String = ""
)

/** One entry of `artists.primary`. */
@Serializable
data class SaavnArtistRef(
    val id: String = "",
    val name: String = "",
    val image: List<SaavnImage> = emptyList()
)

/** The `artists` object on songs/albums. */
@Serializable
data class SaavnArtists(
    val primary: List<SaavnArtistRef> = emptyList()
)

/** Minimal album reference embedded in songs. */
@Serializable
data class SaavnAlbumRef(
    val name: String? = null,
    val id: String? = null,
    val language: String? = null
)

/** One entry of the `downloadUrl` array. */
@Serializable
data class SaavnDownloadUrl(
    val quality: String? = null,
    val url: String? = null
)

/**
 * A song in any JioSaavn response. Field names vary across endpoints
 * (`name`/`title`, `playCount`/`play_count`), so every alias is accepted
 * and normalized in [com.waveyy.app.data.remote.mapper.SongMapper].
 */
@Serializable
data class SongDto(
    val id: String = "",
    val name: String? = null,
    val title: String? = null,
    val song: String? = null,
    val image: List<SaavnImage> = emptyList(),
    val artists: SaavnArtists? = null,
    val artist: String? = null,
    val album: SaavnAlbumRef? = null,
    @Serializable(with = FlexibleLongSerializer::class)
    val duration: Long = 0L,
    @Serializable(with = FlexibleLongSerializer::class)
    val playCount: Long = 0L,
    @SerialName("play_count")
    @Serializable(with = FlexibleLongSerializer::class)
    val playCountSnake: Long = 0L,
    val downloadUrl: List<SaavnDownloadUrl> = emptyList(),
    val language: String? = null,
    val genres: List<String> = emptyList(),
    val genre: String? = null,
    val year: String? = null,
    val releaseDate: String? = null
)

/** `{success, data: {results|...}}` search envelope. */
@Serializable
data class SongsResponse(
    val success: Boolean = false,
    val data: SongsData? = null
)

/** Payload of [SongsResponse]; endpoints disagree on the list key. */
@Serializable
data class SongsData(
    val results: List<SongDto> = emptyList(),
    val songs: List<SongDto> = emptyList()
)

/** Single-song envelope (`songs/<id>`). */
@Serializable
data class SongResponse(
    val success: Boolean = false,
    val data: SongDto? = null
)

/** Album detail envelope (`albums?id=`). */
@Serializable
data class AlbumResponse(
    val success: Boolean = false,
    val data: AlbumDto? = null
)

/** Album search envelope (`search/albums?query=`, direct deployment). */
@Serializable
data class AlbumSearchResponse(
    val success: Boolean = false,
    val data: AlbumSearchData? = null
)

/** Payload of [AlbumSearchResponse]. */
@Serializable
data class AlbumSearchData(
    val results: List<AlbumDto> = emptyList()
)

/** Album payload. */
@Serializable
data class AlbumDto(
    val id: String = "",
    val name: String? = null,
    val title: String? = null,
    val image: List<SaavnImage> = emptyList(),
    val artists: SaavnArtists? = null,
    val artist: String? = null,
    val songs: List<SongDto> = emptyList()
)

/** Artist detail envelope (`artists?id=`). */
@Serializable
data class ArtistResponse(
    val success: Boolean = false,
    val data: ArtistDto? = null
)

/** Artist payload. */
@Serializable
data class ArtistDto(
    val id: String = "",
    val name: String? = null,
    val image: List<SaavnImage> = emptyList(),
    @Serializable(with = FlexibleLongSerializer::class)
    val followerCount: Long = 0L,
    val topSongs: List<SongDto> = emptyList(),
    val topAlbums: List<AlbumDto> = emptyList()
)
