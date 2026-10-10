package com.waveyy.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.BuildConfig
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.lyrics.LyricsRaceRepository
import com.waveyy.app.data.repository.AuthRepository
import com.waveyy.app.ui.components.WaveyyBackButton
import com.waveyy.app.ui.player.rememberChromeBottomReserve
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Audio quality options (best = highest-ranked stream). */
val AUDIO_QUALITY_OPTIONS = listOf("Best", "High (320kbps)", "Medium (160kbps)", "Low (96kbps)")

/** Lyrics provider options (preferred mode), plus disabled. */
val LYRICS_PROVIDER_OPTIONS = listOf(
    "lrclib", "netease", "qqmusic", "lyricsovh", "tekstowo", "musixmatch", "genius", "none"
)

/** Settings backing state: audio quality preference, version, sign-out. */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPrefs,
    private val auth: AuthRepository,
    private val lyricsRace: LyricsRaceRepository
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

    /** Whether JioSaavn is enabled (default on). */
    val saavnEnabled = userPrefs.saavnEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles JioSaavn. */
    fun setSaavnEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setSaavnEnabled(enabled) }
    }

    /** Whether to prefer video over music (YT only). */
    val preferVideo = userPrefs.preferVideoFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    /** Toggles prefer-video. */
    fun setPreferVideo(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setPreferVideo(enabled) }
    }

    /** Whether the YouTube Music source is enabled. */
    val ytmusicEnabled = userPrefs.ytmusicEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles the YouTube Music source. */
    fun setYtmusicEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setYtmusicEnabled(enabled) }
    }

    /** Whether haptic feedback is enabled (default on). */
    val hapticsEnabled = userPrefs.hapticsEnabledFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Persists the haptics toggle. */
    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setHapticsEnabled(enabled) }
    }

    /** Whether hybrid races resolve fast-first (vs merged-complete). */
    val preferFast = userPrefs.preferFastFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    /** Toggles race mode. */
    fun setPreferFast(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setPreferFast(enabled) }
    }

    /** Lyrics race mode (`auto` or `preferred`). */
    val lyricsMode = userPrefs.lyricsModeFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPrefs.LYRICS_MODE_AUTO)

    /** Persists the lyrics race mode. */
    fun setLyricsMode(mode: String) {
        viewModelScope.launch { userPrefs.setLyricsMode(mode) }
    }

    /** Preferred lyrics provider (preferred mode). */
    val preferredLyricsProvider = userPrefs.lyricsProviderFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), UserPrefs.DEFAULT_LYRICS_PROVIDER)

    /** Persists the preferred lyrics provider. */
    fun setPreferredLyricsProvider(provider: String) {
        viewModelScope.launch { userPrefs.setLyricsProvider(provider) }
    }

    /** Race-winner debug caption toggle. */
    val lyricsDebug = userPrefs.lyricsDebugFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    /** Toggles the race-winner debug caption. */
    fun setLyricsDebug(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setLyricsDebug(enabled) }
    }

    /** Clears the lyrics race cache. */
    fun clearLyricsCache() {
        viewModelScope.launch { lyricsRace.clearCache() }
    }

    /** Whether to stop playback when the app is task-removed. */
    val stopMusicOnClose = userPrefs.stopMusicOnCloseFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), false)

    /** Toggles stop-on-close. */
    fun setStopMusicOnClose(enabled: Boolean) {
        viewModelScope.launch { userPrefs.setStopMusicOnClose(enabled) }
    }

    /** Signs out. */
    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

