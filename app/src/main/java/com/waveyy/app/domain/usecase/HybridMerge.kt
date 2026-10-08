package com.waveyy.app.domain.usecase

import com.waveyy.app.domain.model.MusicSource
import com.waveyy.app.domain.model.Song

/**
 * Hybrid merge: JioSaavn results first, YouTube Music filling the remainder.
 * Dedupes case-insensitively on title+artist, caps at 30, and sorts by
 * title similarity to the query (edit-distance based, reusing
 * [SearchRanking]).
 */
object HybridMerge {

    /** Maximum merged results. */
    const val LIMIT = 30

    /**
     * Merges [jiosaavn] and [ytmusic] into one ranked list.
     */
    fun merge(query: String, jiosaavn: List<Song>, ytmusic: List<Song>): List<Song> {
        val seen = mutableSetOf<String>()
        val out = mutableListOf<Song>()
        for (song in jiosaavn + ytmusic) {
            if (out.size >= LIMIT) break
            val key = (song.name.lowercase() + "||" + song.artist.lowercase())
            if (!seen.add(key)) continue
            out += song
        }
        // JioSaavn order is already relevance-ranked; YT fill keeps response
        // order. A light similarity pass keeps near-matches on top without
        // reordering exact hits.
        return out.sortedWith(
            compareByDescending<Song> { SearchRanking.similarity(it.name, query) }
                .thenBy { if (it.source == MusicSource.JIOSAAVN) 0 else 1 }
        )
    }
}
