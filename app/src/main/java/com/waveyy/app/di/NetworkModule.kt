package com.waveyy.app.di

import com.waveyy.app.BuildConfig
import com.waveyy.app.data.remote.JiosaavnApi
import com.waveyy.app.data.remote.LyricsApi
import com.waveyy.app.data.remote.SpotifyRecApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/** Networking graph: lenient JSON, debug-only logging, three API surfaces. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** JSON tolerant to the upstream shape variance. */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    /** OkHttp with body logging in debug builds only. */
    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(30L, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Dedicated client for large binary downloads (APK updates). No
     * logging interceptor — BODY-level logging of a 27 MB stream stalls
     * the read and can trip the read timeout. Extended timeouts are for
     * slow mobile networks.
     */
    @Provides
    @Singleton
    @Named("download")
    fun provideDownloadOkHttp(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30L, TimeUnit.SECONDS)
            .readTimeout(5L, TimeUnit.MINUTES)
            .writeTimeout(30L, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /** Single Retrofit over `BuildConfig.API_BASE_URL` for all proxies. */
    @Provides
    @Singleton
    fun provideRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /** JioSaavn proxy surface. */
    @Provides
    @Singleton
    fun provideJiosaavnApi(retrofit: Retrofit): JiosaavnApi =
        retrofit.create(JiosaavnApi::class.java)

    /** Lyrics proxy surface. */
    @Provides
    @Singleton
    fun provideLyricsApi(retrofit: Retrofit): LyricsApi =
        retrofit.create(LyricsApi::class.java)

    /** Spotify-recommendations proxy surface. */
    @Provides
    @Singleton
    fun provideSpotifyRecApi(retrofit: Retrofit): SpotifyRecApi =
        retrofit.create(SpotifyRecApi::class.java)

    /** GitHub REST API (releases endpoint). Uses a dedicated base URL. */
    @Provides
    @Singleton
    @Named("github")
    fun provideGitHubRetrofit(json: Json, client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    /** GitHub release API surface. */
    @Provides
    @Singleton
    fun provideGitHubReleaseApi(@Named("github") retrofit: Retrofit): com.waveyy.app.data.remote.GitHubReleaseApi =
        retrofit.create(com.waveyy.app.data.remote.GitHubReleaseApi::class.java)

    /**
     * Direct-deployment Retrofit, or null when `SAAVN_API_URL` is blank.
     * Covers the two calls with no proxy equivalent (song fetch, album
     * search), mirroring `NEXT_PUBLIC_API_URL` usage in `lib/fetch.js`.
     */
    @Provides
    @Singleton
    @Named("saavnDirect")
    fun provideDirectApi(json: Json, client: OkHttpClient): com.waveyy.app.data.remote.SaavnDirectApi? {
        val base = BuildConfig.SAAVN_API_URL.trim()
        if (base.isBlank()) return null
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(if (base.endsWith("/")) base else "$base/")
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(com.waveyy.app.data.remote.SaavnDirectApi::class.java)
    }
}
