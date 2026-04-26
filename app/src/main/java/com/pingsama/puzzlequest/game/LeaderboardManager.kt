package com.pingsama.puzzlequest.game

import android.content.Context

/**
 * Persists progress and best scores. Uses SharedPreferences so it survives across launches
 * and is included in Android Auto Backup (see backup_rules.xml).
 *
 * NOTE on "best time": the original web app stored the *remaining* time and used Math.min,
 * which produced a counter-intuitive "best". Here we store **time used** instead so that
 * "best = lowest" matches what a player expects.
 */
class LeaderboardManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ----- progress -----

    fun currentLevel(): Int = prefs.getInt(KEY_LEVEL, 1).coerceAtLeast(1)
    fun saveCurrentLevel(level: Int) { prefs.edit().putInt(KEY_LEVEL, level).apply() }

    fun puzzlesCompleted(): Int = prefs.getInt(KEY_COMPLETED, 0)
    fun savePuzzlesCompleted(n: Int) { prefs.edit().putInt(KEY_COMPLETED, n).apply() }

    // ----- best scores per grid size -----

    /** @param timeUsed seconds spent solving (timeLimit - timeRemaining). */
    fun saveScore(gridSize: Int, moves: Int, timeUsed: Int) {
        val movesKey = "best_moves_$gridSize"
        val timeKey  = "best_time_used_$gridSize"
        val edit = prefs.edit()

        val prevMoves = prefs.getInt(movesKey, Int.MAX_VALUE)
        if (moves < prevMoves) edit.putInt(movesKey, moves)

        val prevTime = prefs.getInt(timeKey, Int.MAX_VALUE)
        if (timeUsed < prevTime) edit.putInt(timeKey, timeUsed)

        edit.apply()
    }

    fun bestMoves(gridSize: Int): Int? {
        val v = prefs.getInt("best_moves_$gridSize", -1)
        return if (v >= 0) v else null
    }

    fun bestTimeUsed(gridSize: Int): Int? {
        val v = prefs.getInt("best_time_used_$gridSize", -1)
        return if (v >= 0) v else null
    }

    companion object {
        private const val PREFS = "puzzle_quest_prefs"
        private const val KEY_LEVEL = "current_level"
        private const val KEY_COMPLETED = "puzzles_completed"
    }
}

/** Format seconds → "M:SS". */
fun formatTime(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
