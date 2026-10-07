# Waveyy-Android

Native Android client for Waveyy (Jetpack Compose + Material 3), backed by the
existing Next.js API routes and Socket.IO servers. The web app at
`/root/Waveyy` is the reference implementation and is treated as read-only.

## Prerequisites

- JDK 17 (Gradle/AGP run on 17; `org.gradle.java.home` must point at it)
- Android SDK with `platforms;android-35` + `build-tools;35.0.0`
- Copy `local.properties.example` to `local.properties` and fill in
  `API_BASE_URL`, `SOCKET_URL`, `SUPABASE_URL`, `SUPABASE_ANON_KEY`
  (dummy values keep debug builds compiling)

## Build / verify (Phase 1 gate)

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:detekt
```

## Phase status

- [x] Phase 1 — Scaffolding (this commit)
- [ ] Phase 2 — Design System
- [ ] Phase 3 — Networking & Data
- [ ] Phase 4 — Playback (Media3)
- [ ] Phase 5 — Navigation Shell + Components
- [ ] Phase 6 — Auth (Supabase Kotlin)
- [ ] Phase 7 — Home / Search / Album / Artist
- [ ] Phase 8 — Player UI + Lyrics
- [ ] Phase 9 — Playlists / History / Library
- [ ] Phase 10 — Listen Together (Socket.IO)
- [ ] Phase 11 — Deep Links, Adaptive, Accessibility, Baseline Profile
- [ ] Phase 12 — Release Prep

## Web-only features intentionally not ported

Service worker / PWA manifest, Discord RPC (`scripts/discord-rpc.cjs`,
`/api/discord/activity`), and the commented-out `mobile-menu.jsx`.
