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
                HttpLoggingInterceptor.Level.BASIC
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
}
