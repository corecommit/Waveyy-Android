package com.waveyy.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [Hinglish]. */
class HinglishTest {

    @Test
    fun `non-devanagari passes through`() {
        assertEquals("hello world", Hinglish.toHinglish("hello world"))
        assertEquals("", Hinglish.toHinglish(""))
    }

    @Test
    fun `transliterates namaste with schwa deletion`() {
        assertEquals("namaste", Hinglish.toHinglish("नमस्ते"))
    }

    @Test
    fun `halant cluster drops the vowel`() {
        assertEquals("snaan", Hinglish.toHinglish("स्नान"))
    }

    @Test
    fun `matra attaches to consonant`() {
        assertEquals("giit", Hinglish.toHinglish("गीत"))
    }

    @Test
    fun `independent vowels map directly`() {
        assertEquals("aaiimaa", Hinglish.toHinglish("आईमा"))
    }
}
