package com.waveyy.app.domain.usecase

import com.waveyy.app.domain.model.AudioStream
import com.waveyy.app.domain.model.MusicSource
import com.waveyy.app.domain.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [HybridMerge]. */
class HybridMergeTest {

    private fun js(
        id: String,
        name: String = "Song $id",
        artist: String = "Singer"
    ) = Song(
        id = id,
        name = name,
        artist = artist,
        album = "unknown",
        imageUrl = "",
        durationSec = 0L,
        playCount = 0L,
        streams = emptyList(),
        source = MusicSource.JIOSAAVN
    )

    private fun yt(
        id: String,
        name: String = "Song $id",
        artist: String = "Singer"
    ) = js(id, name, artist).copy(
        id = "yt:$id",
        source = MusicSource.YTMUSIC
    )

    @Test
    fun `dedupes case-insensitively across sources`() {
        val merged = HybridMerge.merge(
            "runaway",
            listOf(js("a", name = "Runaway", artist = "Aurora")),
            listOf(yt("b", name = "RUNAWAY", artist = "aurora"))
        )
        assertEquals(1, merged.size)
        assertEquals(MusicSource.JIOSAAVN, merged.first().source)
    }

    @Test
    fun `caps at thirty`() {
        val js = (1..25).map { js("j$it", name = "Song $it") }
        val yt = (1..25).map { yt("y$it", name = "Track $it") }
        assertEquals(30, HybridMerge.merge("x", js, yt).size)
    }

    @Test
    fun `jiosaavn first on ties`() {
        val merged = HybridMerge.merge(
            "abc",
            listOf(js("a", name = "abc")),
            listOf(yt("b", name = "abc"))
        )
        assertEquals(MusicSource.JIOSAAVN, merged.first().source)
    }

    @Test
    fun `ranks near matches above distant ones`() {
        val merged = HybridMerge.merge(
            "runaway",
            listOf(js("a", name = "Unrelated Symphony")),
            listOf(yt("b", name = "Runaway"))
        )
        assertEquals("yt:b", merged.first().id)
        assertTrue(merged.any { it.id == "a" })
    }
}
