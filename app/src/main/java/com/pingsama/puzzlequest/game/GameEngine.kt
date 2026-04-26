package com.pingsama.puzzlequest.game

/**
 * Pure data + functions describing puzzle state. Mirrors the web app's
 * `client/src/lib/gameEngineV2.ts` 1:1 in behavior:
 *
 *  • Pieces are addressed by a 2-D grid of [GridPiece]s.
 *  • Each piece remembers the (row, col) it BELONGS in (`correctRow`/`correctCol`).
 *  • Initial placement is shuffled until no piece happens to start at its correct spot.
 *  • Dropping a piece on its own correct cell snaps + locks it.
 *  • Dropping anywhere else swaps with whatever piece is there.
 *  • A locked piece can never be moved or displaced.
 *  • Win == every piece is locked.
 */

data class GridPiece(
    val id: String,
    val correctRow: Int,
    val correctCol: Int,
    val isLocked: Boolean,
)

data class GameState(
    val gridSize: Int,
    val imageId: String,
    val grid: List<List<GridPiece>>,
    val lockedCount: Int,
    val totalPieces: Int,
    val isWon: Boolean,
)

object GameEngine {

    /**
     * Build a fresh, shuffled game where every piece starts in a wrong position.
     * (Also matches the original — keeps reshuffling until that invariant holds.)
     */
    fun initialize(imageId: String, gridSize: Int): GameState {
        val pieces = ArrayList<GridPiece>(gridSize * gridSize)
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                pieces += GridPiece(
                    id = "piece-$row-$col",
                    correctRow = row,
                    correctCol = col,
                    isLocked = false,
                )
            }
        }

        var working = pieces.toMutableList().also { it.shuffle() }
        var attempts = 0
        while (anyPieceCorrectlyPlaced(working, gridSize) && attempts < 200) {
            working.shuffle()
            attempts++
        }

        val grid = List(gridSize) { row ->
            List(gridSize) { col -> working[row * gridSize + col] }
        }

        return GameState(
            gridSize = gridSize,
            imageId = imageId,
            grid = grid,
            lockedCount = 0,
            totalPieces = gridSize * gridSize,
            isWon = false,
        )
    }

    private fun anyPieceCorrectlyPlaced(flat: List<GridPiece>, gridSize: Int): Boolean {
        for (i in flat.indices) {
            val p = flat[i]
            if (p.correctRow == i / gridSize && p.correctCol == i % gridSize) return true
        }
        return false
    }

    /**
     * Move a piece from (pieceRow, pieceCol) onto (targetRow, targetCol).
     * Returns the new state (or [state] unchanged if the move is invalid).
     */
    fun placePiece(
        state: GameState,
        pieceRow: Int,
        pieceCol: Int,
        targetRow: Int,
        targetCol: Int,
    ): GameState {
        val sz = state.gridSize
        if (pieceRow !in 0 until sz || pieceCol !in 0 until sz) return state
        if (targetRow !in 0 until sz || targetCol !in 0 until sz) return state
        if (state.grid[pieceRow][pieceCol].isLocked) return state
        if (state.grid[targetRow][targetCol].isLocked) return state
        if (pieceRow == targetRow && pieceCol == targetCol) return state // dropped on self → no-op

        val piece = state.grid[pieceRow][pieceCol]
        val newGrid: MutableList<MutableList<GridPiece>> =
            state.grid.map { it.toMutableList() }.toMutableList()

        val droppedOnCorrectCell = piece.correctRow == targetRow && piece.correctCol == targetCol
        if (droppedOnCorrectCell) {
            // Snap into the correct cell (locked); whatever was there moves to the source cell.
            newGrid[pieceRow][pieceCol] = newGrid[targetRow][targetCol]
            newGrid[targetRow][targetCol] = piece.copy(isLocked = true)
        } else {
            // Plain swap.
            val tmp = newGrid[pieceRow][pieceCol]
            newGrid[pieceRow][pieceCol] = newGrid[targetRow][targetCol]
            newGrid[targetRow][targetCol] = tmp
        }

        // Auto-lock anything that ended up in its correct position (also matches web).
        var locked = 0
        for (r in 0 until sz) {
            for (c in 0 until sz) {
                val p = newGrid[r][c]
                val finalPiece =
                    if (p.correctRow == r && p.correctCol == c && !p.isLocked) p.copy(isLocked = true)
                    else p
                newGrid[r][c] = finalPiece
                if (finalPiece.isLocked) locked++
            }
        }

        return state.copy(
            grid = newGrid,
            lockedCount = locked,
            isWon = locked == state.totalPieces,
        )
    }
}
