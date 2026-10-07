package com.waveyy.app.util

/**
 * Devanagari to Latin transliteration for romanized lyrics display.
 * Faithful port of `toHinglish` in `lib/hinglish.js`, including nukta
 * handling, halant clusters, and schwa deletion.
 */
object Hinglish {

    private val devanagari = Regex("[\\u0900-\\u097F]")
    private const val HALANT = "्"
    private const val NUKTA = "़"

    private val vowels = mapOf(
        "अ" to "a", "आ" to "aa", "इ" to "i", "ई" to "ii", "उ" to "u",
        "ऊ" to "uu", "ऋ" to "ri", "ए" to "e", "ऐ" to "ai", "ओ" to "o",
        "औ" to "au", "ऑ" to "o", "ऍ" to "e"
    )

    private val matras = mapOf(
        "ा" to "aa", "ि" to "i", "ी" to "ii", "ु" to "u", "ू" to "uu",
        "ृ" to "ri", "े" to "e", "ै" to "ai", "ो" to "o", "ौ" to "au",
        "ॉ" to "o", "ॅ" to "e", "ं" to "n", "ँ" to "n", "ः" to "h"
    )

    private val consonants = mapOf(
        "क" to "k", "ख" to "kh", "ग" to "g", "घ" to "gh", "ङ" to "ng",
        "च" to "ch", "छ" to "chh", "ज" to "j", "झ" to "jh", "ञ" to "ny",
        "ट" to "t", "ठ" to "th", "ड" to "d", "ढ" to "dh", "ण" to "n",
        "त" to "t", "थ" to "th", "द" to "d", "ध" to "dh", "न" to "n",
        "प" to "p", "फ" to "ph", "ब" to "b", "भ" to "bh", "म" to "m",
        "य" to "y", "र" to "r", "ल" to "l", "व" to "v", "श" to "sh",
        "ष" to "sh", "स" to "s", "ह" to "h", "ळ" to "l",
        "क़" to "q", "ख़" to "kh", "ग़" to "gh", "ज़" to "z",
        "ड़" to "d", "ढ़" to "rh", "फ़" to "f", "य़" to "y"
    )

    private val nuktaReplacements = listOf(
        "क़".let { "क$NUKTA" } to "क़",
        "ख$NUKTA" to "ख़",
        "ग$NUKTA" to "ग़",
        "ज$NUKTA" to "ज़",
        "ड$NUKTA" to "ड़",
        "ढ$NUKTA" to "ढ़",
        "फ$NUKTA" to "फ़",
        "य$NUKTA" to "य़"
    )

    private val vnde = Regex("\\bvnde\\b", RegexOption.IGNORE_CASE)

    /**
     * Transliterates Devanagari text. Strings without Devanagari pass
     * through untouched, mirroring the JS early return.
     */
    fun toHinglish(text: String): String {
        if (text.isEmpty() || !devanagari.containsMatchIn(text)) return text
        var normalized = text
        for ((base, composed) in nuktaReplacements) {
            normalized = normalized.replace(base, composed)
        }
        val out = StringBuilder()
        var index = 0
        while (index < normalized.length) {
            val ch = normalized[index].toString()
            val next = normalized.getOrNull(index + 1)?.toString()
            val vowel = vowels[ch]
            if (vowel != null) {
                out.append(vowel)
                index += 1
                continue
            }
            val base = consonants[ch]
            if (base != null) {
                if (next == HALANT) {
                    out.append(base)
                    index += 2
                    continue
                }
                val matra = next?.let(matras::get)
                if (matra != null) {
                    out.append(base).append(matra)
                    index += 2
                    continue
                }
                // Schwa deletion: no inherent 'a' before a non-Devanagari
                // char or at the end of the string.
                if (next != null && devanagari.containsMatchIn(next) && next != NUKTA) {
                    out.append(base).append("a")
                } else {
                    out.append(base)
                }
                index += 1
                continue
            }
            val standaloneMatra = matras[ch]
            if (standaloneMatra != null) {
                out.append(standaloneMatra)
                index += 1
                continue
            }
            if (ch == NUKTA) {
                index += 1
                continue
            }
            if (devanagari.containsMatchIn(ch)) {
                index += 1
                continue
            }
            out.append(ch)
            index += 1
        }
        return vnde.replace(out.toString(), "vande")
    }
}
