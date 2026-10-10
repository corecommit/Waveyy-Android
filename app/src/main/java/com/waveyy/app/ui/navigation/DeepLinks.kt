package com.waveyy.app.ui.navigation

import android.net.Uri

/**
 * Deep-link parsing for `waveyy://<host>/<id>` links (song, album, artist,
 * playlist, room). Only room resolution is wired to auto-join today; the
 * rest land on their detail routes via Navigation deep links.
 */
object DeepLinks {

    /** Custom scheme for app links. */
    const val SCHEME = "waveyy"

    /** Room host (`waveyy://room/<id>`). */
    const val HOST_ROOM = "room"

    /** Song host (`waveyy://song/<id>`). */
    const val HOST_SONG = "song"

    /** Album host (`waveyy://album/<id>`). */
    const val HOST_ALBUM = "album"

    /** Artist host (`waveyy://artist/<id>`). */
    const val HOST_ARTIST = "artist"

    /** Playlist host (`waveyy://playlist/<id>`). */
    const val HOST_PLAYLIST = "playlist"

    /**
     * Extracts a room id from [uri], or null when it is not a room link.
     */
    fun roomIdFrom(uri: Uri?): String? = idFrom(uri, HOST_ROOM)

    /**
     * Extracts the id segment from [uri] when its host matches [host].
     */
    fun idFrom(uri: Uri?, host: String): String? {
        if (uri == null) return null
        if (!uri.scheme.equals(SCHEME, ignoreCase = true)) return null
        if (!uri.host.equals(host, ignoreCase = true)) return null
        return uri.lastPathSegment?.takeIf { it.isNotBlank() }
    }

    /**
     * Extracts the first `waveyy://` URI found inside arbitrary shared
     * text (the user may share with a title line prepended).
     */
    fun extractWaveyyUri(text: String?): Uri? {
        if (text.isNullOrBlank()) return null
        val idx = text.indexOf("$SCHEME://")
        if (idx < 0) return null
        val tail = text.substring(idx).trim()
        val end = tail.indexOfFirst { it == ' ' || it == '\n' || it == '\r' }
        val raw = if (end < 0) tail else tail.substring(0, end)
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    /**
     * Song id from a `waveyy://song/{id}` URI, or null when not a song link.
     */
    fun songIdFrom(uri: Uri?): String? = idFrom(uri, HOST_SONG)

    /**
     * Best-effort title hint from the first line of shared text. Returns the
     * part before the URL (typically "Song Name by Artist"), trimmed of
     * trailing whitespace and separators. Null when empty.
     */
    fun titleHintFrom(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val idx = text.indexOf("$SCHEME://")
        val before = if (idx < 0) text else text.substring(0, idx)
        return before
            .lineSequence()
            .map { it.trim().trimEnd(':', '-', '—') }
            .firstOrNull { it.isNotBlank() }
    }
}
