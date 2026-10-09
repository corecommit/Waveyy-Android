package com.waveyy.app.playback

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.waveyy.app.domain.model.Song
import com.waveyy.app.util.AudioQualitySelector
import timber.log.Timber
import javax.inject.Inject

/**
 * Renders Waveyy songs as Media3 queue items.
 *
 * Each [Song] becomes one [MediaItem] whose URI is the best stream picked by
 * [AudioQualitySelector] and whose metadata (title, artist, album, artwork)
 * mirrors `hooks/use-media-session.jsx`, so lock-screen and notification
 * controls show the right track info.
 */
class QueueManager @Inject constructor() {
    /** Translates songs into playable media items, skipping stream-less ones. */
    fun toMediaItems(songs: List<Song>): List<MediaItem> {
        val seen = mutableSetOf<String>()
        return songs
            .filter { it.id.isNotBlank() && seen.add(it.id) }
            .mapNotNull(::toMediaItem)
    }

    /** Translates one song; null when no playable stream exists. */
    fun toMediaItem(song: Song): MediaItem? {
        val url = AudioQualitySelector.selectBestUrl(song.streams)
        Timber.d("MediaItem id=%s streams=%d uri=%s", song.id, song.streams.size, url)
        if (url.isBlank()) return null
        val metadata = MediaMetadata.Builder()
            .setTitle(song.name)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.imageUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
            .build()
        return MediaItem.Builder()
            .setMediaId(song.id)
            .setUri(url)
            .setMediaMetadata(metadata)
            .build()
    }

    /** Shared audio attributes: media usage with content type music. */
    fun audioAttributes(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
    }

    /**
     * Resolves a [QueueSource] tap into an index-aligned queue: stream-less
     * songs are dropped and the tapped position is re-resolved by id so the
     * domain queue and controller queue can never desync. Returns null when
     * nothing is playable.
     */
    fun setQueueFromSource(source: QueueSource, startIndex: Int): ResolvedQueue? {
        val tappedId = source.songs.getOrNull(startIndex)?.id
        // Dedupe by song id, keeping the FIRST occurrence so the tapped index
        // is stable. Also drop blank ids and stream-less songs.
        val seen = mutableSetOf<String>()
        val effective = source.songs
            .filter { song -> song.id.isNotBlank() && seen.add(song.id) }
            .filter { song -> song.streams.any { it.url.isNotBlank() } }
        if (effective.isEmpty()) return null
        val index = effective.indexOfFirst { it.id == tappedId }.takeIf { it >= 0 } ?: 0
        val items = toMediaItems(effective)
        Timber.tag("QueueMgr").d(
            "setQueueFromSource: input=%d unique=%d effective=%d tappedId=%s index=%d",
            source.songs.size,
            source.songs.map { it.id }.distinct().size,
            effective.size,
            tappedId,
            index
        )
        if (items.isEmpty()) return null
        return ResolvedQueue(effective, items, index)
    }
}

/** Where a queue tap came from (drives Up-Next semantics). */
sealed interface QueueSource {
    /** Playable songs in tap context. */
    val songs: List<Song>

    /** Search results rail/list. */
    data class SearchResults(override val songs: List<Song>) : QueueSource

    /** Album track list. */
    data class Album(override val songs: List<Song>) : QueueSource

    /** Artist top tracks / podium. */
    data class Artist(override val songs: List<Song>) : QueueSource

    /** Playlist detail list. */
    data class Playlist(override val songs: List<Song>) : QueueSource

    /** Recommendations / curated rails. */
    data class Recommendations(override val songs: List<Song>) : QueueSource

    /** Home feed rails. */
    data class HomeFeed(override val songs: List<Song>) : QueueSource

    /** Ad-hoc list (resolved queues, tests). */
    data class Custom(override val songs: List<Song>) : QueueSource
}

/** Index-aligned domain + controller queue pair. */
data class ResolvedQueue(
    val songs: List<Song>,
    val items: List<MediaItem>,
    val index: Int
)
