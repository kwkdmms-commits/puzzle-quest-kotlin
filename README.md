# Puzzle Quest — Native Android (Kotlin / Jetpack Compose)

A direct port of the **Puzzle Quest** web game to a native Android app.
Replaces the Capacitor + React + TypeScript stack with **Kotlin + Jetpack Compose**.

---

## What this app is

A drag-and-drop image puzzle:

- **25 levels**, each tied to one of the original cute illustrations.
- Levels 1–4, 6–9, 11–14, 16–19, 21–24 use a **4×4** grid (16 pieces).
- Levels **5, 10, 15, 20, 25** use a **6×6** grid (36 pieces) — these are the "challenge" levels.
- Pieces are shuffled until none start in their correct spot. Drop a piece on its correct cell to **snap + lock** it (cyan glow). Drop anywhere else to **swap** with whatever's there.
- **Time limit:** 2:30 for 4×4, 4:00 for 6×6.
- **Hint** shows the full image. **More Time** adds 60 s (once per level). **Restart** confirms before resetting.
- Sound effects are **synthesized at runtime** via `AudioTrack` PCM — same waveform shapes as the original Web Audio code.
- Progress (current level, total puzzles solved, best moves & best time per grid size) is persisted in `SharedPreferences`, included in Android Auto Backup.

---

## Project layout

```
PuzzleQuest/
├── app/
│   ├── build.gradle.kts                  ← AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/images/                ← 25 compressed WebP puzzle images (≈2.5 MB total)
│       ├── java/com/pingsama/puzzlequest/
│       │   ├── MainActivity.kt           ← Compose host + menu/game routing
│       │   ├── PuzzleQuestApp.kt         ← Application class, owns Audio + Leaderboard singletons
│       │   ├── audio/
│       │   │   └── AudioManager.kt       ← PCM synthesis for the 5 SFX
│       │   ├── game/
│       │   │   ├── GameEngine.kt         ← Pure logic: shuffle, placePiece, win check
│       │   │   ├── LevelSystem.kt        ← Level → grid size + time limit + asset
│       │   │   └── LeaderboardManager.kt ← SharedPreferences progress / best scores
│       │   └── ui/
│       │       ├── theme/                ← Color, Type, Theme
│       │       ├── Components.kt         ← PillButton, PopupCard, RoundCloseButton
│       │       ├── LevelImage.kt         ← Loads + slices the level WebP
│       │       ├── MainMenuScreen.kt
│       │       ├── PuzzleBoard.kt        ← Drag-and-drop grid (the heart of the game)
│       │       └── GameScreen.kt         ← Header, board, buttons, popups
│       └── res/
│           ├── values/                   ← strings, colors, themes, ic_launcher_background
│           ├── mipmap-anydpi-v26/        ← Adaptive launcher icon
│           ├── drawable/                 ← Vector launcher foreground
│           └── xml/                      ← Backup rules
├── build.gradle.kts                      ← Top-level
├── settings.gradle.kts                   ← Project name + module include
├── gradle/wrapper/                       ← gradle-wrapper.jar + properties (Gradle 8.14.3)
├── gradlew, gradlew.bat                  ← Wrapper scripts
├── gradle.properties
└── README.md                             ← (this file)
```

---

## How to open and run

### Recommended: Android Studio

1. Open **Android Studio Hedgehog (2023.1.1)** or newer (Iguana / Jellyfish / Koala all work).
2. **File → Open** → select the `PuzzleQuest/` folder.
3. Wait for the first **Gradle Sync**. On a fresh machine this downloads:
   - Gradle 8.14.3 (~150 MB)
   - AGP 8.5.2 + Kotlin 1.9.24 + Compose 1.6.x dependencies (~500 MB)
   - Android SDK Platform 34 if missing
   - Estimated time: 5-15 min depending on your network.
4. Plug in a phone (USB debugging enabled) or start an emulator running **Android 8.0 (API 26)** or newer.
5. Press the green **Run ▶** button. APK installs in ~30 s on a modern phone.

