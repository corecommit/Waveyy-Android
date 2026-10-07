package com.waveyy.app.data.repository

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
 */
@Singleton
class SongRepository @Inject constructor(
    private val api: JiosaavnApi,
    private val spotifyApi: SpotifyRecApi,
    @Named("saavnDirect") private val direct: SaavnDirectApi?
) {

    /** Searches songs, mirroring `getSongsByQuery` (limit 20 like the web). */
    fun searchSongs(query: String, limit: Int = 20): Flow<List<Song>> = flow {
        val body = api.searchSongs(query = query, limit = limit)
        val dtos = body.data?.results.orEmpty() + body.data?.songs.orEmpty()
        emit(SongMapper.toDomainList(dtos.distinctBy { it.id }))
    }.flowOn(Dispatchers.IO)

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
        emit((body.data?.results.orEmpty() + body.data?.songs.orEmpty()).distinctBy { it.id })
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
}
