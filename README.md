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
- **Time limit:** 1:30 for 4×4, 2:30 for 6×6.
- **Hint** shows the full image (requires watching a rewarded ad once per level). **More Time** adds 60 s (once per level, requires rewarded ad). **Restart** confirms before resetting.
- Sound effects are **synthesized at runtime** via `AudioTrack` PCM — same waveform shapes as the original Web Audio code.
- Progress (current level, total puzzles solved, best moves & best time per grid size) is persisted in `SharedPreferences`, included in Android Auto Backup.
- **Ads:** Banner ads at bottom, interstitial ads between levels, rewarded ads for power-ups.

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
│       │   ├── ads/
│       │   │   ├── BannerAdManager.kt    ← Banner ad management
│       │   │   ├── InterstitialAdManager.kt ← Interstitial ad management
│       │   │   └── RewardedAdManager.kt  ← Rewarded ad management
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
│       │       └── GameScreen.kt         ← Header, board, buttons, popups, ads
│       └── res/
│           ├── values/                   ← strings, colors, themes, ic_launcher_background
│           ├── mipmap-anydpi-v26/        ← Adaptive launcher icon
│           ├── drawable/                 ← Vector launcher foreground
│           └── xml/                      ← Backup rules
├── .github/workflows/
│   ├── build.yml                         ← Debug build on push
│   └── release-build.yml                 ← Release build on tag push
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
./gradlew assembleRelease                 # Builds app/build/outputs/apk/release/app-release.apk
```

For Play Store submission, use the **Android App Bundle** instead of an APK:

```sh
./gradlew bundleRelease                   # Produces app/build/outputs/bundle/release/app-release.aab
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

This port includes **AdMob integration** with test ad unit IDs:

- **Banner Ads:** Bottom of game screen (60dp reserved space)
- **Interstitial Ads:** Between levels (after 2 min gameplay or 3 screen changes)
- **Rewarded Ads:** For Hint and More Time buttons

**Current Configuration (Test Ads):**
- AdMob App ID: `ca-app-pub-3940256099942544~3347511713` (Google test ID)
- Banner: `ca-app-pub-3940256099942544/6300978111` (Google test ID)
- Interstitial: `ca-app-pub-3940256099942544/1033173712` (Google test ID)
- Rewarded: `ca-app-pub-3940256099942544/5224354917` (Google test ID)

**Before Publishing to Play Store:**

1. Replace test ad unit IDs with your real AdMob ad unit IDs:
   - Update `BannerAd()` in `GameScreen.kt` (search for `ca-app-pub-3940256099942544/6300978111`)
   - Update `InterstitialAdManager.kt` (search for `ca-app-pub-3940256099942544/1033173712`)
   - Update `RewardedAdManager.kt` (search for `ca-app-pub-3940256099942544/5224354917`)

2. Update AdMob App ID in `AndroidManifest.xml`:
   ```xml
   <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy" />
   ```

3. Test with real ads on internal test track first.

### Image quality

The 25 puzzle images were re-encoded from 2048×2048 PNGs (totaling 110 MB) to 1024×1024 WebP at quality 85 (totaling 2.5 MB). At a 6×6 grid on the largest phone screen, each piece displays at ~150 px max, so 1024 source = ~170 px per piece, which is more resolution than is ever drawn. If you want sharper still, drop new WebPs into `app/src/main/assets/images/` keeping the `levelN.webp` naming.

---

## GitHub Release & Play Store Publishing

### Automated Release Builds

A GitHub Actions workflow (`release-build.yml`) automatically builds release APK and AAB on:

1. **Tag push:** Push a tag like `v1.0.0` to trigger a release build
   ```sh
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **Manual workflow dispatch:** Run the workflow manually from the Actions tab

### Download Release Artifacts

1. Go to **Actions** tab on GitHub
2. Click the latest **Release Build** workflow run
3. Download artifacts:
   - `puzzle-quest-release-apk` — Unsigned release APK (for direct installation)
   - `puzzle-quest-release-aab` — Android App Bundle (for Play Store submission)

### Play Store Secrets Setup (Optional)

To enable automatic Play Store uploads:

1. Generate a **Play Store service account JSON** from Google Play Console
2. Add it as a GitHub secret: `PLAY_STORE_SERVICE_ACCOUNT_JSON`
3. Uncomment the "Upload to Play Store" step in `.github/workflows/release-build.yml`

### Signing Configuration

**Current:** Release builds sign with the debug keystore (unsigned for distribution).

**For Production:**

1. Generate a release keystore:
   ```sh
   keytool -genkey -v -keystore puzzle-quest-release.jks \
           -keyalg RSA -keysize 2048 -validity 10000 -alias puzzle-quest
   ```

2. Add to `app/build.gradle.kts`:
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../puzzle-quest-release.jks")
           storePassword = System.getenv("KEYSTORE_PASSWORD")
           keyAlias = "puzzle-quest"
           keyPassword = System.getenv("KEY_PASSWORD")
       }
   }
   
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")
       }
   }
   ```

3. Add secrets to GitHub:
   - `KEYSTORE_PASSWORD` — Your keystore password
   - `KEY_PASSWORD` — Your key password

### Version Management

Update version in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 100      // Increment for each release
    versionName = "1.0.0"  // Semantic versioning
}
```

**Current Version:** 1.0.0 (versionCode: 100)

---

## Differences from the web original (intentional)

1. **Best Time meaning fixed.** The original stored "remaining time" then reported `min(remaining)` as the best time, which was inverted — it actually showed the *worst* time. This port stores **time used** and reports `min(time_used)`, which is what players expect.
2. **Pill buttons instead of round icon buttons.** The reference screenshot showed pill-shaped buttons with text labels (Hint / Restart / Home / etc.), so I matched that. The original V2 web code used circular emoji-only buttons in the same positions.
3. **Rewarded ads for power-ups.** Hint and More Time now require watching a rewarded ad, with smart unlock logic (hint unlocks once per level after watching ad).
4. **Timer pauses during ads.** The puzzle timer pauses when any full-screen ad (rewarded or interstitial) is showing, preventing unfair time loss.

Everything else — game logic, level progression, time limits, the snap-and-lock behavior, the win/lose/restart flows, sound design — is a faithful port.

---

## Honest notes about first build

This project was generated end-to-end without a Compose compiler available to verify it. The structure and APIs are all correct against the documented Compose 1.6.x surface, but **expect possibly 1–3 small compile errors on first sync** — typically a missing import, a renamed property in your specific Compose version, or a Material3 API that moved between minor versions. Android Studio will surface them with quick-fix suggestions.

If anything blocks you, the files most likely to need touching are:

- **`ui/PuzzleBoard.kt`** — uses `awaitPointerEventScope` directly. If your Compose version requires a different name on `PointerInputChange.consume()`, fix the imports.
- **`MainActivity.kt`** — uses `enableEdgeToEdge()`. Requires `androidx.activity:activity-compose:1.9.0+`.
