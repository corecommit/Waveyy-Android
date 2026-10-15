package com.waveyy.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.waveyy.app.domain.model.HomeSnapshot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.homeSnapshotDataStore by preferencesDataStore(name = "home_snapshot")

/**
 * Persists a single Home snapshot blob. Separate DataStore file so
 * UserPrefs stays focused on user-controlled toggles, and so a corrupt
 * snapshot can be cleared without touching prefs.
 */
@Singleton
class HomeSnapshotStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val key = stringPreferencesKey(SNAPSHOT_KEY)

    /** Reads the stored snapshot, or null when absent / corrupt. */
    suspend fun read(): HomeSnapshot? {
        val raw = context.homeSnapshotDataStore.data.first()[key] ?: return null
        return runCatching { Json.decodeFromString<HomeSnapshot>(raw) }.getOrNull()
    }

    /** Overwrites the snapshot. */
    suspend fun write(snapshot: HomeSnapshot) {
        val raw = Json.encodeToString(snapshot)
        context.homeSnapshotDataStore.edit { prefs -> prefs[key] = raw }
    }

    /** Clears the snapshot (Settings → Clear cache, future use). */
    suspend fun clear() {
        context.homeSnapshotDataStore.edit { prefs -> prefs.remove(key) }
    }

    companion object {
        private const val SNAPSHOT_KEY = "home-snapshot-json"
    }
}
