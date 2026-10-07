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
}
