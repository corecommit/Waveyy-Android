package com.waveyy.app.domain.usecase

import com.waveyy.app.data.remote.dto.SaavnArtists
import com.waveyy.app.data.remote.dto.SaavnArtistRef
import com.waveyy.app.data.remote.dto.SaavnImage
import com.waveyy.app.data.remote.dto.SongDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [TrendingCurator]. */
class TrendingCuratorTest {

    private fun song(
        id: String,
        name: String = "Song $id",
        artist: String = "Singer",
        album: String = "Album",
        playCount: Long = 100L
    ) = SongDto(
        id = id,
        name = name,
        image = listOf(SaavnImage("img")),
        artists = SaavnArtists(listOf(SaavnArtistRef("a", artist))),
        album = com.waveyy.app.data.remote.dto.SaavnAlbumRef(name = album),
        playCount = playCount
    )

    @Test
    fun `ranks by play count descending`() {
        val (selected, _) = TrendingCurator.curate(
            listOf(song("lo", playCount = 10L), song("hi", playCount = 999L))
        )
        assertEquals(listOf("hi", "lo"), selected.map { it.id })
    }

    @Test
    fun `drops compilation filler by blocklist`() {
        val filler = song("x", artist = "Top 100 Hits 2024")
        val (selected, _) = TrendingCurator.curate(listOf(filler, song("ok")))
        assertEquals(listOf("ok"), selected.map { it.id })
    }

    @Test
    fun `drops slowed and reverb variants`() {
        val slowed = song("x", name = "Song slowed")
        assertTrue(TrendingCurator.isSynthetic(slowed))
    }

    @Test
    fun `requires id name image and artist`() {
        assertTrue(TrendingCurator.isSynthetic(song("x", name = "")))
        assertFalse(TrendingCurator.isSynthetic(song("x")))
    }

    @Test
    fun `dedupes repeat titles`() {
        val (selected, _) = TrendingCurator.curate(
            listOf(song("a", name = "Same"), song("b", name = "Same"))
        )
        assertEquals(1, selected.size)
    }

    @Test
    fun `respects limit and existing keys`() {
        val songs = (1..30).map { song("s$it") }
        val (selected, _) = TrendingCurator.curate(songs, limit = 20)
        assertEquals(20, selected.size)
    }

    @Test
    fun `song keys combine normalized title and artist`() {
        val key = TrendingCurator.songKey(song("a", name = "Hello!", artist = "Adele"))
        assertEquals("hello||adele", key)
    }
}
