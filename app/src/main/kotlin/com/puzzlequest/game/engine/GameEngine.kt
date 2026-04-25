package com.puzzlequest.game.engine

import android.content.Context
import com.puzzlequest.game.data.Level
import com.puzzlequest.game.data.PuzzlePiece
import kotlin.math.abs
import kotlin.random.Random

class GameEngine(context: Context, private val level: Level) {
    private val pieces = mutableListOf<PuzzlePiece>()
    private val gridSize = level.gridSize
    private val snapThreshold = 50 // pixels
    private val pieceManager = PuzzlePieceManager(context, level)
    private val pieceSize = pieceManager.getPieceSize()

    init {
        initializePuzzle()
    }

    private fun initializePuzzle() {
        pieces.clear()
        val totalPieces = gridSize * gridSize

        // Create all pieces with correct positions
        for (i in 0 until totalPieces) {
            val gridX = i % gridSize
            val gridY = i / gridSize
            pieces.add(
                PuzzlePiece(
                    id = i,
                    gridX = gridX,
                    gridY = gridY,
                    imageResId = level.imageResId,
                    currentX = 0f,
                    currentY = 0f,
                    isLocked = false
                )
            )
        }

        // Shuffle pieces to random positions (ensuring none start in correct position)
        shufflePieces()
    }

    private fun shufflePieces() {
        val screenWidth = 1080f // Approximate screen width
        val screenHeight = 1920f // Approximate screen height
        val random = Random(System.currentTimeMillis())

        var allInCorrectPosition = true
        do {
            for (piece in pieces) {
                piece.currentX = random.nextFloat() * (screenWidth - pieceSize)
                piece.currentY = random.nextFloat() * (screenHeight - pieceSize)
                piece.isLocked = false

                // Check if piece is in correct position
                if (!isInCorrectPosition(piece)) {
                    allInCorrectPosition = false
                }
            }
        } while (allInCorrectPosition)
    }

    fun getPieces(): List<PuzzlePiece> = pieces

    fun getPieceManager(): PuzzlePieceManager = pieceManager

    fun getPieceSize(): Int = pieceSize

    fun movePiece(pieceId: Int, newX: Float, newY: Float) {
        val piece = pieces.find { it.id == pieceId } ?: return
        if (!piece.isLocked) {
            piece.currentX = newX
            piece.currentY = newY
        }
    }

    fun checkAndLockPiece(pieceId: Int): Boolean {
        val piece = pieces.find { it.id == pieceId } ?: return false
        if (piece.isLocked) return false

        if (isNearCorrectPosition(piece)) {
            piece.isLocked = true
            snapToCorrectPosition(piece)
            return true
        }
        return false
    }

    private fun isNearCorrectPosition(piece: PuzzlePiece): Boolean {
        val correctX = piece.gridX * pieceSize.toFloat()
        val correctY = piece.gridY * pieceSize.toFloat()

        val distX = abs(piece.currentX - correctX)
        val distY = abs(piece.currentY - correctY)

        return distX < snapThreshold && distY < snapThreshold
    }

    private fun isInCorrectPosition(piece: PuzzlePiece): Boolean {
        val correctX = piece.gridX * pieceSize.toFloat()
        val correctY = piece.gridY * pieceSize.toFloat()

        return piece.currentX == correctX && piece.currentY == correctY && piece.isLocked
    }

    private fun snapToCorrectPosition(piece: PuzzlePiece) {
        piece.currentX = (piece.gridX * pieceSize).toFloat()
        piece.currentY = (piece.gridY * pieceSize).toFloat()
    }

    fun isLevelComplete(): Boolean {
        return pieces.all { it.isLocked }
    }

    fun getLockedPiecesCount(): Int {
        return pieces.count { it.isLocked }
    }

    fun getTotalPieces(): Int {
        return pieces.size
    }

    fun resetLevel() {
        initializePuzzle()
    }

    fun cleanup() {
        pieceManager.cleanup()
    }
}
