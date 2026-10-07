package com.waveyy.app.data.remote

import com.waveyy.app.data.remote.dto.AlbumSearchResponse
import com.waveyy.app.data.remote.dto.SongResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Direct JioSaavn deployment calls, mirroring `lib/fetch.js`
 * (`NEXT_PUBLIC_API_URL`). Base URL is `BuildConfig.SAAVN_API_URL` and the
 * client is disabled when it is blank — the proxy surface in [JiosaavnApi]
 * is the primary path.
 */
interface SaavnDirectApi {

    /** Direct album search (`search/albums?query=`). */
    @GET("search/albums")
    suspend fun searchAlbums(@Query("query") query: String): AlbumSearchResponse

    /** Direct song fetch (`songs/<id>`). */
    @GET("songs/{id}")
    suspend fun songById(@Path("id") id: String): SongResponse
}
