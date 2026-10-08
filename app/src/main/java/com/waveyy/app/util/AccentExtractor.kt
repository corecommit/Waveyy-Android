package com.waveyy.app.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Dominant-color extraction from artwork, framework-only (no Coil
 * internals): downloads bytes, decodes to ARGB, and reads the vibrant
 * swatch via `androidx.palette`. Results cache per key.
 */
object AccentExtractor {

    private val cache = mutableMapOf<String, Int?>()
    private val mutex = Mutex()

    /**
     * Returns the vibrant RGB for [imageUrl], or null when extraction
     * fails. Results (including failures) cache under [cacheKey].
     */
    suspend fun vibrantRgb(imageUrl: String, cacheKey: String): Int? {
        mutex.withLock {
            if (cache.containsKey(cacheKey)) return cache[cacheKey]
        }
        val rgb = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = URL(imageUrl).readBytes()
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@runCatching null
                if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        ?: return@runCatching null
                }
                val palette = Palette.from(bitmap).generate()
                (palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb
            }.getOrNull()
        }
        mutex.withLock { cache[cacheKey] = rgb }
        return rgb
    }

    /** Clears the cache (tests / memory pressure). */
    suspend fun clear() {
        mutex.withLock { cache.clear() }
    }
}
