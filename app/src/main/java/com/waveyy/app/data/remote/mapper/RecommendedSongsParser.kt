package com.waveyy.app.data.remote.mapper

import com.waveyy.app.data.remote.dto.SongDto
import com.waveyy.app.domain.model.Song
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Parses the `recommendedSongs` proxy payload, whose envelope varies by
 * upstream (bare list, `{suggestions: [...]}`, `{results: [...]}`, or
 * `{songs: [...]}`). Mirrors the multi-target fallback in the web route.
 */
object RecommendedSongsParser {

    private val json = Json { ignoreUnknownKeys = true }

    /** Extracts playable songs from any known envelope shape. */
    fun parse(payload: kotlinx.serialization.json.JsonElement): List<Song> {
        val dtos = parseDtos(payload)
        return SongMapper.toDomainList(dtos)
    }

    private fun parseDtos(payload: kotlinx.serialization.json.JsonElement): List<SongDto> {
        return runCatching {
            when (payload) {
                is JsonArray -> json.decodeFromJsonElement<List<SongDto>>(payload)
                is JsonObject -> {
                    val list = payload["suggestions"] ?: payload["results"] ?: payload["songs"]
                    val array = list as? JsonArray ?: return emptyList()
                    json.decodeFromJsonElement<List<SongDto>>(array)
                }
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
    }
}
