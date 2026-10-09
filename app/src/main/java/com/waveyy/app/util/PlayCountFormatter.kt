package com.waveyy.app.util

/**
 * Compact play-count formatting. Ports `formatPlayCount` in `lib/utils.js`:
 * 1.5K, 2.3M, 1.1B (trailing `.0` trimmed).
 */
object PlayCountFormatter {

    /** Formats [count] compactly, returning "" for missing values. */
    fun format(count: Long): String {
        if (count <= 0L) return ""
        if (count >= 1_000_000_000L) return trim(count / 1_000_000_000.0) + "B"
        if (count >= 1_000_000L) return trim(count / 1_000_000.0) + "M"
        if (count >= 1_000L) return trim(count / 1_000.0) + "K"
        return count.toString()
    }

    /** Formats [count], returning [fallback] when count is unknown/zero. */
    fun formatOr(count: Long, fallback: String): String {
        val text = format(count)
        return text.ifEmpty { fallback }
    }

    private fun trim(value: Double): String {
        val text = "%.1f".format(value)
        return if (text.endsWith(".0")) text.dropLast(2) else text
    }
}
