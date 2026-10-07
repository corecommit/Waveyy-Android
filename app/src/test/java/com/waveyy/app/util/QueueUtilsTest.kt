package com.waveyy.app.util

import com.waveyy.app.data.remote.dto.SaavnArtistRef
import com.waveyy.app.data.remote.dto.SaavnArtists
import com.waveyy.app.data.remote.dto.SaavnImage
import com.waveyy.app.data.remote.dto.SongDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for [QueueUtils]. */
class QueueUtilsTest {

    private fun dto() = SongDto(
        id = "s1",
        name = "Song",
        image = listOf(SaavnImage("a"), SaavnImage("b"), SaavnImage("c")),
        artists = SaavnArtists(listOf(SaavnArtistRef("a1", "Singer"))),
        playCount = 42L
    )

    @Test
    fun `normalize keeps canonical fields`() {
        val song = QueueUtils.normalize(dto())!!
        assertEquals("s1", song.id)
        assertEquals("Song", song.name)
        assertEquals("Singer", song.artist)
        assertEquals("c", song.imageUrl)
    }

    @Test
    fun `normalize rejects id-less payloads`() {
        assertNull(QueueUtils.normalize(dto().copy(id = "")))
    }

    @Test
    fun `toNextUp maps handoff shape`() {
        val next = QueueUtils.toNextUp(QueueUtils.normalize(dto())!!)
        assertEquals("s1", next.id)
        assertEquals("Singer", next.artist)
    }
}
