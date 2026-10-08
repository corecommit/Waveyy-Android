package com.waveyy.app.data.remote.ytmusic

import com.waveyy.app.data.remote.ytmusic.dto.YtStream
import com.waveyy.app.data.remote.ytmusic.dto.YtTrack
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Endpoint layer over [InnerTubeClient]: track search and stream
 * resolution. Parsing is defensive (missing keys yield skips, never
 * crashes); bot-guard responses (`UNPLAYABLE`, consent screens) yield
 * null/empty so the hybrid layer falls back to JioSaavn.
 */
@Singleton
class YtMusicApi @Inject constructor(
    private val transport: InnerTubeClient
) {

    /**
     * Searches tracks. Returns parsed tracks in response order.
     *
     * @param query search text.
     * @param visitorData optional stored visitor token.
     */
    suspend fun search(query: String, visitorData: String?): YtSearchResult {
        val body = buildJsonObject {
            put("query", JsonPrimitive(query))
        }
        val response = transport.post("search", body, visitorData) ?: return YtSearchResult(emptyList(), null)
        val tracks = mutableListOf<YtTrack>()
        for (section in searchSections(response)) {
            for (item in shelfItems(section)) {
                parseTrack(item)?.let(tracks::add)
            }
        }
        return YtSearchResult(tracks, response.visitorToken())
    }

    /**
     * Resolves a playable audio stream URL, or null when the track is
     * gated (login/bot-check) or parsing fails.
     */
    suspend fun streamUrl(videoId: String, visitorData: String?): YtStream? {
        val body = buildJsonObject {
            put("videoId", JsonPrimitive(videoId))
        }
        val response = transport.post("player", body, visitorData) ?: return null
        val status = response.optObj("playabilityStatus")
        if (status?.optStr("status") != "OK") {
            Timber.d("YtMusic player not OK: ${status?.optStr("status")} ${status?.optStr("reason")}")
            return null
        }
        val streaming = response.optObj("streamingData") ?: return null
        val candidates = (streaming.optArr("formats") ?: emptyList()) +
            (streaming.optArr("adaptiveFormats") ?: emptyList())
        return candidates
            .mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val url = obj.optStr("url")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val mime = obj.optStr("mimeType").orEmpty()
                if (!mime.startsWith("audio/")) return@mapNotNull null
                YtStream(url = url, bitrate = obj.optLong("bitrate"), mimeType = mime)
            }
            .maxByOrNull { it.bitrate }
    }

    /**
     * Fetches track metadata for [videoId] (title/artist/thumbnail from the
     * player response's videoDetails).
     */
    suspend fun trackMetadata(videoId: String, visitorData: String?): YtTrack? {
        val body = buildJsonObject {
            put("videoId", JsonPrimitive(videoId))
        }
        val response = transport.post("player", body, visitorData) ?: return null
        val details = response.optObj("videoDetails") ?: return null
        val title = details.optStr("title")?.takeIf { it.isNotBlank() } ?: return null
        val thumbnails = details.optObj("thumbnail")
            ?.optArr("thumbnails").orEmpty()
            .mapNotNull { (it as? JsonObject)?.optStr("url") }
        return YtTrack(
            videoId = videoId,
            title = title,
            artist = details.optStr("author") ?: "unknown",
            durationSec = details.optStr("lengthSeconds")?.toLongOrNull() ?: 0L,
            thumbnailUrl = thumbnails.lastOrNull().orEmpty()
        )
    }

    private fun searchSections(response: JsonObject): List<JsonObject> {
        val tabs = response.optObj("contents")
            ?.optObj("tabbedSearchResultsRenderer")
            ?.optArr("tabs").orEmpty()
        val tab = tabs.firstNotNullOfOrNull { (it as? JsonObject)?.optObj("tabRenderer") } ?: return emptyList()
        return tab.optObj("content")
            ?.optObj("sectionListRenderer")
            ?.optArr("contents").orEmpty()
            .mapNotNull { it as? JsonObject }
    }

    private fun shelfItems(section: JsonObject): List<JsonObject> {
        val shelf = section["musicShelfRenderer"] as? JsonObject
            ?: section["musicCardShelfRenderer"] as? JsonObject
            ?: return emptyList()
        return (shelf["contents"] as? JsonArray).orEmpty().mapNotNull { entry ->
            ((entry as? JsonObject)?.get("musicResponsiveListItemRenderer") as? JsonObject)
        }
    }

    private fun parseTrack(item: JsonObject): YtTrack? {
        val columns = item.optArr("flexColumns").orEmpty()
            .mapNotNull { (it as? JsonObject)?.optObj("musicResponsiveListItemFlexColumnRenderer") }
        if (columns.isEmpty()) return null
        val titleRuns = columns.firstOrNull()?.runs().orEmpty()
        val titleRun = titleRuns.firstOrNull() ?: return null
        val title = titleRun.optStr("text")?.takeIf { it.isNotBlank() } ?: return null
        val videoId = titleRun.optObj("navigationEndpoint")
            ?.optObj("watchEndpoint")
            ?.optStr("videoId")?.takeIf { it.isNotBlank() } ?: return null
        val artistRuns = columns.drop(1).flatMap { it.runs() }
        val artist = artistRuns.firstOrNull { run ->
            run.optObj("navigationEndpoint")?.optObj("browseEndpoint")
                ?.optStr("browseId")?.startsWith("UC") == true
        }?.optStr("text") ?: artistRuns.firstOrNull()?.optStr("text") ?: "unknown"
        val durationSec = columns.flatMap { it.runs() }
            .mapNotNull { it.optStr("text") }
            .firstOrNull { DURATION.matches(it) }
            ?.let(::parseDuration) ?: 0L
        val thumbnails = item.optObj("thumbnailRenderer")
            ?.optObj("musicThumbnailRenderer")
            ?.optObj("thumbnail")
            ?.optArr("thumbnails").orEmpty()
            .mapNotNull { (it as? JsonObject)?.optStr("url") }
        return YtTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            durationSec = durationSec,
            thumbnailUrl = thumbnails.lastOrNull().orEmpty()
        )
    }

    private fun JsonObject.runs(): List<JsonObject> {
        return (optObj("text")?.optArr("runs")).orEmpty().mapNotNull { it as? JsonObject }
    }

    private fun parseDuration(text: String): Long {
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        if (parts.isEmpty()) return 0L
        return parts.fold(0L) { acc, part -> acc * 60L + part }
    }

    companion object {
        private val DURATION = Regex("^\\d{1,3}:\\d{2}(?::\\d{2})?\$")
    }
}

/** Search outcome plus a refreshed visitor token (if the server sent one). */
data class YtSearchResult(
    val tracks: List<YtTrack>,
    val visitorData: String?
)

private fun JsonObject.optObj(key: String): JsonObject? = this[key] as? JsonObject

private fun JsonObject.optArr(key: String): List<JsonElement>? =
    (this[key] as? JsonArray)?.toList()

private fun JsonObject.optStr(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.optLong(key: String): Long =
    (this[key] as? JsonPrimitive)?.longOrNull
        ?: (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
        ?: 0L

private fun JsonObject.visitorToken(): String? =
    optObj("responseContext")?.optStr("visitorData")?.takeIf { it.isNotBlank() }
