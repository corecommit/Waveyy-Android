package com.waveyy.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.local.dao.HistoryDao
import com.waveyy.app.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** History screen state. */
sealed interface HistoryUiState {
    /** Loading. */
    data object Loading : HistoryUiState

    /** Loaded history rows (newest first). */
    data class Ready(val songs: List<Song>) : HistoryUiState
}

/**
 * History: reads the local listening-history table (capped at 50 rows),
 * maps rows to stream-less [Song]s. Playback re-resolves streams lazily
 * via [com.waveyy.app.ui.player.PlayerViewModel.playSource].
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyDao: HistoryDao
) : ViewModel() {

    private val mutableState = MutableStateFlow<HistoryUiState>(HistoryUiState.Loading)
    /** Screen state. */
    val state: StateFlow<HistoryUiState> = mutableState.asStateFlow()

    init { refresh() }

    /** Reloads the list from Room. */
    fun refresh() {
        viewModelScope.launch {
            val rows = historyDao.recent(HISTORY_LIMIT)
            mutableState.value = HistoryUiState.Ready(
                rows.map { row ->
                    Song(
                        id = row.songId,
                        name = row.name,
                        artist = row.artist,
                        album = "unknown",
                        imageUrl = row.image,
                        durationSec = 0L,
                        playCount = 0L,
                        streams = emptyList()
                    )
                }
            )
        }
    }

    /** Clears history, then reloads. */
    fun clear() {
        viewModelScope.launch {
            historyDao.clearAll()
            refresh()
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 50
    }
}
