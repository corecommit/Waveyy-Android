package com.waveyy.app.util

import com.waveyy.app.domain.model.AudioStream

/**
 * Picks the best playable stream from a song's `downloadUrl` entries.
 * Ports `selectBestAudioStream` / `selectBestAudioUrl` in
 * `lib/audio-quality.js`: lossless markers outrank everything, then raw
 * bitrate (parsed from the quality label or the `_<NNN>.<ext>` URL suffix).
 */
object AudioQualitySelector {

    private val losslessMarkers = listOf(
        "lossless", "flac", "wav", "alac", "1411", "24bit", "hi-res", "hires"
    )
    private val qualityBitrate = Regex("""(\d+(?:\.\d+)?)\s*(k|kbps|mbps|m)""")
    private val urlBitrate = Regex("""_(\d{2,4})\.(mp3|mp4|m4a|aac|flac|wav)\b""")

    private fun normalize(value: String): String {
        return value.lowercase().replace(Regex("""\s+"""), " ").trim()
    }

    private fun parseBitrate(quality: String, url: String): Int {
        val qualityText = normalize(quality)
        qualityBitrate.find(qualityText)?.let { match ->
            val value = match.groupValues[1].toDoubleOrNull() ?: return@let
            val unit = match.groupValues[2]
            if (!value.isFinite()) return@let
            return if (unit == "m" || unit == "mbps") (value * 1000).toInt() else value.toInt()
        }
        urlBitrate.find(normalize(url))?.let { match ->
            match.groupValues[1].toIntOrNull()?.let { return it }
        }
        return 0
    }

    private fun isLikelyLossless(quality: String, url: String): Boolean {
        val combined = normalize(quality) + " " + normalize(url)
        return losslessMarkers.any { combined.contains(it) }
    }

    private fun rankOf(stream: AudioStream): Long {
        val bitrate = if (stream.bitrateKbps > 0) {
            stream.bitrateKbps
        } else {
            parseBitrate(stream.quality, stream.url)
        }
        val lossless = stream.lossless || isLikelyLossless(stream.quality, stream.url)
        return (if (lossless) 1_000_000L else 0L) + bitrate
    }

    /** Returns the highest-ranked stream, or null when empty. */
    fun selectBest(streams: List<AudioStream>): AudioStream? {
        val ranked = streams.filter { it.url.isNotBlank() }
        if (ranked.isEmpty()) return null
        return ranked.maxByOrNull(::rankOf)?.let { best ->
            val bitrate = if (best.bitrateKbps > 0) {
                best.bitrateKbps
            } else {
                parseBitrate(best.quality, best.url)
            }
            val lossless = best.lossless || isLikelyLossless(best.quality, best.url)
            best.copy(bitrateKbps = bitrate, lossless = lossless)
        }
    }

    /** Shorthand returning just the URL, or "" when empty. */
    fun selectBestUrl(streams: List<AudioStream>): String {
        return selectBest(streams)?.url.orEmpty()
    }
}
