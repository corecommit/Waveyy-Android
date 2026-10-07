package com.waveyy.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Normalized top-charts envelope produced by our `/api/jiosaavn`
 * `topCharts` proxy (`asTopChartsPayload` in the web route).
 */
@Serializable
data class ChartsResponse(
    val success: Boolean = false,
    val data: ChartsData? = null
)

/** Charts payload; every key carries the same song list. */
@Serializable
data class ChartsData(
    val topCharts: List<SongDto> = emptyList(),
    val charts: List<SongDto> = emptyList(),
    val results: List<SongDto> = emptyList(),
    val trending: TrendingBlock? = null
)

/** `trending: {songs}` block of [ChartsData]. */
@Serializable
data class TrendingBlock(
    val songs: List<SongDto> = emptyList()
)
