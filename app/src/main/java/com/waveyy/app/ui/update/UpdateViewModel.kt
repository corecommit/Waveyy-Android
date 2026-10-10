package com.waveyy.app.ui.update

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waveyy.app.data.repository.UpdateRepository
import com.waveyy.app.domain.model.AppUpdate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Update state exposed to the popup and Settings. */
data class UpdateState(
    /** Non-null when a newer release is available. */
    val latest: AppUpdate? = null,
    /** True when the popup for [latest] should currently be visible. */
    val showPopup: Boolean = false,
    /** True while downloading. */
    val downloading: Boolean = false,
    /** 0f..1f download progress. */
    val progress: Float = 0f,
    /** Transient error message from a failed download attempt. */
    val error: String? = null
)

/**
 * Checks GitHub for a newer release on init and every 45 minutes while
 * the view model is alive. Shows the popup once per version; dismissing
 * persists so it never re-shows for that tag.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: UpdateRepository,
    private val installer: UpdateInstaller
) : ViewModel() {

    private val mutableState = MutableStateFlow(UpdateState())
    /** Update state. */
    val state: StateFlow<UpdateState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { check(showIfUnseen = true) }
        viewModelScope.launch {
            while (true) {
                delay(CHECK_INTERVAL_MS)
                check(showIfUnseen = true)
            }
        }
    }

    /** Fetches the latest release. */
    private suspend fun check(showIfUnseen: Boolean) {
        val update = repository.checkForUpdate() ?: run {
            mutableState.value = mutableState.value.copy(latest = null, showPopup = false)
            return
        }
        repository.markChecked()
        val seen = repository.wasSeen(update.tag)
        mutableState.value = mutableState.value.copy(
            latest = update,
            showPopup = showIfUnseen && !seen
        )
    }

    /** Dismisses the popup and marks the tag as seen. */
    fun dismiss() {
        val update = mutableState.value.latest ?: return
        mutableState.value = mutableState.value.copy(showPopup = false)
        viewModelScope.launch { repository.markSeen(update.tag) }
    }

    /** Reopens the popup manually (Settings row tap). */
    fun showPopup() {
        if (mutableState.value.latest != null) {
            mutableState.value = mutableState.value.copy(showPopup = true)
        }
    }

    /** Download the APK and trigger the system install prompt. */
    fun downloadAndInstall() {
        val update = mutableState.value.latest ?: return
        if (mutableState.value.downloading) return
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                downloading = true,
                progress = 0f,
                error = null
            )
            val result = installer.downloadAndInstall(
                context = context,
                downloadUrl = update.downloadUrl,
                fileName = update.apkFileName,
                onProgress = { p ->
                    mutableState.value = mutableState.value.copy(progress = p)
                }
            )
            mutableState.value = mutableState.value.copy(
                downloading = false,
                error = result.exceptionOrNull()?.message
            )
        }
    }

    /** Clears a download error. */
    fun clearError() {
        mutableState.value = mutableState.value.copy(error = null)
    }

    private companion object {
        /** Recheck cadence while the process is alive. */
        const val CHECK_INTERVAL_MS = 45L * 60L * 1000L
    }
}
