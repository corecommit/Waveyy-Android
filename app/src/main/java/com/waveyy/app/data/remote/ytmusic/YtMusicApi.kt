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
     * Searches YouTube Music. [params] is an optional base64-encoded
     * protobuf that filters results by category (see [YtSearchFilter]).
     * When null, YT returns a mixed result set (songs + albums + artists
     * + playlists + podcasts interleaved).
     *
     * Response path (WEB_REMIX, when params present):
     *   contents.tabbedSearchResultsRenderer
     *     .tabs[0].tabRenderer.content
     *     .sectionListRenderer.contents.last()
     *     .musicShelfRenderer.contents[]
     *
     * Each item is a musicResponsiveListItemRenderer whose flexColumns
     * follow the `title` / `secondaryLine` split — [parseTrack] handles
     * the separator logic.
     *
     * @param query user search text.
     * @param visitorData optional stored visitor token.
     * @param params filter params (see [YtSearchFilter]); null for unfiltered.
     */
    suspend fun search(
        query: String,
        visitorData: String?,
        params: String? = null
    ): YtSearchResult {
        val body = buildJsonObject {
            put("query", JsonPrimitive(query))
            if (!params.isNullOrBlank()) {
                put("params", JsonPrimitive(params))
            }
        }
        val response = transport.post("search", body, visitorData)
            ?: return YtSearchResult(emptyList(), null)
        val tracks = mutableListOf<YtTrack>()
        for (item in searchItems(response)) {
            parseTrack(item)?.let(tracks::add)
        }
        return YtSearchResult(tracks, response.visitorToken())
    }

    /**
     * Fetches the YouTube Music radio queue that plays after [videoId].
     * Response shape (WEB_REMIX):
     *   contents.singleColumnMusicWatchNextResultsRenderer
     *     .tabbedRenderer.watchNextTabbedResultsRenderer
     *     .tabs[0].tabRenderer.content.musicQueueRenderer.content
     *     .playlistPanelRenderer.contents[]
     *
     * Each item is a playlistPanelVideoRenderer with videoId, title runs,
     * artist byline runs, and thumbnails. Returned in YT's natural
     * relevance order — the first N are what YouTube itself would autoplay.
     *
     * @param videoId seed video id (e.g. "dQw4w9WgXcQ").
     * @return ordered list of tracks, or empty when the request fails or
     *   the response is malformed.
     */
    suspend fun nextRadio(videoId: String, visitorData: String?): List<YtTrack> {
        val body = buildJsonObject {
            put("videoId", JsonPrimitive(videoId))
            put("isAudioOnly", JsonPrimitive(true))
            put("enablePersistentPlaylistPanel", JsonPrimitive(true))
        }
        val response = transport.post("next", body, visitorData)
            ?: return emptyList()
        val tabs = response.optObj("contents")
            ?.optObj("singleColumnMusicWatchNextResultsRenderer")
            ?.optObj("tabbedRenderer")
            ?.optObj("watchNextTabbedResultsRenderer")
            ?.optArr("tabs").orEmpty()
        val firstTabContent = (tabs.firstOrNull() as? JsonObject)
            ?.optObj("tabRenderer")
            ?.optObj("content")
            ?: return emptyList()
        val panelContents = firstTabContent
            .optObj("musicQueueRenderer")
            ?.optObj("content")
            ?.optObj("playlistPanelRenderer")
            ?.optArr("contents").orEmpty()
        return panelContents.mapNotNull { element ->
            val obj = element as? JsonObject ?: return@mapNotNull null
            val renderer = obj.optObj("playlistPanelVideoRenderer")
                ?: return@mapNotNull null
            parsePanelVideo(renderer)
        }
    }

    /**
     * Parses one playlistPanelVideoRenderer into a YtTrack.
     * Fields: videoId, title.runs[], longBylineText.runs[], lengthText.
     */
    private fun parsePanelVideo(renderer: JsonObject): YtTrack? {
        val videoId = renderer.optStr("videoId")?.takeIf { it.isNotBlank() }
            ?: return null
        val titleRuns = renderer.optObj("title")?.optArr("runs").orEmpty()
        val title = titleRuns.mapNotNull { (it as? JsonObject)?.optStr("text") }
            .joinToString("").takeIf { it.isNotBlank() } ?: return null
        // longBylineText has "Artist • Album • Year" in separate runs.
        // Take the first run's text — that's the artist name.
        val bylineRuns = renderer.optObj("longBylineText")?.optArr("runs").orEmpty()
        val artist = bylineRuns.firstOrNull()
            ?.let { (it as? JsonObject)?.optStr("text") }
            ?.takeIf { it.isNotBlank() } ?: "unknown"
        // lengthText is "3:41" — parse into seconds.
        val lengthText = renderer.optObj("lengthText")?.optStr("simpleText")
        val durationSec = lengthText?.let(::parseDuration) ?: 0L
        val thumbnails = renderer.optObj("thumbnail")?.optObj("musicThumbnailRenderer")
            ?.optObj("thumbnail")?.optArr("thumbnails").orEmpty()
            .mapNotNull { (it as? JsonObject)?.optStr("url") }
        return YtTrack(
            videoId = videoId,
            title = title,
            artist = artist,
            durationSec = durationSec,
            thumbnailUrl = thumbnails.lastOrNull().orEmpty()
        )
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

    private fun searchItems(response: JsonObject): List<JsonObject> {
        val tabs = response.optObj("contents")
            ?.optObj("tabbedSearchResultsRenderer")
            ?.optArr("tabs").orEmpty()
        val tab = tabs.firstNotNullOfOrNull {
            (it as? JsonObject)?.optObj("tabRenderer")
        } ?: return emptyList()
        val sections = tab.optObj("content")
            ?.optObj("sectionListRenderer")
            ?.optArr("contents").orEmpty()
            .mapNotNull { it as? JsonObject }
        val shelf = sections.lastOrNull()?.let { section ->
            section.optObj("musicShelfRenderer")
                ?: section.optObj("musicCardShelfRenderer")
        } ?: return emptyList()
        return (shelf["contents"] as? JsonArray).orEmpty().mapNotNull { entry ->
            ((entry as? JsonObject)?.get("musicResponsiveListItemRenderer") as? JsonObject)
        }
    }

    /**
     * Parses one musicResponsiveListItemRenderer. Reads:
     *   flexColumns[0] — title runs (first run has the videoId).
     *   flexColumns[1] — the "secondary line" of runs. YT splits this
     *     into segments separated by runs whose text is " • " (or "·").
     *     Segment 0: artists (each is a nav-link run).
     *     Segment 1: album (usually one nav-link run).
     *     Segment last: duration ("3:41").
     *
     * Mirrors SimpMusic's `secondaryLine.splitBySeparator()` pattern.
     */
    private fun parseTrack(item: JsonObject): YtTrack? {
        val columns = item.optArr("flexColumns").orEmpty()
            .mapNotNull { (it as? JsonObject)?.optObj("musicResponsiveListItemFlexColumnRenderer") }
        if (columns.isEmpty()) return null

        // Title + videoId from the first flex column.
        val titleRuns = columns.firstOrNull()?.runs().orEmpty()
        val titleRun = titleRuns.firstOrNull() ?: return null
        val title = titleRun.optStr("text")?.takeIf { it.isNotBlank() } ?: return null
        val videoId = titleRun.optObj("navigationEndpoint")
            ?.optObj("watchEndpoint")
            ?.optStr("videoId")?.takeIf { it.isNotBlank() } ?: return null

        // Secondary line: split runs into segments by separator.
        val secondaryRuns = columns.getOrNull(1)?.runs().orEmpty()
        val segments = splitBySeparator(secondaryRuns)

        // Artists: first segment, keep only runs with a UC browse endpoint
        // (skips any non-artist nav links that sneak in).
        val artist = segments.firstOrNull()
            ?.firstOrNull { run ->
                run.optObj("navigationEndpoint")
                    ?.optObj("browseEndpoint")
                    ?.optStr("browseId")?.startsWith("UC") == true
            }
            ?.optStr("text")
            ?.takeIf { it.isNotBlank() }
            ?: segments.firstOrNull()?.firstOrNull()?.optStr("text")
            ?: "unknown"

        // Duration: scan every segment's last run for "M:SS" or "H:MM:SS".
        val durationSec = segments.asSequence()
            .flatMap { seg -> seg.mapNotNull { it.optStr("text") } }
            .firstOrNull { DURATION.matches(it) }
            ?.let(::parseDuration) ?: 0L

        // Thumbnail.
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

    /**
     * Splits runs into segments separated by runs whose text is a lone
     * " • " or "·". YT's flex-column renderer emits the separator as
     * its own run between artist / album / duration groups.
     */
    private fun splitBySeparator(runs: List<JsonObject>): List<List<JsonObject>> {
        val out = mutableListOf<MutableList<JsonObject>>()
        var current = mutableListOf<JsonObject>()
        for (run in runs) {
            val text = run.optStr("text").orEmpty().trim()
            val isSeparator = text == "•" || text == "·" || text == "|"
            if (isSeparator) {
                if (current.isNotEmpty()) {
                    out += current
                    current = mutableListOf()
                }
            } else {
                current += run
            }
        }
        if (current.isNotEmpty()) out += current
        return out
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

/** Search filter params for YT Music's `search` endpoint. */
object YtSearchFilter {
    /** Songs only. */
    const val SONG = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
    /** Music videos only. */
    const val VIDEO = "EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D"
    /** Albums only. */
    const val ALBUM = "EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D"
    /** Artists only. */
    const val ARTIST = "EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D"
    /** Community playlists only. */
    const val PLAYLIST = "EgeKAQQoAEABagoQAxAEEAoQCRAF"
}
