package com.puzzlequest.game.data

import android.graphics.PointF

// Puzzle piece data
data class PuzzlePiece(
    val id: Int,
    val gridX: Int,
    val gridY: Int,
    val imageResId: Int,
    var currentX: Float = 0f,
    var currentY: Float = 0f,
    var isLocked: Boolean = false
)

// Level configuration
data class Level(
    val levelNumber: Int,
    val gridSize: Int, // 4 for 4x4, 6 for 6x6
    val imageResId: Int,
    val difficulty: String // "normal" or "hard"
)

// Game state
data class GameState(
    val currentLevel: Int = 1,
    val timer: Int = 0,
    val moves: Int = 0,
    val hintsUsed: Int = 0,
    val moreTimeUsed: Int = 0,
    val isGameActive: Boolean = true,
    val piecesPlaced: Int = 0
)

// Level definitions (all 25 levels)
object LevelConfig {
    fun getLevel(levelNumber: Int): Level {
        val imageResId = when (levelNumber) {
            1 -> com.puzzlequest.game.R.drawable.level1
            2 -> com.puzzlequest.game.R.drawable.level2
            3 -> com.puzzlequest.game.R.drawable.level3
            4 -> com.puzzlequest.game.R.drawable.level4
            5 -> com.puzzlequest.game.R.drawable.level5
            6 -> com.puzzlequest.game.R.drawable.level6
            7 -> com.puzzlequest.game.R.drawable.level7
            8 -> com.puzzlequest.game.R.drawable.level8
            9 -> com.puzzlequest.game.R.drawable.level9
            10 -> com.puzzlequest.game.R.drawable.level10
            11 -> com.puzzlequest.game.R.drawable.level11
            12 -> com.puzzlequest.game.R.drawable.level12
            13 -> com.puzzlequest.game.R.drawable.level13
            14 -> com.puzzlequest.game.R.drawable.level14
            15 -> com.puzzlequest.game.R.drawable.level15
            16 -> com.puzzlequest.game.R.drawable.level16
            17 -> com.puzzlequest.game.R.drawable.level17
            18 -> com.puzzlequest.game.R.drawable.level18
            19 -> com.puzzlequest.game.R.drawable.level19
            20 -> com.puzzlequest.game.R.drawable.level20
            21 -> com.puzzlequest.game.R.drawable.level21
            22 -> com.puzzlequest.game.R.drawable.level22
            23 -> com.puzzlequest.game.R.drawable.level23
            24 -> com.puzzlequest.game.R.drawable.level24
            25 -> com.puzzlequest.game.R.drawable.level25
            else -> com.puzzlequest.game.R.drawable.level1
        }

        val gridSize = if (levelNumber <= 15) 4 else 6
        val difficulty = if (levelNumber <= 15) "normal" else "hard"

        return Level(levelNumber, gridSize, imageResId, difficulty)
    }

    fun getTotalLevels() = 25
}
