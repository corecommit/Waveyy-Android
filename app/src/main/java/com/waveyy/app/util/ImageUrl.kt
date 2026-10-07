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
}
