package com.puzzlequest.game.engine

import android.content.Context
import com.puzzlequest.game.data.Level
import com.puzzlequest.game.data.PuzzlePiece
import kotlin.math.abs
import kotlin.random.Random

class GameEngine(context: Context, private val level: Level) {
    private val pieces = mutableListOf<PuzzlePiece>()
    private val gridSize = level.gridSize
    private val snapThreshold = 40 // pixels
    private val pieceManager = PuzzlePieceManager(context, level)
    private val pieceSize = pieceManager.getPieceSize()
    
    // Board dimensions (will be set by view)
    private var boardSize = 0
    private var boardOffsetX = 0f
    private var boardOffsetY = 0f
    
    // Store starting positions for pieces
    private val startingPositions = mutableMapOf<Int, Pair<Float, Float>>()

    init {
        initializePuzzle()
    }

    fun setBoardDimensions(size: Int, offsetX: Float, offsetY: Float) {
        this.boardSize = size
        this.boardOffsetX = offsetX
        this.boardOffsetY = offsetY
    }

    private fun initializePuzzle() {
        pieces.clear()
        startingPositions.clear()
        val totalPieces = gridSize * gridSize

        // Create all pieces with correct grid positions
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

        // Shuffle pieces to random positions WITHIN the board grid
        shufflePiecesInBoard()
    }

    private fun shufflePiecesInBoard() {
        val random = Random(System.currentTimeMillis())
        val availablePositions = mutableListOf<Pair<Int, Int>>()

        // Generate all grid positions
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                availablePositions.add(Pair(col, row))
            }
        }

        // Shuffle positions
        availablePositions.shuffle(random)

        // Assign shuffled positions to pieces
        for ((index, piece) in pieces.withIndex()) {
            val (shuffledCol, shuffledRow) = availablePositions[index]
            
            // Calculate pixel position within board
            val pixelX = boardOffsetX + shuffledCol * pieceSize
            val pixelY = boardOffsetY + shuffledRow * pieceSize
            
            piece.currentX = pixelX
            piece.currentY = pixelY
            piece.isLocked = false
            
            // Store starting position for snap-back
            startingPositions[piece.id] = Pair(pixelX, pixelY)
        }
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
        } else {
            // Snap back to starting position if dropped in wrong spot
            snapBackToStartingPosition(piece)
        }
        return false
    }

    private fun isNearCorrectPosition(piece: PuzzlePiece): Boolean {
        val correctX = boardOffsetX + piece.gridX * pieceSize
        val correctY = boardOffsetY + piece.gridY * pieceSize

        val distX = abs(piece.currentX - correctX)
        val distY = abs(piece.currentY - correctY)

        return distX < snapThreshold && distY < snapThreshold
    }

    private fun snapToCorrectPosition(piece: PuzzlePiece) {
        piece.currentX = boardOffsetX + (piece.gridX * pieceSize).toFloat()
        piece.currentY = boardOffsetY + (piece.gridY * pieceSize).toFloat()
    }

    private fun snapBackToStartingPosition(piece: PuzzlePiece) {
        val startPos = startingPositions[piece.id]
        if (startPos != null) {
            piece.currentX = startPos.first
            piece.currentY = startPos.second
        }
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
