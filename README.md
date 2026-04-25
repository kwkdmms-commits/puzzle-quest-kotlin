# Puzzle Quest - Native Kotlin Android

A native Android puzzle game built with Kotlin. Fully playable offline with 25 levels.

## Features

- **25 Puzzle Levels** - Progressive difficulty (4x4 normal, 6x6 hard)
- **Offline Gameplay** - All assets bundled locally, no internet required
- **Drag & Drop Mechanics** - Intuitive puzzle piece placement
- **Power-ups** - Hint and More Time per level
- **Smooth UI** - Material Design with responsive layouts
- **Progress Tracking** - Saves player progress locally

## Building

### Prerequisites
- Android SDK 34+
- Kotlin 1.9+
- Gradle 8.1+

### Build APK
```bash
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release
```bash
./gradlew assembleRelease
```

## Project Structure

```
app/
├── src/main/
│   ├── kotlin/com/puzzlequest/game/
│   │   ├── MainActivity.kt
│   │   ├── ui/
│   │   ├── game/
│   │   └── utils/
│   ├── res/
│   │   ├── drawable/
│   │   ├── layout/
│   │   ├── values/
│   │   └── mipmap/
│   └── AndroidManifest.xml
└── build.gradle.kts
```

## Game Levels

- Levels 1-15: 4x4 puzzles (Normal difficulty)
- Levels 16-25: 6x6 puzzles (Hard difficulty)

All puzzle images are bundled in the APK and load from local assets.

## License

Proprietary - Puzzle Quest Game
