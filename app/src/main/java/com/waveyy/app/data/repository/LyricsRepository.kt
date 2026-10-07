package com.waveyy.app.data.repository

import com.waveyy.app.data.local.dao.LyricsDao
import com.waveyy.app.data.local.LyricsEntity
import com.waveyy.app.data.remote.LyricsApi
import com.waveyy.app.domain.model.CachedLyrics
import com.waveyy.app.util.LyricsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lyrics with a 14-day cache-then-fetch policy, mirroring `getLyrics` in
 * `lib/lyrics-client.js` (IndexedDB/localStorage there, Room here).
 */
@Singleton
class LyricsRepository @Inject constructor(
    private val api: LyricsApi,
    private val dao: LyricsDao
) {

    /**
     * Returns cached lyrics when fresh, otherwise fetches, caches, and returns
     * them. Returns null when nothing is available.
     */
    fun getLyrics(
        cacheKey: String,
        track: String,
        artist: String,
        album: String? = null,
        duration: String? = null
    ): Flow<CachedLyrics?> = flow {
        prune()
        val cached = dao.get(cacheKey)
        if (cached != null && System.currentTimeMillis() - cached.ts < CACHE_TTL_MILLIS) {
            emit(CachedLyrics(cached.synced, cached.plain))
            return@flow
        }
        val fetched = runCatching {
            api.getLyrics(track = track, artist = artist, album = album, duration = duration)
        }.getOrNull()
        val payload = LyricsParser.sanitize(
            fetched?.syncedLyrics.orEmpty(),
            fetched?.plainLyrics.orEmpty()
        )
        if (payload.first.isBlank() && payload.second.isBlank()) {
            emit(cached?.let { CachedLyrics(it.synced, it.plain) })
            return@flow
        }
        dao.put(LyricsEntity(cacheKey, payload.first, payload.second, System.currentTimeMillis()))
        emit(CachedLyrics(payload.first, payload.second))
    }.flowOn(Dispatchers.IO)

    private suspend fun prune() {
        dao.pruneOlderThan(System.currentTimeMillis() - CACHE_TTL_MILLIS)
    }

    companion object {
        /** 14-day TTL, matching `LYRICS_CACHE_TTL_MS`. */
        const val CACHE_TTL_MILLIS = 14L * 24L * 60L * 60L * 1000L
    }
}
