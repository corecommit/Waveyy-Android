package com.waveyy.app.data.remote.ytmusic

import com.waveyy.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal first-party InnerTube transport: POSTs to
 * `music.youtube.com/youtubei/v1` with the `WEB_REMIX` client context.
 * Bot-guard and consent failures return null so callers fall back — never
 * throws for API-shape reasons (only transport IO is wrapped in runCatching
 * by callers).
 *
 * No third-party InnerTube library is used (see Phase 16 research:
 * ytm-kt is archived, ytmusic-engine-kotlin has no auth story, SimpMusic
 * is GPL-3.0).
 */
@Singleton
class InnerTubeClient @Inject constructor(
    private val okHttp: OkHttpClient,
    private val json: Json
) {

    /**
     * POSTs to an InnerTube [endpoint] (`search`, `player`, `next`) with
     * [payload] merged into the client context. Returns the raw response
     * object, or null on transport failure.
     */
    suspend fun post(
        endpoint: String,
        payload: JsonObject,
        visitorData: String?
    ): JsonObject? = withContext(Dispatchers.IO) {
        runCatching {
            val body = buildJsonObject {
                put("context", clientContext(visitorData))
                payload.forEach { (key, value) -> put(key, value) }
            }.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url("$BASE_URL/$endpoint")
                .header("X-Goog-Api-Key", API_KEY)
                .header("X-YouTube-Client-Name", CLIENT_ID)
                .header("X-YouTube-Client-Version", CLIENT_VERSION)
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .header("Origin", "https://music.youtube.com")
                .header("Accept-Language", "en-US,en;q=0.9")
                .post(body)
                .build()
            okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val text = response.body?.string() ?: return@runCatching null
                json.parseToJsonElement(text).jsonObject
            }
        }.onFailure {
            Timber.d("InnerTube $endpoint failed: ${it.message}")
        }.getOrNull()
    }

    private fun clientContext(visitorData: String?) = buildJsonObject {
        put("client", buildJsonObject {
            put("clientName", CLIENT_NAME)
            put("clientVersion", CLIENT_VERSION)
            put("hl", "en")
            put("gl", "US")
            if (!visitorData.isNullOrBlank()) put("visitorData", JsonPrimitive(visitorData))
        })
    }

    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"

        // API key + client version aligned with the WEB_REMIX client the
        // yt-dlp and SimpMusic projects ship as of 2026-10. Google rotates
        // these periodically; if 401s return, bump CLIENT_VERSION first.
        private const val API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        private const val CLIENT_ID = "67"
        private const val CLIENT_VERSION = "1.20260304.03.00"
        private const val CLIENT_NAME = "WEB_REMIX"

        // Desktop Chrome UA — matches what the WEB_REMIX client advertises
        // and avoids the "mobile app but no device fingerprint" mismatch
        // that gets requests softly-flagged.
        private const val USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/140.0.0.0 Safari/537.36"

        private const val REFERER = "https://music.youtube.com/"

        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
