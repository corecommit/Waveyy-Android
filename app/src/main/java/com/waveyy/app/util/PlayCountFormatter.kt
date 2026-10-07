package com.waveyy.app.util

/**
 * Compact play-count formatting. Ports `formatPlayCount` in `lib/utils.js`:
 * 1.5K, 2.3M, 1.1B (trailing `.0` trimmed).
 */
object PlayCountFormatter {

    /** Formats [count] compactly, returning "0" for missing values. */
    fun format(count: Long): String {
        if (count <= 0L) return "0"
        if (count >= 1_000_000_000L) return trim(count / 1_000_000_000.0) + "B"
        if (count >= 1_000_000L) return trim(count / 1_000_000.0) + "M"
        if (count >= 1_000L) return trim(count / 1_000.0) + "K"
        return count.toString()
    }

    private fun trim(value: Double): String {
        val text = "%.1f".format(value)
        return if (text.endsWith(".0")) text.dropLast(2) else text
    }
}
