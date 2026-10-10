package com.waveyy.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** GitHub release as returned by `GET /repos/{owner}/{repo}/releases/latest`. */
@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
    val prerelease: Boolean = false,
    val draft: Boolean = false
)

/** One downloadable asset attached to a GitHub release. */
@Serializable
data class GitHubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    val size: Long = 0L
)
