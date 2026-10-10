package com.waveyy.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.remote.mapper.SongMapper
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.Song
import com.waveyy.app.domain.usecase.TrendingCurator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Discover screen state. */
sealed interface DiscoverUiState {
    /** Loading. */
    data object Loading : DiscoverUiState

    /** Curated trending songs. */
    data class Success(val songs: List<Song>) : DiscoverUiState

    /** Load failed with [message]. */
    data class Error(val message: String) : DiscoverUiState
}

/**
 * Discover: full-screen trending curation (same pipeline as home rails).
 * Target of the compass button in the home header.
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: SongRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow<DiscoverUiState>(DiscoverUiState.Loading)
    /** Screen state. */
    val state: StateFlow<DiscoverUiState> = mutableState.asStateFlow()

    private val mutableRefreshing = MutableStateFlow(false)
    /** True while a soft pull-to-refresh is in flight (state stays Success). */
    val isRefreshing: StateFlow<Boolean> = mutableRefreshing.asStateFlow()

    init {
        refresh()
    }

    /** (Re)loads trending. */
    fun refresh() {
        mutableState.value = DiscoverUiState.Loading
        viewModelScope.launch {
            runCatching {
                val charts = repository.topCharts().first()
                val dtos = charts.map { song ->
                    com.waveyy.app.data.remote.dto.SongDto(
                        id = song.id,
                        name = song.name,
                        playCount = song.playCount
                    )
                }
                val (selected, _) = TrendingCurator.curate(dtos, 50)
                SongMapper.toDomainList(selected)
            }.onSuccess { mutableState.value = DiscoverUiState.Success(it) }
                .onFailure { mutableState.value = DiscoverUiState.Error(it.message ?: "Discover failed") }
        }
    }

    /**
     * Pull-to-refresh variant of [refresh]: does not flip state to Loading,
     * so the grid stays visible while the network re-fetches. On failure
     * the previous Success is retained.
     */
    fun refreshQuiet() {
        if (mutableRefreshing.value) return
        viewModelScope.launch {
            mutableRefreshing.value = true
            runCatching {
                val charts = repository.topCharts().first()
                val dtos = charts.map { song ->
                    com.waveyy.app.data.remote.dto.SongDto(
                        id = song.id,
                        name = song.name,
                        playCount = song.playCount
                    )
                }
                val (selected, _) = TrendingCurator.curate(dtos, 50)
                SongMapper.toDomainList(selected)
            }.onSuccess { mutableState.value = DiscoverUiState.Success(it) }
                .onFailure {
                    if (mutableState.value !is DiscoverUiState.Success) {
                        mutableState.value = DiscoverUiState.Error(it.message ?: "Discover failed")
                    }
                }
            mutableRefreshing.value = false
        }
    }
}
