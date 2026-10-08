package com.waveyy.app.data.repository

import com.waveyy.app.data.local.datastore.UserPrefs
import com.waveyy.app.data.remote.JiosaavnApi
import com.waveyy.app.data.remote.SaavnDirectApi
import com.waveyy.app.data.remote.SpotifyRecApi
import com.waveyy.app.data.remote.dto.SongDto
import com.waveyy.app.data.remote.mapper.AlbumMapper
import com.waveyy.app.data.remote.mapper.ArtistMapper
import com.waveyy.app.data.remote.mapper.RecommendedSongsParser
import com.waveyy.app.data.remote.mapper.SongMapper
import com.waveyy.app.domain.model.Album
import com.waveyy.app.domain.model.Artist
import com.waveyy.app.domain.model.Song
import com.waveyy.app.domain.usecase.HybridMerge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.selects.select
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Catalog access: search, charts, songs, albums, artists, recommendations.
 * Every stream is cold and runs on IO.
 *
 * The proxy surface ([JiosaavnApi]) is primary. [direct] covers the two
 * calls with no proxy equivalent (direct song fetch, album search) and is
 * null when `SAAVN_API_URL` is unconfigured — those flows then stay empty.
 *
 * Search races JioSaavn against YouTube Music (when enabled): fast mode
 * returns the first non-empty success, complete mode merges both with
 * [HybridMerge]. Per-source circuit breakers disable a repeatedly failing
 * source for 10 minutes.
 */
