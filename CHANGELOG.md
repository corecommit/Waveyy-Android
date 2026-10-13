# Changelog

All notable changes to Waveyy are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.1.0] — 2026-10-13

### Added

- **Home redesign** — Quick Picks list at the top (six tracks drawn from
  your history + recommendations), followed by four horizontal rails:
  Recently played, Made for You, Artists you listen to, Albums for you.
- **Song radio** — when the queue runs low, Waveyy now fetches related
  tracks (JioSaavn suggestions + same-artist search, with YT radio for
  YT Music seeds) and appends them so playback keeps flowing instead of
  ending. Covers, karaoke, live, and slowed variants are filtered out.
- **Lyrics search** — search a lyric line like "at least we are together"
  and Waveyy finds the matching song via LRCLIB's phrase index.
- **Liked Songs screen** — full-screen list of liked tracks with play-all
  and shuffle, reachable from a pinned card in the Library.
- **History screen** — recently played list, backed by local storage.
- **In-app updates** — Waveyy checks for new releases on launch and every
  45 minutes, shows a changelog popup, and downloads + installs the APK
  in place.
- **Auto-hiding bottom chrome** — the nav pill and mini player slide down
  after 5 seconds of inactivity and return on touch.
- **Together room codes** — rooms are now 8-character alphanumeric codes
  you can read aloud. Join by typing a code or pasting the shared link.

### Changed

- **Search results** are cleaner: songs only, ranked by combined title +
  artist + popularity so the correct track beats covers and remixes.
- **Player controls** — white play/pause button on both the full player
  and lyrics screens; prev/next now use FastRewind / FastForward icons.
- **Mini player** — emerald pill with a circular progress ring around the
  album art. Drag slides the pill; the heart and play buttons stay fixed.
- **Lyrics view** — the active line is centered, the last line parks at
  the bottom, and nothing highlights before the first line or after the
  last one ends.
- **Auth screens** — cleaner layout, password visibility toggle, next/
  done keyboard flow, back arrow.
- **Settings** — working dropdowns for Stream quality and Lyrics; library
  cleanup rows now show an undoable snackbar. Reworded items so the
  source toggles read as plain English.

### Fixed

- Skipping to the next/previous song is instant — stream URLs are
  prefetched in the background while the current song plays.
- The previous song now stops the moment you tap a new one instead of
  overlapping during load.
- Update dialog no longer shows the version twice.
- Search-based lyrics providers verify the matched title + artist before
  fetching, so wrong-song lyrics ("Someday" by TheFatRat appearing for
  "Unity") no longer slip through.
- LRCLIB now retries without the duration parameter, and falls back to
  its fuzzy search endpoint, so songs that used to return "lyrics not
  available" now resolve.
- Artwork loading is resilient on flaky connections — placeholder shown
  while loading, two fallback URLs tried on failure, graceful music-note
  icon as last resort. Fixed the "sometimes the art doesn't load" issue.
- History screen no longer leaves dead space around each tile.

### Internal

- InnerTube (YouTube Music) client uses updated API key + client version.
- YT Music search now filters to songs only via the `params` field.
- Coil configured with a 256 MB disk cache and crossfade.
- Release workflow tags the changelog into the GitHub release body.

## [1.0.0] — 2026-10-11

First public release of Waveyy for Android. Sideload-only; the APK is
available from the [releases page](https://github.com/corecommit/Waveyy-Android/releases).

### Fixed

- Mini player: horizontal drag slides only art + title + artist while
  the heart and play buttons stay fixed; content clips inside the pill.
- Search loading skeleton fills more of the screen while loading.
- Login/Signup back button no longer sits behind the status bar.

### Added

- **Home** — curated trending grid with Continue, Made for You, Popular
  Artists/Albums, and New Releases rails. Pull-to-refresh with a minimum
  600 ms spinner so the gesture always gives visible feedback.
- **Search** — hybrid JioSaavn + YouTube Music search with per-source
  circuit breakers, query variant fan-out, and relevance-based ranking
  (title + artist + popularity). Filter chips for Tracks / Albums /
  Artists / Playlists.
- **Player** — full-screen now-playing with album-art-tinted gradient,
  seek bar, shuffle, repeat, sleep timer, playback speed, and per-song
  queue. Mini player with axis-locked swipe gestures (left = next,
  right = previous, down = hide) and a circular progress ring.
- **Lyrics** — 7-provider race (lrclib, NetEase, QQ Music, lyrics.ovh,
  Teksto, Musixmatch, Genius) with scored winner selection, 30-day
  winner cache, cache-first rendering, and Hinglish romanization.
- **Library** — Liked Songs (unified across guest and signed-in users),
  user-created playlists, and per-song add/remove.
- **History** — recently played grid backed by local Room storage.
- **Listen Together** — create a room and share the 8-character code,
  or join a friend's room by entering their code. Real-time playback
  sync over Socket.IO.
- **Downloads** — save the best available stream to `Music/Waveyy` via
  MediaStore, with a success/failure toast.
- **Settings** — audio quality, source toggles, lyrics loading style,
  haptics, battery-optimization shortcut, library cleanup with undo.
- **Deep links** — `waveyy://song/{id}`, `waveyy://album/{id}`,
  `waveyy://artist/{id}`, `waveyy://playlist/{id}`, `waveyy://room/{code}`.
- **Share sheet receiver** — share a `waveyy://song/{id}` link from any
  app; the song is added to "play next".
- **In-app updates** — checks GitHub releases on launch and every 45
  minutes; popup shows the changelog with a one-tap download + install
  flow via `PackageInstaller`.

### Design

- Emerald-on-black theme (`#10B981` primary, `#09090B` background).
- Material 3 Expressive; glass header (Haze), floating nav pill, and
  auto-hiding bottom chrome after 5 seconds of inactivity.
- Bricolage Grotesque typeface throughout.

### Known limitations

- YouTube Music streams are blocked by YouTube's bot-wall; search works,
  playback falls back to JioSaavn.
- Supabase project is unprovisioned; the app runs as guest by default.
- Facebook / Google OAuth flows are wired but unproven against a live
  backend.
