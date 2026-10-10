package com.waveyy.app.data.repository

import com.waveyy.app.BuildConfig
import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.remote.GitHubReleaseApi
import com.waveyy.app.domain.model.AppUpdate
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GitHub releases accessor. Fetches the latest release and, when its tag
 * is newer than the running [BuildConfig.VERSION_NAME], returns an
 * [AppUpdate]. Never throws to callers — network failures yield null so
 * the updater stays invisible.
 */
@Singleton
class UpdateRepository @Inject constructor(
    private val api: GitHubReleaseApi,
    private val userPrefs: UserPrefs
) {

    /**
     * Checks for a newer release. Returns null when:
     *   - the network call fails,
     *   - no published release exists yet,
     *   - the latest release is not newer than the running version,
     *   - no APK asset is attached.
     */
    suspend fun checkForUpdate(): AppUpdate? {
        return runCatching {
            val release = api.latestRelease()
            if (release.draft || release.prerelease) return@runCatching null
            val tag = release.tagName.removePrefix("v").trim()
            if (tag.isEmpty()) return@runCatching null
            if (compareVersions(tag, BuildConfig.VERSION_NAME) <= 0) {
                return@runCatching null
            }
            val apk = release.assets.firstOrNull { asset ->
                asset.name.endsWith(".apk", ignoreCase = true)
            } ?: return@runCatching null
            AppUpdate(
                version = tag,
                tag = release.tagName,
                title = release.name?.takeIf { it.isNotBlank() } ?: "Waveyy $tag",
                changelog = release.body.orEmpty(),
                downloadUrl = apk.browserDownloadUrl,
                apkFileName = apk.name.ifBlank { "waveyy-update.apk" },
                apkSizeBytes = apk.size
            )
        }.onFailure {
            Timber.tag("UpdateRepo").d("check failed: ${it.message}")
        }.getOrNull()
    }

    /** Marks [tag] as dismissed so the popup won't re-show for it. */
    suspend fun markSeen(tag: String) {
        userPrefs.setLastSeenUpdateTag(tag)
    }

    /** True when the popup for [tag] has already been dismissed. */
    suspend fun wasSeen(tag: String): Boolean {
        return userPrefs.lastSeenUpdateTag() == tag
    }

    /** Stamps the last successful check time. */
    suspend fun markChecked(nowMs: Long = System.currentTimeMillis()) {
        userPrefs.setLastUpdateCheckMs(nowMs)
    }

    /**
     * Semantic version comparison. Strips leading `v`, splits on `.`,
     * missing components are 0. Returns positive when [a] > [b].
     */
    private fun compareVersions(a: String, b: String): Int {
        fun parts(v: String): List<Int> = v.removePrefix("v")
            .split(".")
            .mapNotNull { it.toIntOrNull() }
        val av = parts(a)
        val bv = parts(b)
        val len = maxOf(av.size, bv.size)
        for (i in 0 until len) {
            val x = av.getOrElse(i) { 0 }
            val y = bv.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
