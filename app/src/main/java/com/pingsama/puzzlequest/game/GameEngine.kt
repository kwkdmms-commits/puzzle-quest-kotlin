package com.pingsama.puzzlequest.game

/**
 * Pure puzzle state + movement logic.
 *
 *  • Pieces have NO locked flag. Every piece is always draggable.
 *  • A piece is "correct" iff its current position matches (correctRow, correctCol).
 *  • Drag & drop = unconditional swap of source and destination cells.
 *  • Win = every cell holds the piece that belongs there.
 */

data class GridPiece(
    val id: String,
    val correctRow: Int,
    val correctCol: Int,
)

data class GameState(
    val gridSize: Int,
    val imageId: String,
    val grid: List<List<GridPiece>>,
    val correctCount: Int,
    val totalPieces: Int,
    val isWon: Boolean,
)

object GameEngine {

    /** Build a fresh game where no piece happens to start at its correct position. */
    fun initialize(imageId: String, gridSize: Int): GameState {
        val pieces = ArrayList<GridPiece>(gridSize * gridSize)
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                pieces += GridPiece(
                    id = "piece-$row-$col",
                    correctRow = row,
                    correctCol = col,
                )
            }
        }

        val working = pieces.toMutableList().also { it.shuffle() }
        var attempts = 0
        while (anyPieceCorrectlyPlaced(working, gridSize) && attempts < 200) {
            working.shuffle()
            attempts++
        }

        val grid = List(gridSize) { row ->
            List(gridSize) { col -> working[row * gridSize + col] }
        }
        return finalize(grid, gridSize, imageId)
    }

    private fun anyPieceCorrectlyPlaced(flat: List<GridPiece>, gridSize: Int): Boolean {
        for (i in flat.indices) {
            val p = flat[i]
            if (p.correctRow == i / gridSize && p.correctCol == i % gridSize) return true
        }
        return false
    }

    /** Is the piece at (row, col) sitting on its correct cell? */
    fun isCorrect(state: GameState, row: Int, col: Int): Boolean {
        val p = state.grid[row][col]
        return p.correctRow == row && p.correctCol == col
    }

    /**
     * Unconditional swap of (srcRow, srcCol) with (dstRow, dstCol). Returns the
     * new state, or [state] unchanged only if indices are off-grid or src == dst.
     * Never blocked by correctness — a correct piece can be displaced freely.
     */
    fun placePiece(
        state: GameState,
        srcRow: Int, srcCol: Int,
        dstRow: Int, dstCol: Int,
    ): GameState {
        val sz = state.gridSize
        if (srcRow !in 0 until sz || srcCol !in 0 until sz) return state
        if (dstRow !in 0 until sz || dstCol !in 0 until sz) return state
        if (srcRow == dstRow && srcCol == dstCol) return state

        val newGrid = state.grid.map { it.toMutableList() }.toMutableList()
        val tmp = newGrid[srcRow][srcCol]
        newGrid[srcRow][srcCol] = newGrid[dstRow][dstCol]
        newGrid[dstRow][dstCol] = tmp
        return finalize(newGrid, sz, state.imageId)
    }

    private fun finalize(
        grid: List<List<GridPiece>>,
        gridSize: Int,
        imageId: String,
    ): GameState {
        var correct = 0
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val p = grid[r][c]
                if (p.correctRow == r && p.correctCol == c) correct++
            }
        }
        val total = gridSize * gridSize
        return GameState(
            gridSize = gridSize,
            imageId = imageId,
            grid = grid.map { it.toList() },
            correctCount = correct,
            totalPieces = total,
            isWon = correct == total,
        )
    }
}