@Singleton
class SongRepository @Inject constructor(
    private val api: JiosaavnApi,
    private val spotifyApi: SpotifyRecApi,
    @Named("saavnDirect") private val direct: SaavnDirectApi?,
    private val ytmusic: YtMusicRepository? = null,
    private val userPrefs: UserPrefs? = null
) {

    private var jsFails = 0
    private var ytFails = 0
    private var jsDisabledUntil = 0L
    private var ytDisabledUntil = 0L

    /** Searches songs, racing JioSaavn against YouTube Music. */
    fun searchSongs(query: String, limit: Int = 20): Flow<List<Song>> = flow {
        emit(raceSearch(query, limit))
    }.flowOn(Dispatchers.IO)

    private suspend fun raceSearch(query: String, limit: Int): List<Song> = coroutineScope {
        val prefs = userPrefs
        val ytEnabled = prefs?.let { runCatching { it.ytmusicEnabledFlow().first() }.getOrDefault(true) } ?: true
        val preferFast = prefs?.let { runCatching { it.preferFastFlow().first() }.getOrDefault(true) } ?: true
        val ytClient = ytmusic
        val jsOpen = System.currentTimeMillis() >= jsDisabledUntil
        val ytOpen = ytEnabled && ytClient != null && System.currentTimeMillis() >= ytDisabledUntil
        if (!ytOpen) return@coroutineScope jsSearch(query, limit)
        if (!jsOpen) {
            val yt = runCatching { ytClient?.searchSongs(query).orEmpty() }
            recordYt(yt)
            return@coroutineScope yt.getOrDefault(emptyList())
        }
        val jsDef = async { runCatching { jsSearchRaw(query, limit) }.also(::recordJs) }
        val ytDef = async { runCatching { ytClient?.searchSongs(query).orEmpty() }.also(::recordYt) }
        if (preferFast) {
            val first = select<Pair<String, Result<List<Song>>>> {
                jsDef.onAwait { "js" to it }
                ytDef.onAwait { "yt" to it }
            }
            val (winner, result) = first
            val songs = result.getOrDefault(emptyList())
            if (songs.isNotEmpty()) {
                Timber.d("HybridSearch winner=$winner q=$query")
                jsDef.cancel()
                ytDef.cancel()
                return@coroutineScope songs
            }
            Timber.d("HybridSearch winner empty, falling back q=$query")
            val other = if (winner == "js") ytDef else jsDef
            return@coroutineScope other.await().getOrDefault(emptyList())
        }
        val js = jsDef.await().getOrDefault(emptyList())
        val yt = ytDef.await().getOrDefault(emptyList())
        Timber.d("HybridSearch complete js=${js.size} yt=${yt.size} q=$query")
        HybridMerge.merge(query, js, yt)
    }

    private suspend fun jsSearch(query: String, limit: Int): List<Song> {
        return runCatching { jsSearchRaw(query, limit) }.also(::recordJs).getOrDefault(emptyList())
    }

    private suspend fun jsSearchRaw(query: String, limit: Int): List<Song> {
        val body = api.searchSongs(query = query, limit = limit)
        body.error?.takeIf { it.isNotBlank() }?.let { throw java.io.IOException(it) }
        val dtos = body.data?.results.orEmpty() + body.data?.songs.orEmpty()
        val songs = SongMapper.toDomainList(dtos.distinctBy { it.id })
        Timber.d("JsSearch q=$query dtos=${dtos.size} songs=${songs.size}")
        return songs
    }

    @Synchronized
    private fun recordJs(result: Result<List<Song>>) {
        record(result, isJs = true)
    }

    @Synchronized
    private fun recordYt(result: Result<List<Song>>) {
        record(result, isJs = false)
    }

    private fun record(result: Result<List<Song>>, isJs: Boolean) {
        if (result.isSuccess) {
            if (isJs) jsFails = 0 else ytFails = 0
            return
        }
        val fails = if (isJs) jsFails + 1 else ytFails + 1
        if (isJs) jsFails = fails else ytFails = fails
        if (fails >= CIRCUIT_FAILS) {
            val until = System.currentTimeMillis() + CIRCUIT_MILLIS
            if (isJs) {
                jsDisabledUntil = until
                jsFails = 0
            } else {
                ytDisabledUntil = until
                ytFails = 0
            }
            Timber.d("HybridSearch circuit open isJs=$isJs")
        }
    }

    /** Trending charts, mirroring `topChartsPublic()`. */
    fun topCharts(): Flow<List<Song>> = flow {
        val data = api.topCharts().data
        val dtos = data?.topCharts.orEmpty() + data?.charts.orEmpty() +
            data?.results.orEmpty() + (data?.trending?.songs.orEmpty())
        emit(SongMapper.toDomainList(dtos.distinctBy { it.id }))
    }.flowOn(Dispatchers.IO)

    /** Suggestions for a song, mirroring `getSongsSuggestions`. */
    fun suggestions(id: String): Flow<List<Song>> = flow {
        emit(RecommendedSongsParser.parse(api.recommendedSongs(songId = id)))
    }.flowOn(Dispatchers.IO)

    /** Album detail, mirroring `getAlbumById`. */
    fun albumById(id: String): Flow<Album?> = flow {
        emit(api.albumDetails(albumId = id).data?.let(AlbumMapper::toDomain))
    }.flowOn(Dispatchers.IO)

    /** Artist detail, mirroring `getArtistById`. */
    fun artistById(id: String): Flow<Artist?> = flow {
        emit(api.artistDetails(artistId = id).data?.let(ArtistMapper::toDomain))
    }.flowOn(Dispatchers.IO)

    /** Raw song DTOs for callers needing pre-normalization shapes. */
    fun searchSongDtos(query: String, limit: Int = 20): Flow<List<SongDto>> = flow {
        val body = api.searchSongs(query = query, limit = limit)
        body.error?.takeIf { it.isNotBlank() }?.let { throw java.io.IOException(it) }
        val dtos = (body.data?.results.orEmpty() + body.data?.songs.orEmpty()).distinctBy { it.id }
        Timber.d("SongDtos q=$query dtos=${dtos.size}")
        emit(dtos)
    }.flowOn(Dispatchers.IO)

    /** Album search via the direct deployment (empty when unconfigured). */
    fun searchAlbums(query: String): Flow<List<Album>> = flow {
        val body = direct?.searchAlbums(query)
        emit(body?.data?.results.orEmpty().mapNotNull(AlbumMapper::toDomain))
    }.flowOn(Dispatchers.IO)

    /** Direct song fetch (empty when unconfigured). */
    fun songById(id: String): Flow<Song?> = flow {
        emit(direct?.songById(id)?.data?.let(SongMapper::toDomain))
    }.flowOn(Dispatchers.IO)

    /** Spotify-mapped recommendations, mirroring `getSpotifyRecommendations`. */
    fun spotifyRecommendations(name: String, artist: String, limit: Int = 12): Flow<List<Song>> = flow {
        emit(SongMapper.toDomainList(spotifyApi.recommend(name, artist, limit).data))
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val CIRCUIT_FAILS = 5
        private const val CIRCUIT_MILLIS = 10L * 60L * 1000L
    }
}
