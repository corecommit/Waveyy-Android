package com.waveyy.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.waveyy.app.domain.model.AudioStream

/** Unit tests for [AudioQualitySelector]. */
class AudioQualitySelectorTest {

    private fun stream(url: String, quality: String = "") =
        AudioStream(url = url, quality = quality, bitrateKbps = 0, lossless = false)

    @Test
    fun `empty list returns null`() {
        assertNull(AudioQualitySelector.selectBest(emptyList()))
        assertEquals("", AudioQualitySelector.selectBestUrl(emptyList()))
    }

    @Test
    fun `picks highest bitrate from quality labels`() {
        val streams = listOf(
            stream("https://x/96.mp3", "96kbps"),
            stream("https://x/320.mp3", "320kbps"),
            stream("https://x/160.mp3", "160kbps")
        )
        assertEquals("https://x/320.mp3", AudioQualitySelector.selectBestUrl(streams))
    }

    @Test
    fun `lossless outranks higher bitrate lossy`() {
        val streams = listOf(
            stream("https://x/320.mp3", "320kbps"),
            stream("https://x/song.flac", "FLAC")
        )
        assertEquals("https://x/song.flac", AudioQualitySelector.selectBestUrl(streams))
    }

    @Test
    fun `parses bitrate from url suffix`() {
        val streams = listOf(
            stream("https://x/song_96.mp3"),
            stream("https://x/song_160.mp3")
        )
        assertEquals("https://x/song_160.mp3", AudioQualitySelector.selectBestUrl(streams))
    }

    @Test
    fun `skips blank urls`() {
        val streams = listOf(
            stream(""),
            stream("https://x/only.mp3", "128kbps")
        )
        assertEquals("https://x/only.mp3", AudioQualitySelector.selectBestUrl(streams))
    }
}
