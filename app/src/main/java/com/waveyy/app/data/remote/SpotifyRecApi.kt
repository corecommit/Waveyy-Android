package com.waveyy.app.data.remote

import com.waveyy.app.data.remote.dto.SpotifyRecResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Spotify-recommendations proxy surface. The route maps every Spotify pick
 * back onto a JioSaavn song, so results are directly playable.
 * Base URL is `BuildConfig.API_BASE_URL`.
 */
interface SpotifyRecApi {

    /** Recommendations for a track, mirroring `getSpotifyRecommendations`. */
    @GET("spotify/recommendations")
    suspend fun recommend(
        @Query("name") name: String,
        @Query("artist") artist: String,
        @Query("limit") limit: Int = 12
    ): SpotifyRecResponse
}
