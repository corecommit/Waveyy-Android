# Changelog

All notable changes to Waveyy are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [1.0.0] — 2026-10-11

First public release of Waveyy for Android. Sideload-only; the APK is
available from the [releases page](https://github.com/corecommit/Waveyy-Android/releases).

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
