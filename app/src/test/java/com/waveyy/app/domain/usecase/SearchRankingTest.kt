package com.waveyy.app.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [SearchRanking] pure functions. */
class SearchRankingTest {

    @Test
    fun `normalize strips diacritics and punctuation`() {
        assertEquals("beyonce hello", SearchRanking.normalizeText("Beyoncé, Hello!"))
        assertEquals("", SearchRanking.normalizeText(""))
    }

    @Test
    fun `edit distance matches textbook examples`() {
        assertEquals(0, SearchRanking.editDistance("arijit", "arijit"))
        assertEquals(3, SearchRanking.editDistance("kitten", "sitting"))
        assertEquals(5, SearchRanking.editDistance("", "hello"))
    }

    @Test
    fun `similarity is one for identical strings`() {
        assertEquals(1.0, SearchRanking.similarity("arijit singh", "Arijit Singh")!!, 1e-9)
        assertEquals(0.0, SearchRanking.similarity("", "x")!!, 1e-9)
    }

    @Test
    fun `query variants cover and-removal and squash`() {
        // Note: the `&` -> `and` branch is dead upstream too, because
        // normalization strips `&` before the check runs.
        val variants = SearchRanking.queryVariants("Tom and Jerry")
        assertTrue("tom and jerry" in variants)
        assertTrue("tom jerry" in variants)
        assertTrue("tomandjerry" in variants)
    }

    @Test
    fun `exact match scores one hundred`() {
        assertEquals(100.0, SearchRanking.artistScore("Arijit Singh", "arijit singh")!!, 1e-9)
    }

    @Test
    fun `unrelated names score zero`() {
        assertEquals(0.0, SearchRanking.artistScore("zz top", "arijit singh")!!, 1e-9)
    }

    @Test
    fun `prefix match beats substring match`() {
        val prefix = SearchRanking.artistScore("Arijit Singh Live", "arijit")!!
        val substring = SearchRanking.artistScore("Best of Arijit", "arijit")!!
        assertTrue(prefix > substring)
    }
}
