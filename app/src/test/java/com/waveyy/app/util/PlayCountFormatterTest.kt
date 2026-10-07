package com.waveyy.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [PlayCountFormatter]. */
class PlayCountFormatterTest {

    @Test
    fun `formats bands`() {
        assertEquals("0", PlayCountFormatter.format(0L))
        assertEquals("999", PlayCountFormatter.format(999L))
        assertEquals("1.5K", PlayCountFormatter.format(1500L))
        assertEquals("2M", PlayCountFormatter.format(2_000_000L))
        assertEquals("1.1B", PlayCountFormatter.format(1_100_000_000L))
    }
}