### From command line (after the first sync has populated caches)

```sh
cd PuzzleQuest
./gradlew assembleDebug                   # Builds app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug                    # Builds + installs on a connected device
```

For a release APK:

```sh
./gradlew assembleRelease
```

Note: the release config currently signs with the **debug keystore** so the build succeeds out of the box. **Before publishing to Play Store**, generate a real keystore:

```sh
keytool -genkey -v -keystore puzzle-quest-release.jks \
        -keyalg RSA -keysize 2048 -validity 10000 -alias puzzle-quest
```

Then add a `signingConfigs` block to `app/build.gradle.kts` and reference it from `release { signingConfig = ... }`.

For Play Store submission, use the **Android App Bundle** instead of an APK:

```sh
./gradlew bundleRelease   # produces app/build/outputs/bundle/release/app-release.aab
```

---

## Things you may want to tweak

### Application ID & display name

Currently:

- App ID: `com.pingsama.puzzlequest` (`app/build.gradle.kts` — `applicationId`)
- App name: "Puzzle Quest" (`res/values/strings.xml` — `app_name`)

Both should be changed if either no longer fits — the App ID in particular **cannot be changed** after the first Play Store upload.

### Fonts (Fredoka / Poppins)

The original web app used **Fredoka** (display) and **Poppins** (body). To keep the project building offline with no extra setup, this port uses the system sans-serif. The styling difference is small — bold / size hierarchy carries the look — but you can swap in the real fonts for an exact match. See the comment in `ui/theme/Type.kt` for the 3-step recipe (uses Compose's downloadable Google Fonts API).

### Ads

The original had AdMob banner + interstitials. This port does **not** include ad SDKs. To add them later:

1. Add `implementation("com.google.android.gms:play-services-ads:23.x")` to `app/build.gradle.kts`.
2. Initialize in `PuzzleQuestApp.onCreate`.
3. Wire banner views in via `AndroidView` inside `GameScreen.kt`.
4. Show interstitials between levels in the `onLevelComplete` callback.

The codebase deliberately keeps `GameScreen` decoupled from any ad logic so this is a clean addition.

### Image quality

The 25 puzzle images were re-encoded from 2048×2048 PNGs (totaling 110 MB) to 1024×1024 WebP at quality 85 (totaling 2.5 MB). At a 6×6 grid on the largest phone screen, each piece displays at ~150 px max, so 1024 source = ~170 px per piece, which is more resolution than is ever drawn. If you want sharper still, drop new WebPs into `app/src/main/assets/images/` keeping the `levelN.webp` naming.

---

## Differences from the web original (intentional)

1. **Best Time meaning fixed.** The original stored "remaining time" then reported `min(remaining)` as the best time, which was inverted — it actually showed the *worst* time. This port stores **time used** and reports `min(time_used)`, which is what players expect.
2. **Pill buttons instead of round icon buttons.** The reference screenshot you provided showed pill-shaped buttons with text labels (Hint / Restart / Home / etc.), so I matched that. The original V2 web code used circular emoji-only buttons in the same positions.
3. **No ad SDK, no analytics, no debug-collector script.** Everything is offline-first.

Everything else — game logic, level progression, time limits, the snap-and-lock behavior, the win/lose/restart flows, sound design — is a faithful port.

---

## Honest notes about first build

This project was generated end-to-end without a Compose compiler available to verify it. The structure and APIs are all correct against the documented Compose 1.6.x surface, but **expect possibly 1–3 small compile errors on first sync** — typically a missing import, a renamed property in your specific Compose version, or a Material3 API that moved between minor versions. Android Studio will surface them with quick-fix suggestions.

If anything blocks you, the files most likely to need touching are:

- **`ui/PuzzleBoard.kt`** — uses `awaitPointerEventScope` directly. If your Compose version requires a different name on `PointerInputChange.consume()`, fix the imports.
- **`MainActivity.kt`** — uses `enableEdgeToEdge()`. Requires `androidx.activity:activity-compose:1.9.0+`.
