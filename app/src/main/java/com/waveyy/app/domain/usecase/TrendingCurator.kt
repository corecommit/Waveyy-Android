package com.waveyy.app.domain.usecase

import com.waveyy.app.data.remote.dto.SongDto
import java.text.Normalizer

/**
 * Trending curation pipeline ported verbatim from `app/(root)/page.js`:
 * blocklists, synthetic-track detection, and play-count-ranked curation
 * with title deduplication.
 */
object TrendingCurator {

    /** Five-day window (ms) for recent-history relevance. */
    const val FIVE_DAYS_MILLIS = 5L * 24L * 60L * 60L * 1000L

    /** Artist-name patterns that mark compilation/chart filler. */
    val artistBlocklist = listOf(
        Regex("top \\d+", RegexOption.IGNORE_CASE),
        Regex("top hit music charts", RegexOption.IGNORE_CASE),
        Regex("todays hits", RegexOption.IGNORE_CASE),
        Regex("chart hits allstars", RegexOption.IGNORE_CASE),
        Regex("summer hit superstars", RegexOption.IGNORE_CASE),
        Regex("dance hits \\d+", RegexOption.IGNORE_CASE),
        Regex("pop tracks", RegexOption.IGNORE_CASE)
    )

    /** Album-name patterns that mark compilation/chart filler. */
    val albumBlocklist = listOf(
        Regex("big chart", RegexOption.IGNORE_CASE),
        Regex("massive chart", RegexOption.IGNORE_CASE),
        Regex("vital pop", RegexOption.IGNORE_CASE),
        Regex("ultimate noughties", RegexOption.IGNORE_CASE),
        Regex("top chart", RegexOption.IGNORE_CASE),
        Regex("chart tunes", RegexOption.IGNORE_CASE),
        Regex("superstars", RegexOption.IGNORE_CASE),
        Regex("monsoon bollywood hits", RegexOption.IGNORE_CASE),
        Regex("bollywood party hits", RegexOption.IGNORE_CASE),
        Regex("essentials", RegexOption.IGNORE_CASE)
    )

    /** Title patterns that mark low-quality variants. */
    val titleBlocklist = listOf(
        Regex("trending version", RegexOption.IGNORE_CASE),
        Regex("slowed", RegexOption.IGNORE_CASE),
        Regex("reverb", RegexOption.IGNORE_CASE),
        Regex("nightcore", RegexOption.IGNORE_CASE),
        Regex("8d", RegexOption.IGNORE_CASE)
    )

    /** Lowercase, single-spaced tag normalization. */
    fun normalizeTag(value: String?): String {
        return (value ?: "").trim().lowercase().replace(Regex("\\s+"), " ")
    }

    /** Title-cases a normalized tag for display. */
    fun formatTag(value: String): String {
        return value.split(" ").filter { it.isNotEmpty() }.joinToString(" ") { word ->
            (word[0].uppercaseChar() + word.drop(1))
        }
    }

    /** Genre/language tags for recommendation seeding. */
    fun genreTags(dto: SongDto): Set<String> {
        val raw = mutableListOf<String>()
        raw += dto.genres
        dto.genre?.split(Regex("[|,]"))?.let { raw += it }
        dto.language?.let { raw += it }
        dto.album?.language?.let { raw += it }
        return raw.map(::normalizeTag).filter { it.isNotEmpty() }.toSet()
    }

    /** Normalized `title||artist` identity key. */
    fun songKey(dto: SongDto): String {
        val title = normalizeText(dto.name ?: dto.title ?: dto.song ?: "")
        val artist = normalizeText(
            dto.artists?.primary.orEmpty().mapNotNull { it.name }.joinToString(" ")
        )
        return "$title||$artist"
    }

    /** Primary artist names. */
    fun primaryArtistNames(dto: SongDto): List<String> {
        return dto.artists?.primary.orEmpty().mapNotNull { it.name }
    }

    /**
     * True for chart filler: missing core fields, blocklisted title/artist/
     * album, or no usable artist.
     */
    fun isSynthetic(dto: SongDto?): Boolean {
        if (dto == null) return true
        val artistBlob = normalizeText(primaryArtistNames(dto).joinToString(" "))
        val albumName = normalizeText(dto.album?.name ?: "")
        val title = normalizeText(dto.name ?: "")
        if (dto.id.isBlank() || dto.name.isNullOrBlank() || dto.image.isEmpty()) return true
        if (artistBlob.isBlank()) return true
        if (titleBlocklist.any { it.containsMatchIn(title) }) return true
        if (artistBlocklist.any { it.containsMatchIn(artistBlob) }) return true
        if (albumBlocklist.any { it.containsMatchIn(albumName) }) return true
        return false
    }

    /**
     * Ranks by play count, drops synthetic/duplicates, caps at [limit].
     *
     * @return pair of (selected songs, updated track-key set).
     */
    fun curate(
        songs: List<SongDto>,
        limit: Int = 20,
        existingTrackKeys: Set<String> = emptySet()
    ): Pair<List<SongDto>, Set<String>> {
        val selected = mutableListOf<SongDto>()
        val seenIds = mutableSetOf<String>()
        val seenTracks = existingTrackKeys.toMutableSet()
        val ranked = songs.sortedByDescending { maxOf(it.playCount, it.playCountSnake) }
        for (song in ranked) {
            if (song.id.isBlank() || !seenIds.add(song.id)) continue
            if (isSynthetic(song)) continue
            val trackKey = normalizeText(song.name ?: "")
            if (trackKey in seenTracks) continue
            seenTracks.add(trackKey)
            selected += song
            if (selected.size >= limit) break
        }
        return selected to seenTracks
    }

    private fun normalizeText(value: String): String {
        return Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
