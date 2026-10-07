package com.waveyy.app.data.remote

import com.waveyy.app.data.remote.dto.AlbumResponse
import com.waveyy.app.data.remote.dto.ArtistResponse
import com.waveyy.app.data.remote.dto.ChartsResponse
import com.waveyy.app.data.remote.dto.SongResponse
import com.waveyy.app.data.remote.dto.SongsResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Same-origin `/api/jiosaavn` proxy surface, mirroring the `path` values in
 * `lib/jiosaavn-public.js` (`searchSongs`, `topCharts`, `albumDetails`,
 * `artistDetails`, `recommendedSongs`). Base URL is `BuildConfig.API_BASE_URL`.
 */
interface JiosaavnApi {

    /** Proxies `search/songs` on the upstream deployment. */
    @GET("jiosaavn")
    suspend fun searchSongs(
        @Query("path") path: String = "searchSongs",
        @Query("q") query: String
    ): SongsResponse

    /** Proxies the trending/modules upstream with chart normalization. */
    @GET("jiosaavn")
    suspend fun topCharts(
        @Query("path") path: String = "topCharts"
    ): ChartsResponse

    /** Proxies `albums?id=`. */
    @GET("jiosaavn")
    suspend fun albumDetails(
        @Query("path") path: String = "albumDetails",
        @Query("albumId") albumId: String
    ): AlbumResponse

    /** Proxies `artists?id=`. */
    @GET("jiosaavn")
    suspend fun artistDetails(
        @Query("path") path: String = "artistDetails",
        @Query("artistId") artistId: String
    ): ArtistResponse

    /**
     * Proxies song recommendations. The envelope varies by upstream, so the
     * raw payload is returned for [RecommendedSongsParser].
     */
    @GET("jiosaavn")
    suspend fun recommendedSongs(
        @Query("path") path: String = "recommendedSongs",
        @Query("songId") songId: String
    ): JsonElement
}
