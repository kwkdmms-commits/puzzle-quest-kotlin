package com.puzzlequest.game.engine

import android.content.Context
import com.puzzlequest.game.data.Level
import kotlin.math.floor
import kotlin.random.Random

data class GridPiece(
    val id: String,
    val correctRow: Int,
    val correctCol: Int,
    var isLocked: Boolean = false
)

data class GameState(
    val gridSize: Int,
    val grid: Array<Array<GridPiece>>,
    var lockedCount: Int = 0,
    var totalPieces: Int = 0,
    var isWon: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        if (gridSize != other.gridSize) return false
        if (lockedCount != other.lockedCount) return false
        if (totalPieces != other.totalPieces) return false
        if (isWon != other.isWon) return false
        if (!grid.contentDeepEquals(other.grid)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = gridSize
        result = 31 * result + grid.contentDeepHashCode()
        result = 31 * result + lockedCount
        result = 31 * result + totalPieces
        result = 31 * result + isWon.hashCode()
        return result
    }
}

class GameEngine(context: Context, private val level: Level) {
    private val pieceManager = PuzzlePieceManager(context, level)
    private val gridSize = level.gridSize
    private var gameState: GameState
    
    // Board dimensions
    private var boardSize = 0
    private var pieceSize = 0
    private var moveCount = 0

    init {
        gameState = initializeGame()
    }

    fun setBoardDimensions(screenWidth: Int, screenHeight: Int) {
        // Match React logic: maxBoardSize = min(screenWidth - 32, screenHeight * 0.5)
        val maxBoardSize = minOf(screenWidth - 32, (screenHeight * 0.5).toInt())
        this.pieceSize = floor(maxBoardSize.toFloat() / gridSize).toInt()
        this.boardSize = pieceSize * gridSize
    }

    private fun initializeGame(): GameState {
        val totalPieces = gridSize * gridSize
        
        // Create pieces with their correct positions
        val pieces = mutableListOf<GridPiece>()
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                pieces.add(
                    GridPiece(
                        id = "piece-$row-$col",
                        correctRow = row,
                        correctCol = col,
                        isLocked = false
                    )
                )
            }
        }
        
        // Shuffle pieces until NO piece is in its correct position (derangement)
        var shuffledPieces = pieces.shuffled()
        var hasCorrectPosition = true
        
        while (hasCorrectPosition) {
            hasCorrectPosition = false
            
            // Check if any piece is already in its correct position
            for (i in shuffledPieces.indices) {
                val piece = shuffledPieces[i]
                val row = i / gridSize
                val col = i % gridSize
                
                if (piece.correctRow == row && piece.correctCol == col) {
                    hasCorrectPosition = true
                    break
                }
            }
            
            // If any piece is correct, reshuffle
            if (hasCorrectPosition) {
                shuffledPieces = pieces.shuffled()
            }
        }
        
        // Create grid with shuffled pieces
        val grid = Array(gridSize) { Array(gridSize) { GridPiece("", 0, 0) } }
        var pieceIndex = 0
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                grid[row][col] = shuffledPieces[pieceIndex++]
            }
        }
        
        return GameState(
            gridSize = gridSize,
            grid = grid,
            lockedCount = 0,
            totalPieces = totalPieces,
            isWon = false
        )
    }

    fun getGameState(): GameState = gameState

    fun getPieceManager(): PuzzlePieceManager = pieceManager

    fun getPieceSize(): Int = pieceSize

    fun getBoardSize(): Int = boardSize

    fun getGridSize(): Int = gridSize

    fun getMoveCount(): Int = moveCount

    fun getLockedCount(): Int = gameState.lockedCount

    fun getTotalPieces(): Int = gameState.totalPieces

    fun isWon(): Boolean = gameState.isWon

    /**
     * Place a piece at a target grid position (swap with target)
     * If piece lands in its correct position, lock it
     */
    fun placePiece(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        // Validate positions
        if (!isValidPosition(fromRow, fromCol) || !isValidPosition(toRow, toCol)) {
            return
        }
        
        // Cannot move locked pieces
        if (gameState.grid[fromRow][fromCol].isLocked) {
            return
        }
        
        // Cannot drop on locked pieces
        if (gameState.grid[toRow][toCol].isLocked) {
            return
        }
        
        val piece = gameState.grid[fromRow][fromCol]
        
        // Check if piece is being dropped on its correct position
        val isCorrectPosition = piece.correctRow == toRow && piece.correctCol == toCol
        
        if (isCorrectPosition) {
            // Swap pieces
            gameState.grid[fromRow][fromCol] = gameState.grid[toRow][toCol]
            // Place piece in correct position and lock it
            gameState.grid[toRow][toCol] = piece.copy(isLocked = true)
        } else {
            // Swap pieces
            val temp = gameState.grid[fromRow][fromCol]
            gameState.grid[fromRow][fromCol] = gameState.grid[toRow][toCol]
            gameState.grid[toRow][toCol] = temp
        }
        
        // Check and lock all correctly placed pieces
        var lockedCount = 0
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val p = gameState.grid[r][c]
                if (p.correctRow == r && p.correctCol == c) {
                    p.isLocked = true
                }
                if (p.isLocked) {
                    lockedCount++
                }
            }
        }
        
        gameState.lockedCount = lockedCount
        gameState.isWon = lockedCount == gameState.totalPieces
        moveCount++
    }

    private fun isValidPosition(row: Int, col: Int): Boolean {
        return row >= 0 && row < gridSize && col >= 0 && col < gridSize
    }

    fun resetLevel() {
        gameState = initializeGame()
        moveCount = 0
    }

    fun cleanup() {
        pieceManager.cleanup()
    }
}
