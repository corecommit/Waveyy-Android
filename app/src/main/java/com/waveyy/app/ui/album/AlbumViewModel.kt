package com.waveyy.app.ui.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.SongRepository
import com.waveyy.app.domain.model.Album
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Album screen state. */
sealed interface AlbumUiState {
    /** Loading the album. */
    data object Loading : AlbumUiState

    /** Loaded album. */
    data class Success(val album: Album) : AlbumUiState

    /** Load failed with [message]. */
    data class Error(val message: String) : AlbumUiState
}

/** Album detail, mirroring `album/[id]/page.jsx` + `Album.jsx`. */
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val repository: SongRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val albumId: String = savedStateHandle.get<String>("id").orEmpty()

    private val mutableState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    /** Screen state. */
    val state: StateFlow<AlbumUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { repository.albumById(albumId).first() }
                .onSuccess { album ->
                    mutableState.value = album?.let(AlbumUiState::Success)
                        ?: AlbumUiState.Error("Album not found")
                }
                .onFailure { mutableState.value = AlbumUiState.Error(it.message ?: "Album failed") }
        }
    }
}
