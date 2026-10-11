package com.waveyy.app.data.repository

import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.remote.ytmusic.YtMusicApi
import com.waveyy.app.domain.model.AudioStream
import com.waveyy.app.domain.model.MusicSource
import com.waveyy.app.domain.model.Song
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * YouTube Music catalog access over the first-party InnerTube client.
 * Scope-disciplined: search + stream URL only (no login, library sync,
 * or recommendations). Every failure mode returns empty/null so the
 * hybrid layer falls back to JioSaavn.
 */
@Singleton
class YtMusicRepository @Inject constructor(
    private val api: YtMusicApi,
    private val userPrefs: UserPrefs
) {

    /** Searches tracks, mapped to domain songs with source YTMUSIC. */
    suspend fun searchSongs(query: String): List<Song> {
        return runCatching {
            val visitor = userPrefs.ytVisitor()
            val result = api.search(query, visitor)
            result.visitorData?.let { userPrefs.setYtVisitor(it) }
            result.tracks.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    /**
     * Fetches the YT Music "radio" for [videoId] — the queue YouTube
     * itself would autoplay after this track. Returns up to 25 tracks
     * in YT's relevance order. Empty when the request fails.
     */
    suspend fun nextRadio(videoId: String): List<Song> {
        return runCatching {
            val visitor = userPrefs.ytVisitor()
            val tracks = api.nextRadio(videoId, visitor)
            tracks.map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    /**
     * Resolves a playable stream URL, or null when gated (login/bot-check)
     * or unresolvable.
     */
    suspend fun streamUrl(videoId: String): String? {
        return runCatching {
            api.streamUrl(videoId, userPrefs.ytVisitor())?.url
        }.getOrNull()
    }

    private fun com.waveyy.app.data.remote.ytmusic.dto.YtTrack.toDomain(): Song {
        return Song(
            id = "yt:$videoId",
            name = title,
            artist = artist,
            album = "unknown",
            imageUrl = thumbnailUrl,
            durationSec = durationSec,
            playCount = 0L,
            streams = emptyList(),
            source = MusicSource.YTMUSIC
        )
    }
}
