package com.waveyy.app.data.repository

import app.cash.turbine.test
import com.waveyy.app.data.remote.JiosaavnApi
import com.waveyy.app.data.remote.SpotifyRecApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

/** Repository tests over a mock backend, asserting flows with Turbine. */
class SongRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false; coerceInputValues = true }

    private fun repository(server: MockWebServer): SongRepository {
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return SongRepository(
            retrofit.create(JiosaavnApi::class.java),
            retrofit.create(SpotifyRecApi::class.java)
        )
    }

    @Test
    fun `searchSongs emits normalized songs`() = runTest {
        val server = MockWebServer()
        server.enqueue(
            MockResponse().setBody(
                "{\"success\":true,\"data\":{\"results\":[" +
                    "{\"id\":\"s1\",\"name\":\"Song &amp; Co\",\"playCount\":1500," +
                    "\"image\":[{\"url\":\"a\"},{\"url\":\"b\"}]," +
                    "\"artists\":{\"primary\":[{\"id\":\"a1\",\"name\":\"Singer\"}]}" +
                    "}]}}"
            )
        )
        try {
            repository(server).searchSongs("song").test {
                val songs = awaitItem()
                assertEquals(1, songs.size)
                assertEquals("Song & Co", songs[0].name)
                assertEquals("Singer", songs[0].artist)
                assertEquals(1500L, songs[0].playCount)
                awaitComplete()
            }
        } finally {
            server.shutdown()
        }
    }
}
