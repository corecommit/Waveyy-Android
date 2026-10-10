# Waveyy Android

Native Android client for Waveyy — a music streaming app built with
**Jetpack Compose** and **Material 3 Expressive**.

Backed by the existing Waveyy Next.js API routes and Socket.IO server. This
app does **not** reimplement the backend.

## Tech Stack

- Kotlin 2.1.20 · AGP 8.9.1 · Gradle 8.11.1 (provisioned by CI)
- Jetpack Compose (BOM 2025.12.01) + Material 3 + Material 3 Adaptive
- Navigation Compose
- Media3 (ExoPlayer + MediaSessionService) for playback
- Hilt for dependency injection
- Retrofit + OkHttp + kotlinx-serialization
- Room + DataStore for local persistence
- Coil for image loading
- Supabase Kotlin SDK (auth + PostgREST)
- socket.io-client-java for Listen Together
- Detekt + ktlint for static analysis

## Architecture

MVVM + Clean Architecture. Two Gradle modules: `:app` (the app) and
`:baselineprofile` (Baseline Profile generator for startup optimization).

```text
com.waveyy.app/
├── di/               Hilt modules
├── data/             remote (Retrofit), local (Room, DataStore), repository
├── domain/           models + use cases
├── playback/         PlaybackService, PlaybackConnection, QueueManager
├── realtime/         Socket.IO room client
├── ui/               theme, navigation, components, feature screens
└── util/             audio-quality, lyrics parser, formatters
```

## Build

**All builds run in GitHub Actions.** This repo does not contain a Gradle
wrapper and is not intended to be built locally. Every push to `main`
triggers `.github/workflows/android.yml`, which produces a debug APK under
the run's **Artifacts** on the Actions tab.

### Required secrets (Settings → Secrets and variables → Actions)

Build secrets:
- `API_BASE_URL` — Waveyy Next.js API base (e.g. `https://<host>/api/`)
- `SOCKET_URL`   — Socket.IO server URL
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SAAVN_API_URL` (optional) — direct JioSaavn deployment for song fetch
  and album search; blank disables those two calls

Optional API keys (leave blank to disable the corresponding lyrics provider):
- `MUSIXMATCH_API_KEY`
- `GENIUS_API_KEY`

Signing secrets:
- `DEBUG_KEYSTORE_BASE64` — base64-encoded persistent debug keystore, so
  successive APKs install cleanly over each other
- `RELEASE_KEYSTORE_BASE64` — base64-encoded release keystore (skip the
  release job by leaving this blank)
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Discord notifications:
- `DISCORD_WEBHOOK_WORKFLOWS` — receives build / release failure messages
- `DISCORD_WEBHOOK_RELEASES`  — receives APK / AAB ready messages

## Notifications

The workflow posts to two Discord channels:

- **Workflows** — build or release failures, with branch, commit, and a
  link to the failed run. Cancelled runs also notify here.
- **Debug Releases** — successful debug APK builds and successful release
  AAB builds, with a link to the run's Artifacts section.

To wire up a channel: create a webhook via Discord
(Server Settings → Integrations → Webhooks → New Webhook), copy the URL,
and paste it into the matching GitHub Actions secret.

### Downloading the APK

1. Open the **Actions** tab.
2. Click the latest successful run of **Android CI**.
3. Scroll to **Artifacts** → download `waveyy-debug-apk`.
4. Unzip, install on device.

Or grab it from the Discord notification in **Debug Releases** — click the
embed title, then download from the run's Artifacts section.

## Backend

The backend (Next.js + Socket.IO) lives in the Waveyy web repo and is used
unchanged. See that repo's README for deployment.

## Play Store listing (draft)

- Title: Waveyy — Sound like real waves
- Short description: High-fidelity streaming, playlists, lyrics, and
  real-time Listen Together rooms.
- Full description: Waveyy for Android streams millions of tracks with a
  dark, immersive Material 3 interface. Build playlists, follow lyrics in
  sync (with Hinglish romanization), sync across devices with
  Listen Together rooms, and take downloads offline.
- Feature graphic: placeholder — 1024x500 banner with the Waveyy mark on
  `#0B0B0F` (see `res/drawable/ic_launcher_foreground.xml`).
- Screenshot checklist: Home rails, Search + top result, Artist page,
  Full player (Now Playing / Lyrics tabs), Together room, dark theme only.
- Category: Music & Audio. Content rating: Everyone (lyrics unfiltered —
  confirm before submission).
- Crashlytics: not integrated (pending explicit approval).

## License

MIT
