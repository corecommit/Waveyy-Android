package com.waveyy.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [LyricsParser]. */
class LyricsParserTest {

    @Test
    fun `parses centisecond timestamps`() {
        val lines = LyricsParser.parseLrc("[00:12.34]Hello")
        assertEquals(1, lines.size)
        assertEquals(12_340L, lines[0].timeMs)
        assertEquals("Hello", lines[0].text)
    }

    @Test
    fun `expands multiple timestamps on one line and sorts`() {
        val lines = LyricsParser.parseLrc("[00:20.00][00:10.00]Again")
        assertEquals(2, lines.size)
        assertEquals(10_000L, lines[0].timeMs)
        assertEquals(20_000L, lines[1].timeMs)
    }

    @Test
    fun `drops lines without valid timestamps`() {
        val lines = LyricsParser.parseLrc("no timestamp here\n[99]bad\n[00:05.00]Good")
        assertEquals(1, lines.size)
        assertEquals("Good", lines[0].text)
    }

    @Test
    fun `sanitize applies transliteration fixes`() {
        assertEquals("aashiki bhi mera", LyricsParser.sanitizeText("aashikli bhil meraa"))
    }

    @Test
    fun `empty input parses to empty`() {
        assertTrue(LyricsParser.parseLrc("").isEmpty())
    }
}
