package com.waveyy.app.playback

import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.waveyy.app.domain.model.Song
import com.waveyy.app.util.AudioQualitySelector
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
        return songs.mapNotNull(::toMediaItem)
    }

    /** Translates one song; null when no playable stream exists. */
    fun toMediaItem(song: Song): MediaItem? {
        val url = AudioQualitySelector.selectBestUrl(song.streams)
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
}
