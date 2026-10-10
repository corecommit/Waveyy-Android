# TASK: Regenerate PROJECT_CONTEXT.md for Waveyy-Android

You are a codebase analyst. Scan `/root/Waveyy-Android` and produce
`/root/Waveyy-Android/PROJECT_CONTEXT.md`. This file is the single source of
truth for any LLM (or human) picking up this project cold.

## Golden rules

1. **Never delete hand-written sections.** Sections marked
`<!-- BEGIN HAND-CURATED -->` … `<!-- END HAND-CURATED -->` must be
preserved verbatim if they already exist in the file. You are only
permitted to append to them (e.g. add a new decision log entry).
2. **Never invent.** Every claim traces to a real file, real line, or real
git commit. If you can't verify something, write "unknown" — do not guess.
3. **Be idempotent.** Running this twice on an unchanged codebase produces
byte-identical auto sections.
4. **Do not modify any other file.** Only write `PROJECT_CONTEXT.md`.
5. **Compact > complete.** Aim for < 800 lines. Skip bodies; index instead.

## Step 1 — Scan

Run these commands from `/root/Waveyy-Android` and capture the output:

```bash
# Project shape
find app/src/main/java -name "*.kt" -type f | sort
find app/src/main/res -type d | sort
find . -maxdepth 2 -name "*.gradle.kts" -o -name "*.toml" -o -name "*.properties" | grep -v node_modules | grep -v build/ | sort

# LOC per file (for the file index)
find app/src/main/java -name "*.kt" -exec wc -l {} + | sort -rn | head -50

# Recent commits (for the phase history)
git log --oneline -100

# Current branch + status
git branch --show-current
git status --short

# Deps (parse manually)
cat gradle/libs.versions.toml
cat app/build.gradle.kts

# CI
ls -la .github/workflows/ 2>/dev/null
cat .github/workflows/*.yml 2>/dev/null

# Public contract surface
grep -rn "^interface \|^sealed \|^data class \|^enum class " app/src/main/java/ | sort

# Known TODOs
grep -rn "TODO\|FIXME\|XXX\|HACK" app/src/main/java/ | grep -v "^Binary"

# Current test suite
find app/src/test app/src/androidTest -name "*.kt" 2>/dev/null | sort

# Recent file modifications (last 7 days)
find app/src/main/java -name "*.kt" -mtime -7 -type f | sort

# How many files in each top-level package
for d in app/src/main/java/com/waveyy/app/*/; do
  count=$(find "$d" -name "*.kt" | wc -l)
  echo "$count $d"
done | sort -rn
```

## Step 2 — Produce PROJECT_CONTEXT.md

The file must contain these sections, in this order. Section headers are
literal — do not rename them.

```markdown
# PROJECT_CONTEXT — Waveyy Android

<!-- AUTO-GENERATED. Hand-curated sections are preserved across regenerations. -->
Last refreshed: <ISO timestamp>
Refreshed by: <agent/model identifier if known, else "manual">
Git HEAD: <short hash> — <commit subject>

---

## 1. TL;DR

<!-- 4-6 sentences. What is this app? What platform? What backend? What is
     its current state (working features vs. in-progress)? -->

---

## 2. Tech stack

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | ... |
| Build | Gradle + AGP | ... |
| UI | Jetpack Compose + Material 3 Expressive | ... |
| DI | Hilt | ... |
| Networking | Retrofit + OkHttp + kotlinx-serialization | ... |
| Local DB | Room | ... |
| Key-value | DataStore | ... |
| Playback | Media3 (ExoPlayer + MediaSession) | ... |
| Images | Coil | ... |
| Auth/Backend | Supabase Kotlin SDK | ... |
| Realtime | socket.io-client-java | ... |
| Static analysis | Detekt + ktlint | ... |

Full dependency list: `gradle/libs.versions.toml`.

---

## 3. Architecture

<!-- 5-10 lines on the pattern (MVVM + Clean), package layering, and how a
     request flows from UI → ViewModel → Repository → Remote/Local. -->

Package root: `com.waveyy.app`.

---

## 4. Directory index

<!-- One line per top-level package. Format:
     `path/` — purpose. Key files: a.kt, b.kt. -->

---

## 5. File index

<!-- Every .kt file under app/src/main/java. Format:
     | File | LOC | Primary exports | Purpose |
     Sorted by path. Purpose is a single clause, max 12 words. -->

| File | LOC | Exports | Purpose |
|---|---|---|---|
| ... | ... | ... | ... |

---

## 6. Public contracts

<!-- Every interface, sealed class, data class over ~5 fields, and enum
     that other code depends on. Include the full signature block. This is
     what a new LLM needs to write code against without reading the impl. -->

### `QueueSource` (in `playback/QueueManager.kt`)

```kotlin
sealed interface QueueSource {
    val songs: List<Song>
    data class SearchResults(override val songs: List<Song>) : QueueSource
    ...
}
```

<!-- repeat for each contract -->

---

## 7. Key flows

<!-- For each of: Startup, Playback, Search, Lyrics, Auth, Queue
management, Background playback, Listen Together. Format:

```
 ### Flow name
 1. Entry: <file:line> — what triggers
 2. Step: <file:line> — what happens
 3. ...
 Ends at: <file:line> — final state

 Every step cites a file:line. -->
```

---

## 8. Build & CI

- **Local build**: not supported. All builds run in GitHub Actions.
- **CI**: `.github/workflows/android.yml` — jobs: <list>
- **Artifacts**: debug APK uploaded to Actions run artifact
- **Secrets required**: <list>
- **Build time**: ~X min on ubuntu-latest
- **Known CI issues**: <list>

---

## 9. Known issues & gotchas

<!-- This is the most valuable section for a new LLM. List every non-obvious
behavior, workaround, or trap. Format:

```
 ### <short title>
 **Symptom**: ...
 **Cause**: ...
 **Workaround / fix**: <file:line>

 Include: stale URLs, duplicate handling, hardcoded quirks, races,
 ordering dependencies, state that must survive restarts. -->
```

---

## 10. Phase history

<!-- Append-only. Never delete past entries. Newest at the bottom.
Format per phase:

```
 ### Phase N — <title>
 - Commit: <hash>
 - Summary: <1-3 sentences>
 - Files touched: <count, or list if <5>
 -->
```

<!-- BEGIN HAND-CURATED: PHASE HISTORY -->
<!-- This block is preserved across regenerations. To add a phase, edit
inside the markers or paste the diff to the regen prompt. -->
<!-- END HAND-CURATED: PHASE HISTORY -->

---

## 11. Open work / TODO

<!-- What is currently broken, in progress, or planned next. Derived from
TODOs in code + git log analysis. Format: bullet list. -->

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
