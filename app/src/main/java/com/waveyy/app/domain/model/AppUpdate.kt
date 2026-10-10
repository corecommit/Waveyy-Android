package com.waveyy.app.domain.model

/** A newer release of Waveyy that is available to install. */
data class AppUpdate(
    /** Version string without leading `v`, e.g. `1.0.1`. */
    val version: String,
    /** Original tag from the release, e.g. `v1.0.1`. Used as an identity key. */
    val tag: String,
    /** Human-readable release title (falls back to version when absent). */
    val title: String,
    /** Release description (markdown body). */
    val changelog: String,
    /** Direct URL of the APK asset to download. */
    val downloadUrl: String,
    /** APK asset filename (used for the cache file). */
    val apkFileName: String,
    /** Total APK size in bytes, or 0 when unknown. */
    val apkSizeBytes: Long
)
