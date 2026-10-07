package com.waveyy.app.data.remote

import com.waveyy.app.data.remote.dto.LyricsDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Lyrics proxy surface (`/api/lyrics` in the web app, backed by lrclib).
 * Base URL is `BuildConfig.API_BASE_URL`.
 */
interface LyricsApi {

    /**
     * Fetches synced + plain lyrics. Mirrors `fetchLyricsFromApi` in
     * `lib/lyrics-client.js` (album is sent when known; duration disambiguates).
     */
    @GET("lyrics")
    suspend fun getLyrics(
        @Query("track") track: String,
        @Query("artist") artist: String,
        @Query("album") album: String? = null,
        @Query("duration") duration: String? = null
    ): LyricsDto
}
