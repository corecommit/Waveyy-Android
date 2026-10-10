package com.waveyy.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recent search queries, most-recent-first, capped at 10 and deduplicated
 * case-insensitively. Written when a submitted query returns results (or a
 * suggestion row is tapped).
 */
@Singleton
class SearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore get() = context.searchHistory
    private val key = stringPreferencesKey(HISTORY_KEY)

    /** History, newest first. */
    val history: Flow<List<String>> = context.searchHistory.data.map { prefs ->
        prefs[key]?.let { decode(it) }.orEmpty()
    }

    /** Records [query] (moves to front, dedupes, caps at 10). */
    suspend fun add(query: String) {
        val normalized = query.trim()
        if (normalized.isEmpty()) return
        val current = history.first().toMutableList()
        current.removeAll { it.equals(normalized, ignoreCase = true) }
        current.add(0, normalized)
        save(current.take(MAX_SIZE))
    }

    /** Removes one entry (case-insensitive match). */
    suspend fun remove(query: String) {
        val current = history.first().toMutableList()
        current.removeAll { it.equals(query.trim(), ignoreCase = true) }
        save(current)
    }

    /** Clears all history. */
    suspend fun clear() {
        save(emptyList())
    }

    /** Restores a previously captured list (used for undo). */
    suspend fun restore(queries: List<String>) {
        save(queries.take(MAX_SIZE))
    }

    private suspend fun save(queries: List<String>) {
        context.searchHistory.edit { prefs ->
            prefs[key] = Json.encodeToString(queries)
        }
    }

    private fun decode(raw: String): List<String> {
        return runCatching { Json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    companion object {
        private const val HISTORY_KEY = "search-history"
        private const val MAX_SIZE = 10
    }
}

private val Context.searchHistory by androidx.datastore.preferences.preferencesDataStore(
    name = "search_history"
)
