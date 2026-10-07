# Waveyy Android

Native Android client for Waveyy — a music streaming app built with
**Jetpack Compose** and **Material 3 Expressive**.

Backed by the existing Waveyy Next.js API routes and Socket.IO server. This
app does **not** reimplement the backend.

## Tech Stack

- Kotlin 2.1 · AGP 8.7 · Gradle 8.9
- Jetpack Compose (BOM 2025.x) + Material 3 + Material 3 Adaptive
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

MVVM + Clean Architecture, single `:app` module.

```
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
- `API_BASE_URL` — Waveyy Next.js API base (e.g. `https://<host>/api/`)
- `SOCKET_URL`   — Socket.IO server URL
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `SAAVN_API_URL` (optional) — direct JioSaavn deployment for song fetch +
  album search; blank disables those two calls

### Downloading the APK
1. Open the **Actions** tab.
2. Click the latest successful run of **Android CI**.
3. Scroll to **Artifacts** → download `waveyy-debug-apk`.
4. Unzip, install on device.

## Backend

The backend (Next.js + Socket.IO) lives in the Waveyy web repo and is used
unchanged. See that repo's README for deployment.

## License

MIT
