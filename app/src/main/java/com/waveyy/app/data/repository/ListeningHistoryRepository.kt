package com.waveyy.app.data.repository

import com.waveyy.app.data.remote.auth.SupabaseClientProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Supabase `listening_events` row (see `supabase/listening_events.sql`). */
@Serializable
data class ListeningEventRow(
    val user_id: String,
    val song_id: String,
    val played_at: String
)

/** Supabase `listening_events` read projection. */
@Serializable
data class ListeningEventRead(
    val song_id: String,
    val played_at: String? = null
)

/**
 * Listening-history port of `lib/listening-events.js`: fire-and-forget
 * logging plus recent-event reads (default 5 days / 300 rows, matching the
 * web defaults). Server-side retention (90 days / 5000 rows) lives in the
 * SQL trigger; no client work needed.
 */
@Singleton
class ListeningHistoryRepository @Inject constructor(
    private val clientProvider: SupabaseClientProvider
) {

    /** Inserts a play event; failures (incl. missing table) are returned. */
    suspend fun logListeningEvent(userId: String?, songId: String): Result<Unit> {
        if (userId.isNullOrBlank() || songId.isBlank()) return Result.success(Unit)
        return runCatching {
            clientProvider.client.from("listening_events").insert(
                ListeningEventRow(
                    user_id = userId,
                    song_id = songId,
                    played_at = java.time.Instant.now().toString()
                )
            )
            Unit
        }
    }

    /** Recent song ids, newest first, bounded by [days] and [limit]. */
    suspend fun recentSongIds(
        userId: String?,
        days: Int = 5,
        limit: Int = 300
    ): Result<List<String>> {
        if (userId.isNullOrBlank()) return Result.success(emptyList())
        return runCatching {
            val cutoff = java.time.Instant.now().minusSeconds(days * 24L * 60L * 60L).toString()
            clientProvider.client.from("listening_events").select {
                filter { eq("user_id", userId) }
            }.decodeList<ListeningEventRead>()
                .filter { (it.played_at ?: "") >= cutoff }
                .sortedByDescending { it.played_at.orEmpty() }
                .take(limit)
                .map { it.song_id }
        }
    }
}
