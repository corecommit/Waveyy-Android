package com.waveyy.app.data.repository

import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.remote.auth.SupabaseClientProvider
import com.waveyy.app.domain.model.Playlist
import com.waveyy.app.domain.model.Song
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Supabase `playlists` row. */
@Serializable
data class PlaylistRow(
    val id: String,
    val user_id: String? = null,
    val name: String? = null,
    val created_at: String? = null
)

/** Supabase `playlist_songs` row. */
@Serializable
data class PlaylistSongRow(
    val playlist_id: String,
    val user_id: String? = null,
    val song_id: String,
    val song_name: String? = null,
    val song_artist: String? = null,
    val song_image: String? = null,
    val play_count: Long = 0L,
    val created_at: String? = null
)

/** Thrown when the playlists schema has not been applied yet. */
class PlaylistBackendMissingException :
    IllegalStateException("Playlists are not configured yet. Run supabase/playlists.sql in Supabase.")

/**
 * Playlist CRUD ported from `lib/playlists.js`, backed by the `playlists`
 * and `playlist_songs` tables (`supabase/playlists.sql`). Guests use
 * local-only DataStore playlists; a missing schema surfaces as
 * [PlaylistBackendMissingException] (PGRST205).
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val userPrefs: UserPrefs
) {

    /** Returns true when [error] means the playlists tables do not exist. */
    fun isBackendMissing(error: Throwable?): Boolean {
        if (error is PlaylistBackendMissingException) return true
        if (error is PostgrestRestException && error.code == MISSING_TABLE_CODE) return true
        val message = generateSequence(error) { it.cause }.mapNotNull { it.message }.joinToString(" ")
        return message.contains(MISSING_TABLE_CODE) ||
            message.contains("Could not find the table 'public.playlists'") ||
            message.contains("Could not find the table 'public.playlist_songs'")
    }

    private fun missing(): Result<Nothing> = Result.failure(PlaylistBackendMissingException())

    /** Loads the user's playlists with songs (liked first). */
    suspend fun getPlaylists(userId: String?): Result<List<Playlist>> {
        if (userId.isNullOrBlank()) {
            return Result.success(userPrefs.guestPlaylists())
        }
        return runCatching {
            val client = clientProvider.client
            val rows = client.from("playlists").select {
                filter { eq("user_id", userId) }
            }.decodeList<PlaylistRow>()
            val songs = client.from("playlist_songs").select {
                filter { eq("user_id", userId) }
            }.decodeList<PlaylistSongRow>()
            val byPlaylist = songs.groupBy { it.playlist_id }
            val likedId = likedPlaylistId(userId)
            rows.map { row -> toDomain(row, byPlaylist[row.id].orEmpty()) }
                .sortedByDescending { it.createdAt }
                .let { sorted ->
                    val liked = sorted.find { it.id == likedId }
                    if (liked == null) sorted else listOf(liked) + sorted.filter { it.id != likedId }
                }
        }.recoverCatching { error ->
            if (isBackendMissing(error)) throw PlaylistBackendMissingException()
            throw error
        }
    }

    /** Creates a playlist (optimistic id generated client-side, like the web). */
    suspend fun createPlaylist(userId: String?, name: String): Result<Playlist> {
        val safeName = name.trim().ifBlank { "My Playlist" }
        if (safeName.equals(LIKED_NAME, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("\"$LIKED_NAME\" is reserved for your likes"))
        }
        if (userId.isNullOrBlank()) {
            val playlist = Playlist(
                id = "local_${System.currentTimeMillis()}_${(0..99999).random()}",
                name = safeName,
                songs = emptyList(),
                createdAt = System.currentTimeMillis()
            )
            val existing = userPrefs.guestPlaylists()
            userPrefs.setGuestPlaylists(existing + playlist)
            return Result.success(playlist)
        }
        return runCatching {
            val playlist = Playlist(
                id = "pl_${System.currentTimeMillis()}_${(0..99999).random()}",
                name = safeName,
                songs = emptyList(),
                createdAt = System.currentTimeMillis()
            )
            clientProvider.client.from("playlists").insert(
                PlaylistRow(id = playlist.id, user_id = userId, name = playlist.name)
            )
            playlist
        }.recoverCatching { error ->
            if (isBackendMissing(error)) throw PlaylistBackendMissingException()
            throw error
        }
    }

    /** Deletes a playlist (the Liked list is protected). */
    suspend fun deletePlaylist(userId: String?, playlistId: String): Result<Unit> {
        if (userId.isNullOrBlank()) {
            if (playlistId == "local_liked") {
                return Result.failure(IllegalArgumentException("Liked Songs playlist cannot be deleted"))
            }
            val existing = userPrefs.guestPlaylists()
            userPrefs.setGuestPlaylists(existing.filterNot { it.id == playlistId })
            return Result.success(Unit)
        }
        if (isLikedPlaylist(playlistId, userId)) {
            return Result.failure(IllegalArgumentException("Liked Songs playlist cannot be deleted"))
        }
        return runCatching {
            clientProvider.client.from("playlists").delete {
                filter {
                    eq("user_id", userId)
                    eq("id", playlistId)
                }
            }
            Unit
        }.recoverCatching { error ->
            if (isBackendMissing(error)) throw PlaylistBackendMissingException()
            throw error
        }
    }

    /** Adds a song (upsert on playlist + song). */
    suspend fun addSong(userId: String?, playlistId: String, song: Song): Result<Unit> {
        if (userId.isNullOrBlank()) {
            val existing = userPrefs.guestPlaylists()
            val updated = existing.map { pl ->
                if (pl.id == playlistId && pl.songs.none { it.id == song.id }) {
                    pl.copy(songs = pl.songs + song)
                } else pl
            }
            userPrefs.setGuestPlaylists(updated)
            return Result.success(Unit)
        }
        return runCatching {
            clientProvider.client.from("playlist_songs").upsert(
                PlaylistSongRow(
                    playlist_id = playlistId,
                    user_id = userId,
                    song_id = song.id,
                    song_name = song.name,
                    song_artist = song.artist,
                    song_image = song.imageUrl,
                    play_count = song.playCount
                )
            ) {
                onConflict = "playlist_id,song_id"
            }
            Unit
        }.recoverCatching { error ->
            if (isBackendMissing(error)) throw PlaylistBackendMissingException()
            throw error
        }
    }

    /** Removes a song from a playlist. */
    suspend fun removeSong(userId: String?, playlistId: String, songId: String): Result<Unit> {
        if (userId.isNullOrBlank()) {
            val existing = userPrefs.guestPlaylists()
            val updated = existing.map { pl ->
                if (pl.id == playlistId) {
                    pl.copy(songs = pl.songs.filterNot { it.id == songId })
                } else pl
            }
            userPrefs.setGuestPlaylists(updated)
            return Result.success(Unit)
        }
        return runCatching {
            clientProvider.client.from("playlist_songs").delete {
                filter {
                    eq("user_id", userId)
                    eq("playlist_id", playlistId)
                    eq("song_id", songId)
                }
            }
            Unit
        }.recoverCatching { error ->
            if (isBackendMissing(error)) throw PlaylistBackendMissingException()
            throw error
        }
    }

    /** Ensures the reserved `liked_<userId>` playlist exists. */
    suspend fun ensureLikedPlaylist(userId: String?): Result<Playlist> {
        if (userId.isNullOrBlank()) {
            val existing = userPrefs.guestPlaylists()
            val liked = existing.firstOrNull { it.id == "local_liked" }
            if (liked != null) return Result.success(liked)
            val newLiked = Playlist(
                id = "local_liked",
                name = LIKED_NAME,
                songs = emptyList(),
                createdAt = 0L
            )
            userPrefs.setGuestPlaylists(existing + newLiked)
            return Result.success(newLiked)
        }
        val id = likedPlaylistId(userId)
        getPlaylists(userId).onSuccess { playlists ->
            playlists.find { it.id == id }?.let { return Result.success(it) }
        }.onFailure { error ->
            if (isBackendMissing(error)) return missing()
        }
        return runCatching {
            val liked = Playlist(id = id, name = LIKED_NAME, songs = emptyList(), createdAt = 0L)
            clientProvider.client.from("playlists").insert(
                PlaylistRow(id = liked.id, user_id = userId, name = liked.name)
            )
            liked
        }.recoverCatching { error ->
            if (isBackendMissing(error)) throw PlaylistBackendMissingException()
            throw error
        }
    }

    private fun toDomain(row: PlaylistRow, songs: List<PlaylistSongRow>): Playlist {
        return Playlist(
            id = row.id,
            name = row.name ?: "My Playlist",
            songs = songs.map { song ->
                Song(
                    id = song.song_id,
                    name = song.song_name ?: "Unknown",
                    artist = song.song_artist ?: "unknown",
                    album = "unknown",
                    imageUrl = song.song_image.orEmpty(),
                    durationSec = 0L,
                    playCount = song.play_count,
                    streams = emptyList()
                )
            },
            createdAt = 0L
        )
    }

    companion object {
        private const val LIKED_NAME = "Liked Songs"
        private const val LIKED_PREFIX = "liked_"
        private const val MISSING_TABLE_CODE = "PGRST205"

        /** Reserved id for a user's Liked Songs list. */
        fun likedPlaylistId(userId: String): String = LIKED_PREFIX + userId

        /** True when [playlistId] is the user's reserved Liked list. */
        fun isLikedPlaylist(playlistId: String?, userId: String?): Boolean {
            return !playlistId.isNullOrBlank() && !userId.isNullOrBlank() &&
                playlistId == likedPlaylistId(userId)
        }
    }
}
