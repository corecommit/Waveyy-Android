package com.waveyy.app.util

import com.waveyy.app.data.remote.dto.SongDto
import com.waveyy.app.data.remote.mapper.SongMapper
import com.waveyy.app.domain.model.Song

/** Queue-ready song handed to the up-next slot. */
data class NextUpSong(
    val id: String,
    val name: String,
    val artist: String,
    val album: String,
    val image: String
)

/**
 * Queue normalization helpers. Ports `lib/queue-utils.js`: raw payloads are
 * reduced to canonical [Song] items, and [NextUpSong] carries the up-next
 * handoff (mirrors `mapQueueSongToNextData`).
 */
object QueueUtils {

    /** Normalizes a DTO into a queue [Song]; null when it has no id. */
    fun normalize(dto: SongDto): Song? {
        val song = SongMapper.toDomain(dto) ?: return null
        return song.copy(
            image = song.imageUrl,
            album = song.album
        )
    }

    /** Maps a queue song to the up-next handoff shape. */
    fun toNextUp(song: Song): NextUpSong {
        return NextUpSong(
            id = song.id,
            name = song.name,
            artist = song.artist,
            album = song.album,
            image = song.imageUrl
        )
    }
}
