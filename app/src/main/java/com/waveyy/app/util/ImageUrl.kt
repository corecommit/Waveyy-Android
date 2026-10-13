package com.waveyy.app.util

/**
 * JioSaavn CDN image helpers. Ports `lib/image-url.js` verbatim in behavior:
 * WebP upgrade, Saavn WebP-to-JPG fallback, and rendition resizing.
 */
object ImageUrl {

    private val webpSuffix = Regex("""\.(webp)(\?.*)?$""", RegexOption.IGNORE_CASE)
    private val rasterSuffix = Regex("""\.(jpe?g|png)(\?.*)?$""", RegexOption.IGNORE_CASE)
    private val sizeToken = Regex("""-(50x50|150x150|500x500)\.""", RegexOption.IGNORE_CASE)

    /**
     * Rewrites a JPG/PNG URL to WebP. Returns null for data URIs, existing
     * WebP URLs pass through, anything else returns null.
     */
    fun toWebpUrl(url: String): String? {
        if (url.isBlank() || url.startsWith("data:")) return null
        if (webpSuffix.containsMatchIn(url)) return url
        if (rasterSuffix.containsMatchIn(url)) {
            return url.replace(rasterSuffix, ".webp$2")
        }
        return null
    }

    /** Rewrites a Saavn WebP URL back to JPG. Null unless it is one. */
    fun toSaavnJpgUrl(url: String): String? {
        if (url.isBlank() || !url.contains("saavncdn.com", ignoreCase = true)) return null
        if (!webpSuffix.containsMatchIn(url)) return null
        return url.replace(webpSuffix, ".jpg$2")
    }

    /**
     * Swaps the Saavn rendition token. Valid sizes: 50x50, 150x150, 500x500.
     * Non-Saavn URLs pass through unchanged.
     */
    fun resizeSaavnUrl(url: String, size: String = "150x150"): String {
        if (url.isBlank() || !url.contains("saavncdn.com", ignoreCase = true)) return url
        return url.replace(sizeToken, "-$size.")
    }

    /**
     * True when [url] is a YouTube-hosted thumbnail. YT thumbnails have no
     * WebP variant and no Saavn fallback, so callers must skip the WebP
     * swap and the Saavn JPG fallback for these.
     */
    fun isYouTubeThumbnail(url: String): Boolean {
        return url.contains("googleusercontent.com", ignoreCase = true) ||
            url.contains("ytimg.com", ignoreCase = true) ||
            url.contains("youtube.com", ignoreCase = true)
    }

    /**
     * Returns a smaller variant of a YouTube thumbnail when possible.
     * YT thumbnail URLs carry a size token like `=w544-h544-l90-rj`; a
     * smaller variant loads faster on weak connections and is a viable
     * retry when the original 404s.
     *
     * Returns null when [url] isn't a YT thumbnail or already uses the
     * smallest size.
     */
    fun youTubeFallback(url: String): String? {
        if (!isYouTubeThumbnail(url)) return null
        // YT size tokens: wXXX-hXXX-...
        val sizeRegex = Regex("""=w(\d+)-h(\d+)""")
        val match = sizeRegex.find(url) ?: return null
        val currentWidth = match.groupValues[1].toIntOrNull() ?: return null
        if (currentWidth <= 120) return null
        // Fall back to a 120px variant; always exists for real videos.
        return url.replace(sizeRegex, "=w120-h120")
    }

    /**
     * Swaps any rendition token in a Saavn URL to [size] (e.g. "150x150").
     * Returns null when the URL is blank, non-Saavn, or already uses the
     * requested size.
     */
    fun swapSaavnRendition(url: String, size: String): String? {
        if (url.isBlank() || !url.contains("saavncdn.com", ignoreCase = true)) return null
        if (url.contains("-$size.", ignoreCase = true)) return null
        // Match any existing size token (-50x50. -150x150. -500x500. etc.)
        val anySizeToken = Regex("""-\d+x\d+\.""", RegexOption.IGNORE_CASE)
        return if (anySizeToken.containsMatchIn(url)) {
            url.replace(anySizeToken, "-$size.")
        } else {
            null
        }
    }
}