/**
 * Settings as sectioned cards with a top bar. Dropdown rows are display-only
 * for now (re-wire in a follow-up); switches call through immediately.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onSignIn: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val quality by viewModel.audioQuality.collectAsState()
    val saavnEnabled by viewModel.saavnEnabled.collectAsState()
    val preferVideo by viewModel.preferVideo.collectAsState()
    val ytmusicEnabled by viewModel.ytmusicEnabled.collectAsState()
    val preferFast by viewModel.preferFast.collectAsState()
    val stopMusicOnClose by viewModel.stopMusicOnClose.collectAsState()
    val session by viewModel.session.collectAsState()
    val lyricsMode by viewModel.lyricsMode.collectAsState()
    val preferredProvider by viewModel.preferredLyricsProvider.collectAsState()
    val lyricsDebug by viewModel.lyricsDebug.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()
    val context = LocalContext.current
    val chromeBottom = rememberChromeBottomReserve()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = chromeBottom)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WaveyyBackButton(onClick = onBack)
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ─── Audio ───
        item {
            SettingsSectionCard(title = "Audio quality") {
                SettingsClickRow(
                    label = "Stream quality",
                    value = quality,
                    onClick = { /* opens dropdown */ }
                )
                // ... existing dropdown as a row action
            }
        }

        // ─── Sources ───
        item {
            SettingsSectionCard(title = "Sources") {
                SettingsToggleRow(
                    label = "Enable JioSaavn",
                    subtitle = "Disable to test YouTube Music only",
                    checked = saavnEnabled,
                    onChecked = viewModel::setSaavnEnabled
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsToggleRow(
                    label = "Enable YouTube Music",
                    subtitle = "Adds YT Music as a second search source",
                    checked = ytmusicEnabled,
                    onChecked = viewModel::setYtmusicEnabled
                )
                if (ytmusicEnabled) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsToggleRow(
                        label = "Prefer video over music",
                        subtitle = "YouTube Music: return music-video versions when available",
                        checked = preferVideo,
                        onChecked = viewModel::setPreferVideo
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsToggleRow(
                    label = "Prefer fast over complete",
                    subtitle = "Return the first source's results instead of merging",
                    checked = preferFast,
                    onChecked = viewModel::setPreferFast
                )
            }
        }

        // ─── Playback ───
        item {
            SettingsSectionCard(title = "Playback") {
                SettingsToggleRow(
                    label = "Stop music when app closes",
                    subtitle = "When off, music continues in the background",
                    checked = stopMusicOnClose,
                    onChecked = viewModel::setStopMusicOnClose
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsClickRow(
                    label = "Battery optimization",
                    value = "Open settings",
                    onClick = {
                        // Opens the SYSTEM battery-optimization list; the user picks
                        // Waveyy manually. No REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        // permission — that permission triggers Play Store review.
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }
                )
            }
        }

        // ─── Lyrics ───
        item {
            SettingsSectionCard(title = "Lyrics") {
                SettingsClickRow(
                    label = "Race mode",
                    value = if (lyricsMode == UserPrefs.LYRICS_MODE_PREFERRED) {
                        "Preferred provider"
                    } else {
                        "Auto (race all)"
                    },
                    onClick = { /* opens dropdown */ }
                )
                if (lyricsMode == UserPrefs.LYRICS_MODE_PREFERRED) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsClickRow(
                        label = "Preferred provider",
                        value = preferredProvider,
                        onClick = { /* opens dropdown */ }
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsClickRow(
                    label = "Clear lyrics cache",
                    value = null,
                    onClick = viewModel::clearLyricsCache
                )
                if (BuildConfig.DEBUG) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    SettingsToggleRow(
                        label = "Show lyrics race winner",
                        subtitle = "Debug caption below lyrics",
                        checked = lyricsDebug,
                        onChecked = viewModel::setLyricsDebug
                    )
                }
            }
        }

        // ─── Appearance ───
        item {
            SettingsSectionCard(title = "Appearance") {
                SettingsClickRow(
                    label = "Theme",
                    value = "Dark (enforced)",
                    onClick = { }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsToggleRow(
                    label = "Haptic feedback",
                    subtitle = "Vibrate on taps, likes, and playback controls",
                    checked = hapticsEnabled,
                    onChecked = viewModel::setHapticsEnabled
                )
            }
        }

        // ─── Account ───
        item {
            SettingsSectionCard(title = "Account") {
                SettingsClickRow(
                    label = if (session != null) "Sign out" else "Sign in",
                    value = session?.displayName ?: "Guest",
                    onClick = {
                        if (session != null) viewModel.signOut() else onSignIn()
                    }
                )
            }
        }

        // ─── About ───
        item {
            SettingsSectionCard(title = "About") {
                SettingsClickRow(
                    label = "Waveyy",
                    value = "v${viewModel.versionName}",
                    onClick = { }
                )
            }
        }
    }
}

/** Rounded card section with a small uppercase title above it. */
@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(content = content)
        }
    }
}

/** Row with label + value on the right + chevron. Tappable. */
@Composable
private fun SettingsClickRow(
    label: String,
    value: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** Row with label + subtitle on the left, switch on the right. */
@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String?,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}
