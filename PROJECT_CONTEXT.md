# PROJECT_CONTEXT — Waveyy Android

<!-- AUTO-GENERATED. Hand-curated sections are preserved across regenerations. -->
Last refreshed: 2026-10-10T00:00:00Z
Refreshed by: opencode/muse-spark
Git HEAD: dd43952 — ci: use a persistent debug signing key so APKs update cleanly

---

## 1. TL;DR

Waveyy Android (`/root/Waveyy-Android`) is a native Android client for the Waveyy music app, mirroring the Next.js web app at `/root/Waveyy` (read-only reference). Kotlin + Compose Material3 + Media3 + Hilt, reusing the existing Next.js/Socket.IO/Supabase backend via Retrofit proxies. Working: home feed, hybrid JioSaavn/YT-Music search, full player with queue/lyrics/sheets, playlists (incl. guest-local), rooms, settings, debug probes. In-progress/polish: visual verification is device-only (no emulator here); Supabase project is dead so auth runs in guest mode; YT Music streams are bot-walled (search works, playback doesn't).

---

## 2. Tech stack

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | 2.1.20 |
| Build | Gradle + AGP | 8.11.1 / 8.9.1 (KSP 2.1.20-1.0.31) |
| UI | Jetpack Compose + Material 3 | BOM 2025.12.01, nav-compose 2.8.9 |
| DI | Hilt | 2.56.2 (+ hilt-navigation-compose 1.2.0) |
| Networking | Retrofit + OkHttp + kotlinx-serialization | 2.11.0 / 4.12.0 / 1.8.1 |
| Local DB | Room | 2.6.1 (v2, has Migration 1→2) |
| Key-value | DataStore Preferences | 1.1.7 |
| Playback | Media3 (ExoPlayer + MediaSession) | 1.6.1 |
| Images | Coil | 3.1.0 |
| Auth/Backend | Supabase Kotlin SDK (dead project → guest mode) | 3.2.6 |
| Realtime | socket.io-client-java | 2.1.0 |
| Blur | Haze (glass header + bottom chrome) | 1.6.10 |
| Static analysis | Detekt + ktlint | 1.23.8 / 12.1.2 |
| Logging | Timber | 5.0.1 |

Full dependency list: `gradle/libs.versions.toml`. SDKs: compile/target 36, min 26. No Gradle wrapper in repo (CI provisions Gradle).

---

## 3. Architecture

MVVM + repository pattern. UI (Compose screens) → Hilt ViewModels (mostly activity-scoped via `shared*ViewModel()` helpers so mini/full player never diverge) → `@Singleton` repositories → Retrofit proxy APIs (`JiosaavnApi` over `BuildConfig.API_BASE_URL`) or Room/DataStore. Playback is split: `PlayerViewModel` owns the domain queue + pre-play stream resolution, `PlaybackConnection` (singleton) owns the MediaController, `QueueManager` maps `Song → MediaItem`. Search races JioSaavn vs InnerTube with per-source circuit breakers; lyrics race 7 providers with scored selection.

Package root: `com.waveyy.app`.

---

## 4. Directory index

`/` (repo root) — gradle config, CI, docs. Key files: settings.gradle.kts, build.gradle.kts.
`app/` — the single app module. Key files: build.gradle.kts (BuildConfig fields).
`di/` (6 files) — Hilt modules: network, database, datastore, player, lyrics, dispatchers.
`data/local/` (5) — Room entities + database (v2). Key files: WaveyyDatabase.kt.
`data/local/dao/` (4) — LyricsDao, LyricsCacheDao, HistoryDao, PlayLogDao.
`data/local/datastore/` (2) — UserPrefs (all prefs), SearchHistoryStore.
`data/lyrics/` (11) — provider interface + 7 providers + race orchestrator/repository.
`data/remote/` (4) — Retrofit APIs: JioSaavnApi, LyricsApi, SaavnDirectApi, SpotifyRecApi.
`data/remote/auth/` (2) — Supabase client provider, Google OAuth.
`data/remote/dto/` (4) — tolerant DTOs + FlexibleLongSerializer.
`data/remote/mapper/` (2) — SongMapper (normalization), RecommendedSongsParser.
`data/remote/ytmusic/` (3) — first-party InnerTube client + API + DTOs.
`data/repository/` (6) — Song, Playlist, Auth, Lyrics, ListeningHistory, YtMusic repos.
`domain/model/` (7) — Song, Album, Artist, Playlist, MusicSource, LyricLine, RoomState.
`domain/usecase/` (3) — HybridMerge, SearchRanking, TrendingCurator.
`playback/` (3) — PlaybackConnection, PlaybackService, QueueManager.
`realtime/` (2) — RoomSocket, RoomRepository (Listen Together).
`ui/` (1) — MainViewModel (tab reselect bus) + sharedMainViewModel().
`ui/album|artist|auth|discover|home|library|navigation|placeholder|player|search|settings|theme|together/` — screens + VMs.
`ui/components/` (22) — design-system primitives (Waveyy*), cards, skeletons, nav pill.
`ui/debug/` (3) — component/theme galleries, YT Music probe (DEBUG builds only).
`util/` (8) — AudioQualitySelector, LyricsParser, PlayCountFormatter, Hinglish, HtmlDecoder, ImageUrl, AccentExtractor, QueueUtils.

---

## 5. File index

| File | LOC | Exports | Purpose |
|---|---|---|---|
| MainActivity.kt | 46 | MainActivity | Activity entry, sets Compose content |
| WaveyyApp.kt | 20 | WaveyyApp | Hilt application, Timber plant |
| data/local/HistoryEntity.kt | 17 | HistoryEntity | History row, songId PK |
| data/local/LyricsCacheEntity.kt | 21 | LyricsCacheEntity | Race-winner cache row |
| data/local/LyricsEntity.kt | 17 | LyricsEntity | Proxy lyrics cache row |
| data/local/PlayLogEntity.kt | 15 | PlayLogEntity | Play-event log row |
| data/local/WaveyyDatabase.kt | 28 | WaveyyDatabase | Room DB v2 + lyricsCacheDao |
| data/local/dao/HistoryDao.kt | 27 | HistoryDao | Recent/upsert/trim history |
| data/local/dao/LyricsCacheDao.kt | 28 | LyricsCacheDao | Winner get/upsert/prune/clear |
| data/local/dao/LyricsDao.kt | 24 | LyricsDao | Proxy cache get/put/prune |
| data/local/dao/PlayLogDao.kt | 23 | PlayLogDao | Play-log insert/recent/trim |
| data/local/datastore/SearchHistoryStore.kt | 71 | SearchHistoryStore | Search history (cap 10) |
| data/local/datastore/UserPrefs.kt | 282 | UserPrefs | All typed DataStore prefs |
| data/lyrics/GeniusProvider.kt | 70 | GeniusProvider | Genius search+scrape (key-gated) |
| data/lyrics/LrclibProvider.kt | 43 | LrclibProvider | LRCLIB direct (keyless) |
| data/lyrics/LyricsOvhProvider.kt | 58 | LyricsOvhProvider | lyrics.ovh plain (keyless) |
| data/lyrics/LyricsProvider.kt | 21 | LyricsProvider, LyricsResult | Provider contract + result sealed |
| data/lyrics/LyricsRaceOrchestrator.kt | 81 | LyricsRaceOrchestrator | Parallel scored race, 3s |
| data/lyrics/LyricsRaceRepository.kt | 92 | LyricsRaceRepository | Race + 7-day winner cache |
| data/lyrics/MusixmatchProvider.kt | 49 | MusixmatchProvider | Musixmatch (key-gated) |
| data/lyrics/NetEaseProvider.kt | 70 | NetEaseProvider | NetEase search+lyric (keyless) |
| data/lyrics/ProviderHttp.kt | 57 | ProviderHttp, Call.await | Shared OkHttp plumbing |
| data/lyrics/QqMusicProvider.kt | 66 | QqMusicProvider | QQ search+lyric (keyless) |
| data/lyrics/TekstowoProvider.kt | 54 | TekstowoProvider | Tekstowo scraper (keyless) |
| data/remote/JiosaavnApi.kt | 61 | JiosaavnApi | Proxy surface (path= dispatch) |
| data/remote/LyricsApi.kt | 24 | LyricsApi | Lyrics proxy surface |
| data/remote/SaavnDirectApi.kt | 24 | SaavnDirectApi | Direct deploy (often dead) |
| data/remote/SpotifyRecApi.kt | 21 | SpotifyRecApi | Spotify-mapped recs |
| data/remote/auth/GoogleOAuth.kt | 13 | GoogleOAuth | Google OAuth provider |
| data/remote/auth/SupabaseClientProvider.kt | 38 | SupabaseClientProvider | Supabase client holder |
| data/remote/dto/ChartsDto.kt | 28 | ChartsResponse | Charts envelope |
| data/remote/dto/LyricsDto.kt | 20 | LyricsDto, SpotifyRecResponse | Lyrics + rec envelopes |
| data/remote/dto/SaavnDtos.kt | 143 | SongDto, SongsResponse | Tolerant Saavn DTOs |
| data/remote/dto/Serializers.kt | 33 | FlexibleLongSerializer | String/number-tolerant Long |
| data/remote/mapper/RecommendedSongsParser.kt | 38 | RecommendedSongsParser | Rec JSON parser |
| data/remote/mapper/SongMapper.kt | 125 | SongMapper, AlbumMapper | DTO→domain normalization |
| data/remote/ytmusic/InnerTubeClient.kt | 91 | InnerTubeClient | WEB_REMIX POST transport |
| data/remote/ytmusic/YtMusicApi.kt | 187 | YtMusicApi | Search/player parsing |
| data/remote/ytmusic/dto/YtDtos.kt | 25 | YtTrack, YtStream | InnerTube DTOs |
| data/repository/AuthRepository.kt | 186 | AuthRepository, AuthUser | Session + profile ops |
| data/repository/ListeningHistoryRepository.kt | 68 | ListeningHistoryRepository | Cloud listening log |
| data/repository/LyricsRepository.kt | 65 | LyricsRepository | Legacy proxy lyrics cache |
| data/repository/PlaylistRepository.kt | 278 | PlaylistRepository | Playlists, guest-local fallbacks |
| data/repository/SongRepository.kt | 236 | SongRepository | Hybrid search + refresh |
| data/repository/YtMusicRepository.kt | 57 | YtMusicRepository | YT search/stream facade |
| di/DataStoreModule.kt | 14 | DataStoreModule | DataStore binding |
| di/DatabaseModule.kt | 55 | DatabaseModule | DB + DAOs + migration |
| di/DispatchersModule.kt | 25 | IoDispatcher | IO dispatcher qualifier |
| di/LyricsModule.kt | 36 | LyricsModule | Provider list binding |
| di/NetworkModule.kt | 102 | NetworkModule | OkHttp/Retrofit/APIs |
| di/PlayerModule.kt | 33 | PlayerModule | ExoPlayer binding |
| domain/model/Album.kt | 10 | Album | Album domain model |
| domain/model/Artist.kt | 11 | Artist | Artist + tops model |
| domain/model/LyricLine.kt | 13 | LyricLine, CachedLyrics | Lyric line + cache payload |
| domain/model/MusicSource.kt | 13 | MusicSource | JIOSAAVN / YTMUSIC enum |
| domain/model/Playlist.kt | 12 | Playlist | Playlist domain model |
| domain/model/RoomState.kt | 17 | RoomState | Together room state |
| domain/model/Song.kt | 35 | Song, AudioStream | Core song + stream models |
| domain/usecase/HybridMerge.kt | 37 | HybridMerge | JS+YT merge/dedupe/sort |
| domain/usecase/SearchRanking.kt | 146 | SearchRanking | Normalize/score/titleKey |
| domain/usecase/TrendingCurator.kt | 135 | TrendingCurator | Home curation pipeline |
| playback/PlaybackConnection.kt | 392 | PlaybackConnection, PlaybackState | MediaController owner + state |
| playback/PlaybackService.kt | 78 | PlaybackService | MediaSession foreground service |
| playback/QueueManager.kt | 126 | QueueManager, QueueSource | Song→MediaItem mapping |
| realtime/RoomRepository.kt | 250 | RoomRepository | Room CRUD + sync |
| realtime/RoomSocket.kt | 116 | RoomSocket | Socket.IO wrapper |
| ui/MainViewModel.kt | 44 | MainViewModel | Tab reselect event bus |
| ui/album/AlbumScreen.kt | 103 | AlbumScreen | Album detail + tracks |
| ui/album/AlbumViewModel.kt | 56 | AlbumViewModel | Album loading logic |
| ui/artist/ArtistScreen.kt | 219 | ArtistScreen | Artist page + podium |
| ui/artist/ArtistViewModel.kt | 88 | ArtistViewModel | Artist loading logic |
| ui/auth/AuthCallbackActivity.kt | 55 | AuthCallbackActivity | OAuth deep-link handler |
| ui/auth/AuthViewModel.kt | 106 | AuthViewModel | Login/signup logic |
| ui/auth/LoginScreen.kt | 100 | LoginScreen | Login form |
| ui/auth/SignupScreen.kt | 92 | SignupScreen | Signup form |
| ui/components/AdaptiveImage.kt | 44 | AdaptiveImage | Coil image with fallback |
| ui/components/AlbumCard.kt | 85 | AlbumCard | Album tile |
| ui/components/ArtistCard.kt | 70 | ArtistCard | Artist tile |
| ui/components/FloatingNavPill.kt | 109 | FloatingNavPill | 3-tab bottom pill |
| ui/components/HeroPlayButton.kt | 28 | HeroPlayButton | Hero play affordance |
| ui/components/NextCard.kt | 66 | NextCard | Up-next tile |
| ui/components/Skeleton.kt | 314 | SkeletonBox/Rows/Home/Lyrics/Artist | Shimmer skeleton kit |
| ui/components/SongCard.kt | 85 | SongCard | Song tile |
| ui/components/SongRow.kt | 103 | SongRow | Song list row |
| ui/components/WaveyyBadge.kt | 22 | WaveyyBadge | Small label badge |
| ui/components/WaveyyBottomSheet.kt | 30 | WaveyyBottomSheet | Sheet wrapper |
| ui/components/WaveyyButton.kt | 74 | WaveyyButton | Branded button |
| ui/components/WaveyyCommandPalette.kt | 72 | WaveyyCommandPalette | Command palette |
| ui/components/WaveyyCredenza.kt | 39 | WaveyyCredenza | Responsive dialog/sheet |
| ui/components/WaveyyDialog.kt | 41 | WaveyyDialog | Dialog wrapper |
| ui/components/WaveyyDropdownMenu.kt | 53 | WaveyyDropdownMenu | Dropdown menu |
| ui/components/WaveyyErrorPanel.kt | 47 | WaveyyErrorPanel | Error + retry panel |
| ui/components/WaveyyInput.kt | 43 | WaveyyInput | Text input |
| ui/components/WaveyyLayout.kt | 63 | WaveyyLayout | Layout helpers |
| ui/components/WaveyyProgress.kt | 36 | WaveyyProgress | Progress indicator |
| ui/components/WaveyyScaffold.kt | 113 | WaveyyScaffold | App scaffold |
| ui/components/WaveyySlider.kt | 39 | WaveyySlider | Branded slider |
| ui/components/WaveyyTooltip.kt | 38 | WaveyyTooltip | Tooltip wrapper |
| ui/debug/ComponentGalleryScreen.kt | 136 | ComponentGalleryScreen | DEBUG component gallery |
| ui/debug/ThemeGalleryScreen.kt | 150 | ThemeGalleryScreen | DEBUG theme gallery |
| ui/debug/YtMusicDebugScreen.kt | 171 | YtMusicDebugScreen | DEBUG YT probe |
| ui/discover/DiscoverScreen.kt | 83 | DiscoverScreen | Discover grid |
| ui/discover/DiscoverViewModel.kt | 65 | DiscoverViewModel | Discover loading |
| ui/home/GreetingHeader.kt | 50 | GreetingHeader | Time greeting (unused now) |
| ui/home/HomeHeader.kt | 103 | HomeHeader | Glass title + icon buttons |
| ui/home/HomeScreen.kt | 456 | HomeScreen | Home grid + sections |
| ui/home/HomeViewModel.kt | 307 | HomeViewModel | Home feed orchestration |
| ui/library/AddToPlaylistButton.kt | 50 | AddToPlaylistButton | Playlist add button |
| ui/library/AddToQueueButton.kt | 28 | AddToQueueButton | Queue add button |
| ui/library/LibraryViewModel.kt | 125 | LibraryViewModel | Playlist CRUD + likes |
| ui/library/LikeSongButton.kt | 45 | LikeSongButton | Shared heart button |
| ui/library/MusicViewModel.kt | 102 | MusicViewModel | Shared liked-ids source |
| ui/library/PlaylistDetailScreen.kt | 156 | PlaylistDetailScreen | Playlist detail + remove |
| ui/library/PlaylistsScreen.kt | 258 | PlaylistsScreen | Playlist 2-col grid |
| ui/navigation/DeepLinks.kt | 44 | DeepLinks | Deep-link table |
| ui/navigation/Routes.kt | 33 | Routes | Route constants + helpers |
| ui/navigation/WaveyyNavHost.kt | 272 | WaveyyNavHost | Root nav + chrome overlay |
| ui/placeholder/PlaceholderScreen.kt | 31 | PlaceholderScreen | Stub screens |
| ui/player/BottomBarPlayer.kt | 105 | BottomBarPlayer | Legacy bar (unused) |
| ui/player/FullLyricsScreen.kt | 256 | FullLyricsScreen | Full lyrics + seek |
| ui/player/FullPlayerScreen.kt | 524 | FullPlayerScreen | Now-playing page |
| ui/player/LyricsSheet.kt | 216 | LyricsSheet | Lyrics bottom sheet |
| ui/player/MiniPlayer.kt | 259 | MiniPlayer | Floating mini-player pill |
| ui/player/PlayerCards.kt | 303 | LyricsCard, ArtistCard | Player cards |
| ui/player/PlayerChrome.kt | 39 | rememberChromeBottomReserve | Bottom reserve helper |
| ui/player/PlayerDialogs.kt | 185 | SpeedDialog et al | Player dialogs |
| ui/player/PlayerViewModel.kt | 667 | PlayerViewModel | Queue + lyrics + errors |
| ui/player/QueueSheet.kt | 92 | QueueSheet | Queue bottom sheet |
| ui/player/SharedPlayer.kt | 20 | sharedPlayerViewModel | Activity-scoped accessor |
| ui/player/SongInfoSheet.kt | 200 | SongInfoSheet | 12-action sheet |
| ui/player/SongMetadataSheet.kt | 182 | SongMetadataSheet | Metadata viewer sheet |
| ui/search/SearchScreen.kt | 762 | SearchScreen | Search + filters + history |
| ui/search/SearchViewModel.kt | 292 | SearchViewModel | Search pipeline |
| ui/settings/SettingsScreen.kt | 450 | SettingsScreen | Sectioned settings |
| ui/theme/Color.kt | 99 | WaveyyPalette etc | Color tokens |
| ui/theme/DynamicAccent.kt | 43 | rememberAlbumAccent | Album accent hook |
| ui/theme/Motion.kt | 38 | Motion | Motion tokens |
| ui/theme/Shape.kt | 17 | WaveyyShapes | Shape tokens |
| ui/theme/Theme.kt | 61 | WaveyyTheme | Theme entry |
| ui/theme/Type.kt | 136 | WaveyyType | Type scale |
| ui/together/TogetherScreen.kt | 96 | TogetherScreen | Room create/join UI |
| ui/together/TogetherViewModel.kt | 61 | TogetherViewModel | Room state |
| util/AccentExtractor.kt | 56 | AccentExtractor | Artwork color extract |
| util/AudioQualitySelector.kt | 71 | AudioQualitySelector | Best-stream picker |
| util/Hinglish.kt | 114 | Hinglish | Romanization toggle |
| util/HtmlDecoder.kt | 36 | HtmlDecoder | HTML entity decode |
| util/ImageUrl.kt | 41 | ImageUrl | Image URL helpers |
| util/LyricsParser.kt | 71 | LyricsParser | LRC parse + sanitize |
| util/PlayCountFormatter.kt | 28 | PlayCountFormatter | Compact counts ("" for 0) |
| util/QueueUtils.kt | 38 | QueueUtils | Queue helpers |

---

## 6. Public contracts

### `Song` (in `domain/model/Song.kt`)

```kotlin
@Serializable
data class Song(
    val id: String, val name: String, val artist: String, val album: String,
    val imageUrl: String, val durationSec: Long, val playCount: Long,
    val streams: List<AudioStream>, val year: String = "",
    val source: MusicSource = MusicSource.JIOSAAVN,
    val primaryArtistId: String = ""
)
```

### `PlaybackState` (in `playback/PlaybackConnection.kt`)

```kotlin
data class PlaybackState(
    val mediaId: String? = null, val isPlaying: Boolean = false,
    val positionMs: Long = 0L, val durationMs: Long = 0L,
    val isBuffering: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val error: String? = null, val mediaIndex: Int = 0
)
```

### `QueueSource` (in `playback/QueueManager.kt`)

```kotlin
sealed interface QueueSource {
    val songs: List<Song>
    data class SearchResults(override val songs: List<Song>) : QueueSource
    data class Album(override val songs: List<Song>) : QueueSource
    data class Artist(override val songs: List<Song>) : QueueSource
    data class Playlist(override val songs: List<Song>) : QueueSource
    data class Recommendations(override val songs: List<Song>) : QueueSource
    data class HomeFeed(override val songs: List<Song>) : QueueSource
    data class Custom(override val songs: List<Song>) : QueueSource
}
```

### `LyricsResult` (in `data/lyrics/LyricsProvider.kt`)

```kotlin
interface LyricsProvider {
    val name: String; val enabled: Boolean
    suspend fun fetchLyrics(artist: String, title: String, duration: Long): LyricsResult?
}
sealed class LyricsResult {
    data class Synced(val lrc: String) : LyricsResult()
    data class Plain(val text: String) : LyricsResult()
    object Instrumental : LyricsResult()
}
```

### `MusicSource` (in `domain/model/MusicSource.kt`)

```kotlin
@Serializable
enum class MusicSource { JIOSAAVN, YTMUSIC }
```

### Routes (in `ui/navigation/Routes.kt`)

```kotlin
SEARCH = "search?query={query}"; ARTIST = "artist/{id}"
PLAYER = "player/{id}"; FULL_LYRICS = "lyrics/{id}"
withId(route, id); withQuery(query)
```

### Key ViewModel APIs

```kotlin
// PlayerViewModel.kt
val queue: StateFlow<List<Song>>; val playerError: StateFlow<String?>
fun playSongs(songs, startIndex = 0)
fun playSource(source: QueueSource, startIndex: Int = 0)
fun advance(); fun rewind(); fun playSong(song: Song)
fun loadLyrics(song, bypassCache = false)
fun artistFlow(id): Flow<Artist?>
// PlaybackConnection.kt
val state: StateFlow<PlaybackState>
suspend fun playSongs/playItems/playSingleItem/appendItems/next/previous/pause/stopPlayback/replaceItem/seekTo
fun currentQueueSongs(): List<Song>; fun clearError(); fun refreshState()
// SearchViewModel.kt
fun submitQuery(newQuery, addToHistory = false); fun refresh()
```

---

## 7. Key flows

### Startup

1. Entry: `MainActivity.kt:30` — setContent hosts app
2. Step: `WaveyyNavHost.kt:66` — builds nav graphs, shared VMs, chrome overlay
3. Step: `HomeScreen` collects `HomeViewModel` (Loading → skeleton → Success)
4. Ends at: home grid rendered; `PlaybackConnection` reconnects silently in init

### Playback (tap → sound)

1. Entry: `SearchScreen.kt` SongResultRow tap → `QueueSource.SearchResults`
2. Step: `PlayerViewModel.kt:222` playSource — resolves tapped song only, plays queue-of-one via `connection.playSingleItem`
3. Step: background backfill resolves rest, dedupes, guards generation, `appendItems`
4. Step: `QueueManager` maps Song→MediaItem (best URL via AudioQualitySelector)
5. Ends at: ExoPlayer playing; `PlaybackState` published; mini-player shows

### Search

1. Entry: `SearchScreen.kt:104` text + 300ms debounce → `SearchViewModel.kt:92` submitQuery
2. Step: `SearchViewModel.kt:115` search → `:134` load — 3 query variants fan out via `SongRepository.searchSongDtos`
3. Step: id-dedupe then title-dedupe (`SearchRanking.titleKey`); all-fail → Error; empty → lyrics-phrase fallback
4. Ends at: `SearchUiState.Success/Error`; history saved only on explicit submit

### Lyrics

1. Entry: `FullPlayerScreen.kt:122` LaunchedEffect → `PlayerViewModel.kt:471` loadLyrics
2. Step: `LyricsRaceRepository` checks 7-day cache, else `LyricsRaceOrchestrator.kt:34` race (7 providers, 3s window, scored pick)
3. Ends at: `LyricsUiState` Ready/Plain/Instrumental/Empty; winner logged via Timber

### Auth

1. Entry: LoginScreen → AuthViewModel → AuthRepository (Supabase)
2. Note: Supabase project is dead (NXDOMAIN) → guest mode; likes/playlists fall back to DataStore-local
3. Ends at: session null (guest) or signed-in user

### Queue management

1. Single taps: playSource (append-only backfill, generation-guarded)
2. Wholesale: playSongs/playItems (setMediaItems replace)
3. Skip: advance()/rewind() refresh target URL, swap in place, then step
4. Dedup: id-first everywhere; title-dedupe in search + setQueueFromSource
5. Ends at: domain queue and controller queue index-identical

### Background playback

1. `PlaybackService` (MediaSessionService) hosts ExoPlayer; survives swipe-away
2. Reopen: new activity-scoped `PlayerViewModel` restores queue from `connection.currentQueueSongs()` when empty
3. `stopMusicOnClose` pref → `onTaskRemoved` stops service if enabled
4. Ends at: mini-player reappears with correct song/position/state

### Listen Together

1. Entry: TogetherScreen → TogetherViewModel → RoomSocket (Socket.IO) + RoomRepository
2. Ends at: room state synced across participants

---

## 8. Build & CI

- **Local build**: not supported. All builds run in GitHub Actions. Never install Gradle/SDK locally; repo has no wrapper.
- **CI**: `.github/workflows/android.yml` — jobs: `build` (decode debug keystore, local.properties, lint, detekt, unit tests, assembleDebug, upload APK + reports), `release` (keystore-or-skip, bundleRelease, upload AAB).
- **Artifacts**: `waveyy-debug-apk` per run; persistent debug keystore so updates install cleanly.
- **Secrets required**: DEBUG_KEYSTORE_BASE64, API_BASE_URL, SOCKET_URL, SUPABASE_URL, SUPABASE_ANON_KEY, SAAVN_API_URL, RELEASE_KEYSTORE_BASE64/PASSWORD/ALIAS/PASSWORD. Local values via `local.properties` (gitignored; see `local.properties.example`).
- **Build time**: ~4–9 min on ubuntu-latest (gradle cache dependent).
- **Known CI issues**: GitHub API returns stale/cached runs data (poll with cache-busters); network flakes (HTTP 000) need retries; sparse-checkout log line in output is default noise, not filtering.

---

## 9. Known issues & gotchas

### No local builds, ever

**Symptom**: temptation to run `./gradlew` locally.
**Cause**: no SDK/wrapper here by design.
**Workaround**: push and read CI logs only (`PLAYBOOK` in agent memory).

### KSP crashes on private vals inside @Module

**Symptom**: KSP NPE naming a `Migration` field.
**Cause**: Dagger/Hilt superficial validation chokes on module-level vals.
**Fix**: move to file top-level — `di/DatabaseModule.kt:19`.

### Hilt List injection needs @JvmSuppressWildcards

**Symptom**: `List<LyricsProvider> cannot be provided`.
**Fix**: `List<@JvmSuppressWildcards LyricsProvider>` — `data/lyrics/LyricsRaceOrchestrator.kt`.

### Dagger "could not be resolved" = bad import

**Symptom**: KSP reports an unresolvable *type*.
**Cause**: usually a wrong import path in the importing file, not a missing dep.
**Fix**: check the import first (`data/lyrics/LyricsRaceRepository.kt` precedent).

### Lint gates that bite

**Symptom**: green code fails `lintDebug`/`detekt`.
**Fixes**: unused imports (detekt); flow operators must sit inside `remember` (`ui/player/PlayerChrome.kt`); `LocalContext as Activity` banned — use `LocalActivity as? ComponentActivity` (all three shared-VM helpers); `PaddingValues(horizontal, bottom)` overload doesn't exist — use start/end/top/bottom.

### Supabase is dead; app runs as guest

**Symptom**: auth/history backend calls fail (NXDOMAIN).
**Cause**: project `kszxplrnmbwfwqwdojwm.supabase.co` deleted.
**Workaround**: guest DataStore likes/playlists; backend-missing card for PGRST205.

### Direct Saavn deployment is dead

**Symptom**: 404 DEPLOYMENT_NOT_FOUND on `jiosaavanapi-tan`.
**Workaround**: `SongRepository.refreshSong` falls back to proxy search-by-name with exact id match; `searchAlbums` returns empty.

### YT Music streams are bot-walled

**Symptom**: InnerTube `player` returns UNPLAYABLE for all tracks.
**Cause**: no po_token/account; search works, streams don't.
**Status**: YT layer degrades to silent no-op; JioSaavn carries playback.

### Backend `{error}` JSON masquerades as empty

**Symptom**: outage renders as "No results".
**Fix**: `SongsResponse.error` throws IOException → Error state + Retry (`data/repository/SongRepository.kt`, `ui/search/SearchViewModel.kt`).

### Shared VMs must be activity-scoped

**Symptom**: mini/full player hearts or queues disagree.
**Cause**: destination-scoped `hiltViewModel()` fragments state.
**Fix**: `sharedPlayerViewModel/sharedMusicViewModel/sharedMainViewModel` via `LocalActivity`.

### Queue desync defenses (layered)

**Symptom**: repeats / wrong song on skip (historically persistent).
**Fixes**: id+title dedup; tapped-only resolve + append backfill; `originalTappedId` filter (refresh-id-drift); generation guard; `mediaIndex` display with id fallback; advance() refresh-and-swap. See `PlayerViewModel.kt:222`, `PlaybackConnection.kt`.

### Mini-player hidden flag

**Symptom**: swipe-down hide must not stick across songs.
**Fix**: `miniPlayerHidden` DataStore flag, cleared on every play entry (`PlayerViewModel.kt`).

### Room needs explicit Migration on version bump

**Symptom**: runtime crash after adding entity.
**Fix**: `MIGRATION_1_2` at file top-level in `di/DatabaseModule.kt` (NOT inside the module object).

### Stale GitHub API data

**Symptom**: runs/jobs endpoints return old results.
**Workaround**: `Cache-Control: no-cache` + `t=$(date +%s)`; confirm via commit check-runs endpoint.

---

## 10. Phase history

<!-- BEGIN HAND-CURATED: PHASE HISTORY -->
<!-- This block is preserved across regenerations. To add a phase, edit
inside the markers or paste the diff to the regen prompt. -->
### Phase 1 — Scaffold + CI (7bc5f66, 52b6a4f): Gradle project, no wrapper, CI workflow, README.
### Phase 2 — Design system (df5296d): colors, type, shape, motion, theme gallery.
### Phase 3 — Networking + data (ea02612, 70f2a77): Retrofit, Room, repos, DI, utils, tests.
### Phase 4 — Playback service (44e1f56): Media3 service, connection, queue manager.
### Phase 5 — Navigation shell (4c0e9a3): nav graphs, adaptive scaffold, primitives.
### Phase 6 — Supabase auth (03ea9e6): password/Google OAuth, login/signup.
### Phase 7 — Home + search + artist (10ee91e): curation, ranking, detail screens.
### Phase 8 — Player UI (8c8d9f5, e053164): full/mini player, lyrics sheet, queue, guest-mode tolerance.
### Phase 9 — Playlists + history (95a90ef): CRUD, library screens, liked list.
### Phase 10 — Listen Together (00cb1c3): Socket.IO rooms.
### Phase 11 — Deep links + baseline profile (c4f6518, b5f2005).
### Phase 12 — Release signing template (6a72de1).
### Phase 13 — Parity pass (e7db518).
### Phase 14 — Expressive overhaul (1f15c4f, b7543fb, 2935559, f994405, 4fcafd6).
### Phase 15 — Dynamic accent + search overhaul (4762c6e, f994405).
### Phase 16 — Playback desync fix (8710330, 962d60f).
### Phase 17 — Home redesign + search history (c4b2121, 174055e).
### Phase 18 — Compact full player (d69b6b2, ea8347b).
### Phase 19 — Info sheet + discover (f5a949e, b519e12).
### Phase 20 — Sticky headers + skeletons (bf217a4, 0e29d2a).
### Phase 21 — InnerTube hybrid search (5b52b51, a83f22c…338dea0): first-party YT client, race/merge, debug probe.
### Phase 22 — Mini-player/likes/gestures, racing lyrics (7 providers), skeletons, search fixes, artist nav (7156cc7, c7e7b92, 2328860, 407a441).
### Phase 23 — Continue refresh fallback, queue restore on reopen, header rebuild, mini-player reserve (effe491, 3e329f7, 474cb59, f083786, c9a6cbf).
### Phase 24 — Scrollable player + cards, anchored mini-player, header log (4dbd998, bc36df4, 3537377).
### Phase 25 — Skeleton visibility, debounced search, header spacer (143cac8).
### Phase 26 — Drop lyrics auto-scroll, header center alignment (8efd316).
### Phase 27 — Queue dedup + stream freshness + index tracking (e8bfaa2).
### Phase 28 — Mini-player anchored gestures + stop-on-hide (0318199).
### Phase 29 — Animated lyrics gradient + fixed transport colors (120af55).
### Phase 30 — Header inset, mini wiggle, skeleton padding (64dfa99).
### Phase 31 — Title-based dedup for search + queue (4d6e9cd).
### Phase 32 — Skeleton inset, header space, badge removal, gesture spring, lyrics blur, accent bg (e617efb).
### Phase 33 — 3-dot/info swap + metadata viewer (b39f3a5).
### Phase 34 — Player layout, ripple discipline, nav reserve, instant playback (15168e3, 4dccd0f).
### Phase 35 — Search redesign, greeting removal, settings rework, lyrics race fix, toggles (6c6768e, 2308e96).
### Phase 36 — Player fit, gentler blur, chrome reserve fix (02bfcc2).
### Phase 37 — Placeholder/history/lyrics-search/alignment/header/skeleton/settings/playback-service prefs (809696f).
### Phase 38 — Lyrics polish + like color, artist skeleton + counts, playlist grid + guest playlists (a95a846, 4964cde, 67dedc0, 1912664).
### Phase 39 — Haze glass header + bottom chrome (041a066, 5115f63, 0315592, 003af64, b80898b, ca10d40, e147371, b70456c).
### Phase 40 — Player spacing, skeleton alignment, lyrics gate, queue append, home fallback, glass bottom nav (bedd3c4, 7ca7709, abfc6c7, b93fd69).
### Phase 41 — Lyrics load trigger, header frost, flat bottom chrome (143e6f1).
### Phase 42 — Append-only play flow, id-drift filter, generation guard (deacdbc).
### Phase 43 — MiniPlayer SimpMusic pill restyle (7b4d574).
### Debug signing — persistent keystore via CI secret (dd43952): APKs update cleanly.
<!-- END HAND-CURATED: PHASE HISTORY -->

---

## 11. Open work / TODO

- No TODO/FIXME/XXX/HACK markers in source (grep clean).
- Visual verification is device-only: header glass, bottom chrome, lyrics blur, skeletons, gestures all need on-device confirmation.
- Prefer-video toggle is stored but not wired into YT filtering (stub).
- `GreetingHeader.kt` retained but unused (may be restored).
- `BottomBarPlayer.kt` legacy, unused.
- `LyricsSheet.kt` unused after player scroll rework (kept).
- Supabase backend dead — re-provision or remove backend code paths.
- YT Music playback blocked on bot-wall (needs po_token strategy or account auth).

---

## 12. Hand-written notes

<!-- BEGIN HAND-CURATED: NOTES -->
<!-- This block is preserved verbatim across regenerations. Add design
decisions, informal specs, or context that isn't derivable from code. -->
<!-- END HAND-CURATED: NOTES -->

---

## 13. How to use this file

If you are an LLM starting a new session on this project:

1. Read this file top to bottom. It is the fastest way to build context.
2. For any code change, read the actual source file — this doc is a map,
not a substitute.
3. When you complete a phase or make a nontrivial change, append an entry
to section 10 (Phase history). Use the format shown.
4. When you discover a new gotcha, add it to section 9.
5. When the codebase changes significantly, re-run the regen prompt (kept
alongside this file, see below).

```
