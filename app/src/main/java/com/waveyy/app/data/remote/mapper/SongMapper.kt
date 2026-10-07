package com.waveyy.app.data.remote.mapper

import com.waveyy.app.data.remote.dto.AlbumDto
import com.waveyy.app.data.remote.dto.ArtistDto
import com.waveyy.app.data.remote.dto.SaavnDownloadUrl
import com.waveyy.app.data.remote.dto.SaavnImage
import com.waveyy.app.data.remote.dto.SongDto
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.AudioStream
import com.waveyy.app.domain.model.Song
import com.waveyy.app.util.AudioQualitySelector
import com.waveyy.app.util.HtmlDecoder

/**
 * Normalizes the inconsistent JioSaavn shapes into domain models.
 * Mirrors `getQueueSongName/Artist/Image` in `lib/queue-utils.js` and the
 * `playCount` normalization in `lib/fetch.js`.
 */
object SongMapper {

    /** Display title: `name`, `title`, then legacy `song` (HTML-decoded). */
    fun titleOf(dto: SongDto): String {
        val title = dto.name ?: dto.title ?: dto.song
        return HtmlDecoder.decode(title?.takeIf { it.isNotBlank() } ?: "Unknown")
    }

    /**
     * Display artist: flat string, else joined `artists.primary` names
     * (HTML-decoded).
     */
    fun artistOf(dto: SongDto): String {
        dto.artist?.takeIf { it.isNotBlank() }?.let { return HtmlDecoder.decode(it.trim()) }
        val names = dto.artists?.primary.orEmpty().mapNotNull { it.name.takeIf(String::isNotBlank) }
        val joined = names.joinToString(", ").takeIf { it.isNotBlank() } ?: "unknown"
        return HtmlDecoder.decode(joined)
    }

    /** Best image: largest rendition first (`[2]`, `[1]`, `[0]`). */
    fun imageOf(images: List<SaavnImage>): String {
        val urls = images.map { it.url }.filter { it.isNotBlank() }
        return urls.lastOrNull() ?: ""
    }

    /** Display album name (HTML-decoded). */
    fun albumOf(dto: SongDto): String {
        return HtmlDecoder.decode(dto.album?.name?.takeIf { it.isNotBlank() } ?: "unknown")
    }

    /** Effective play count (`playCount`, falling back to `play_count`). */
    fun playCountOf(dto: SongDto): Long {
        return if (dto.playCount != 0L) dto.playCount else dto.playCountSnake
    }

    /** Raw download entries as [AudioStream] candidates. */
    fun streamsOf(entries: List<SaavnDownloadUrl>): List<AudioStream> {
        return entries.mapNotNull { entry ->
            val url = entry.url?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            AudioStream(url = url, quality = entry.quality ?: "", bitrateKbps = 0, lossless = false)
        }
    }

    /** Full normalization; returns null when the payload has no id. */
    fun toDomain(dto: SongDto): Song? {
        if (dto.id.isBlank()) return null
        return Song(
            id = dto.id,
            name = titleOf(dto),
            artist = artistOf(dto),
            album = albumOf(dto),
            imageUrl = imageOf(dto.image),
            durationSec = dto.duration,
            playCount = playCountOf(dto),
            streams = streamsOf(dto.downloadUrl)
        )
    }

    /** Normalizes a list, dropping id-less entries. */
    fun toDomainList(dtos: List<SongDto>): List<Song> {
        return dtos.mapNotNull(::toDomain)
    }

    /** Picks the single best stream URL, mirroring `selectBestAudioUrl`. */
    fun bestStreamUrl(dto: SongDto): String {
        return AudioQualitySelector.selectBestUrl(streamsOf(dto.downloadUrl))
    }
}

/** Album normalization. */
object AlbumMapper {

    /** Full normalization; returns null when the payload has no id. */
    fun toDomain(dto: AlbumDto): Album? {
        if (dto.id.isBlank()) return null
        val artist = dto.artist?.takeIf { it.isNotBlank() }
            ?: dto.artists?.primary.orEmpty().firstOrNull()?.name
            ?: "unknown"
        return Album(
            id = dto.id,
            name = HtmlDecoder.decode(dto.name ?: dto.title ?: "Unknown Album"),
            artist = HtmlDecoder.decode(artist),
            imageUrl = SongMapper.imageOf(dto.image),
            songs = SongMapper.toDomainList(dto.songs)
        )
    }
}

/** Artist normalization. */
object ArtistMapper {

    /** Full normalization; returns null when the payload has no id. */
    fun toDomain(dto: ArtistDto): Artist? {
        if (dto.id.isBlank()) return null
        return Artist(
            id = dto.id,
            name = HtmlDecoder.decode(dto.name ?: "Unknown Artist"),
            imageUrl = SongMapper.imageOf(dto.image),
            followerCount = dto.followerCount,
            topSongs = SongMapper.toDomainList(dto.topSongs),
            topAlbums = dto.topAlbums.mapNotNull(AlbumMapper::toDomain)
        )
    }
}
