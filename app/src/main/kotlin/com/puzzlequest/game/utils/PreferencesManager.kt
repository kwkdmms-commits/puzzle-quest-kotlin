package com.puzzlequest.game.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "puzzle_quest_prefs",
        Context.MODE_PRIVATE
    )

    fun getCurrentLevel(): Int {
        return prefs.getInt("current_level", 1)
    }

    fun setCurrentLevel(level: Int) {
        prefs.edit().putInt("current_level", level).apply()
    }

    fun getHighestUnlockedLevel(): Int {
        return prefs.getInt("highest_level", 1)
    }

    fun setHighestUnlockedLevel(level: Int) {
        val current = getHighestUnlockedLevel()
        if (level > current) {
            prefs.edit().putInt("highest_level", level).apply()
        }
    }

    fun getLevelCompletionTime(level: Int): Int {
        return prefs.getInt("level_${level}_time", 0)
    }

    fun setLevelCompletionTime(level: Int, time: Int) {
        prefs.edit().putInt("level_${level}_time", time).apply()
    }

    fun hasCompletedLevel(level: Int): Boolean {
        return prefs.getBoolean("level_${level}_completed", false)
    }

    fun setLevelCompleted(level: Int) {
        prefs.edit().putBoolean("level_${level}_completed", true).apply()
        setHighestUnlockedLevel(level + 1)
    }

    fun resetAllProgress() {
        prefs.edit().clear().apply()
        setCurrentLevel(1)
        setHighestUnlockedLevel(1)
    }
}
