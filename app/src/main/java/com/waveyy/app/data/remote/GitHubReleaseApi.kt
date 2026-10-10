package com.waveyy.app.data.remote

import com.waveyy.app.data.remote.dto.GitHubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Headers

/** GitHub releases endpoint. User-Agent is required by the GitHub API. */
interface GitHubReleaseApi {

    /** Returns the latest published (non-draft, non-prerelease) release. */
    @Headers(
        "Accept: application/vnd.github+json",
        "User-Agent: Waveyy-Android"
    )
    @GET("repos/corecommit/Waveyy-Android/releases/latest")
    suspend fun latestRelease(): GitHubReleaseDto
}
