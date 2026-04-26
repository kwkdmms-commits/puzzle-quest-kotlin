package com.pingsama.puzzlequest.game

/**
 * Maps a level number → grid difficulty + time limit + asset filename.
 * Mirrors `client/src/lib/levelSystem.ts`:
 *
 *  Pattern: 4 levels of 4×4, then 1 level of 6×6, repeating.
 *  → Levels 1-4 = 4×4, Level 5 = 6×6, Levels 6-9 = 4×4, Level 10 = 6×6, …
 *
 *  Time limit (from GameScreenV2.tsx): 150s for 4×4, 240s for 6×6.
 */

data class LevelConfig(val level: Int, val gridSize: Int)

object LevelSystem {
    const val MAX_LEVEL = 25

    fun configFor(level: Int): LevelConfig {
        val positionInCycle = ((level - 1) % 5) + 1
        return if (positionInCycle == 5) LevelConfig(level, 6)
        else LevelConfig(level, 4)
    }

    fun timeLimitFor(level: Int): Int =
        if (configFor(level).gridSize == 6) 240 else 150

    /** Asset path, e.g. "images/level3.webp". Caps at level 25. */
    fun assetForLevel(level: Int): String {
        val capped = level.coerceIn(1, MAX_LEVEL)
        return "images/level$capped.webp"
    }

    fun nextLevel(level: Int): Int = (level + 1).coerceAtMost(MAX_LEVEL)
}
