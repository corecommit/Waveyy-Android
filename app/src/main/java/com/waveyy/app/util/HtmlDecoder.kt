package com.waveyy.app.util

/**
 * Decodes HTML entities in API strings (song/album/artist names arrive
 * entity-encoded). Ports the server-side fallback of `lib/decode-html.js`
 * in pure Kotlin so it stays unit-testable on the JVM (no `android.text.Html`,
 * which is unavailable in local unit tests).
 */
object HtmlDecoder {

    private val hexEntity = Regex("&#x([0-9a-fA-F]+);")
    private val decEntity = Regex("&#(\\d+);")

    /** Decodes common entities; non-string-safe input returns "". */
    fun decode(input: String?): String {
        var out = input ?: return ""
        out = out.replace("&amp;", "&")
        out = out.replace("&quot;", "\"")
        out = out.replace("&#39;", "'").replace("&apos;", "'")
        out = out.replace("&lt;", "<")
        out = out.replace("&gt;", ">")
        out = hexEntity.replace(out) { match ->
            val code = match.groupValues[1].toIntOrNull(16) ?: return@replace match.value
            codeToString(code) ?: match.value
        }
        out = decEntity.replace(out) { match ->
            val code = match.groupValues[1].toIntOrNull() ?: return@replace match.value
            codeToString(code) ?: match.value
        }
        return out
    }

    private fun codeToString(code: Int): String? {
        return runCatching { String(Character.toChars(code)) }.getOrNull()
    }
}
