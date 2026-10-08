package com.waveyy.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.BuildConfig
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.ui.components.WaveyyButton
import com.waveyy.app.ui.components.WaveyyDropdownMenu
import com.waveyy.app.ui.components.WaveyyMenuEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Audio quality options (best = highest-ranked stream). */
val AUDIO_QUALITY_OPTIONS = listOf("Best", "High (320kbps)", "Medium (160kbps)", "Low (96kbps)")

/** Settings backing state: audio quality preference, version, sign-out. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    private val auth: AuthRepository
) : ViewModel() {
    /** Persisted audio quality label. */
    val audioQuality = userPrefs.audioQualityFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), AUDIO_QUALITY_OPTIONS.first())

    /** App version name. */
    val versionName: String = BuildConfig.VERSION_NAME

    /** Current session (null for guests). */
    val session = auth.sessionFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    /** Persists the audio quality choice. */
    fun setAudioQuality(option: String) {
        viewModelScope.launch { userPrefs.setAudioQuality(option) }
    }

    /** Whether the YouTube Music source is enabled. */
    val ytmusicEnabled = userPrefs.ytmusicEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles the YouTube Music source. */
    fun setYtmusicEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setYtmusicEnabled(enabled) }
    }

    /** Whether hybrid races resolve fast-first (vs merged-complete). */
    val preferFast = userPrefs.preferFastFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles race mode. */
    fun setPreferFast(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setPreferFast(enabled) }
    }

    /** Signs out. */
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

/**
 * Settings: audio quality, theme note (dark enforced), about/version,
 * sign-out. Reachable from the user panel dropdown.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSignIn: () -> Unit = {}
) {
    val quality by viewModel.audioQuality.collectAsState()
    val ytmusicEnabled by viewModel.ytmusicEnabled.collectAsState()
    val preferFast by viewModel.preferFast.collectAsState()
    val session by viewModel.session.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Audio quality", style = MaterialTheme.typography.titleMedium)
        WaveyyDropdownMenu(
            entries = AUDIO_QUALITY_OPTIONS.map { option ->
                WaveyyMenuEntry(option) { viewModel.setAudioQuality(option) }
            },
            anchor = { open ->
                WaveyyButton(text = quality, onClick = open, modifier = Modifier.fillMaxWidth())
            }
        )
        Text(text = "Sources", style = MaterialTheme.typography.titleMedium)
        SourceSwitch(
            label = "Enable YouTube Music",
            checked = ytmusicEnabled,
            onChecked = viewModel::setYtmusicEnabled
        )
        SourceSwitch(
            label = "Prefer fast over complete",
            checked = preferFast,
            onChecked = viewModel::setPreferFast
        )
        Text(text = "Theme", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Dark (Waveyy is dark-only)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = "About", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Waveyy ${viewModel.versionName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (session != null) {
            WaveyyButton(
                text = "Sign out",
                onClick = viewModel::signOut,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            WaveyyButton(
                text = "Sign in",
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Labeled switch row for source toggles. */
@Composable
private fun SourceSwitch(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
