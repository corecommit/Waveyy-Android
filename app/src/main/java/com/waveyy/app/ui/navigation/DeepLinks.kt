package com.waveyy.app.ui.navigation

import android.net.Uri

/**
 * Deep-link parsing. Phase 10 registers `waveyy://room/<id>`; song, album,
 * artist, and playlist links land in Phase 11.
 */
object DeepLinks {

    /** Custom scheme for app links. */
    const val SCHEME = "waveyy"

    /** Room host (`waveyy://room/<id>`). */
    const val HOST_ROOM = "room"

    /**
     * Extracts a room id from [uri], or null when it is not a room link.
     */
    fun roomIdFrom(uri: Uri?): String? {
        if (uri == null) return null
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return null
        if (!uri.host.equals(HOST_ROOM, ignoreCase = true)) return null
        return uri.lastPathSegment?.takeIf { it.isNotBlank() }
    }
}
