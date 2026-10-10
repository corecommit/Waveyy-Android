package com.waveyy.app.ui.navigation

/** Root graph destinations. Full route table lands in Phase 5. */
object Routes {
    const val HOME = "home"
    const val SEARCH = "search?query={query}"
    const val SETTINGS = "settings"
    const val ALBUM = "album/{id}"
    const val ARTIST = "artist/{id}"
    const val PLAYLISTS = "playlists"
    const val PLAYLIST_DETAIL = "playlists/{id}"
    const val LIKED_SONGS = "liked"
    const val PROFILE = "profile"
    const val TOGETHER = "together?roomId={roomId}"
    const val CONNECTIONS = "connections"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val PLAYER = "player/{id}"
    const val FULL_LYRICS = "lyrics/{id}"
    const val HISTORY = "history"

    /** Debug-only theme gallery (registered in debug builds only). */
    const val THEME_GALLERY = "debug/theme-gallery"

    /** Route with the `{id}` argument filled in. */
    fun withId(route: String, id: String): String {
        return route.replace("{id}", id)
    }

    /** Search route with the query filled in (URL-encoded). */
    fun withQuery(query: String): String {
        return SEARCH.replace("{query}", android.net.Uri.encode(query))
    }
}
