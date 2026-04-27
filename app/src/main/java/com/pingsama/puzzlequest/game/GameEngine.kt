package com.pingsama.puzzlequest.game

/**
 * Pure data + functions describing puzzle state. Mirrors the web app's
 * `client/src/lib/gameEngineV2.ts` 1:1 in behavior:
 *
 *  • Pieces are addressed by a 2-D grid of [GridPiece]s.
 *  • Each piece remembers the (row, col) it BELONGS in (`correctRow`/`correctCol`).
 *  • Initial placement is shuffled until no piece happens to start at its correct spot.
 *  • Correctly placed pieces are NOT locked and can still be moved.
 *  • Adjacent correctly placed pieces move together as a group.
 *  • Wrong pieces can replace correct pieces (swap allowed).
 *  • Win == every piece is in its correct position.
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
    val correctCount: Int,  // Number of pieces in correct positions
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
            correctCount = 0,
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
     * Find all pieces that are correctly placed and adjacent to the piece at (row, col).
     * Returns a set of (row, col) coordinates representing the connected group.
     */
    private fun findConnectedCorrectGroup(
        grid: List<List<GridPiece>>,
        startRow: Int,
        startCol: Int,
        gridSize: Int,
    ): Set<Pair<Int, Int>> {
        val group = mutableSetOf<Pair<Int, Int>>()
        val queue = mutableListOf(Pair(startRow, startCol))

        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeAt(0)
            if (Pair(r, c) in group) continue
            if (r !in 0 until gridSize || c !in 0 until gridSize) continue

            val piece = grid[r][c]
            if (piece.correctRow != r || piece.correctCol != c) continue

            group.add(Pair(r, c))

            // Check all 4 adjacent cells (up, down, left, right)
            queue.add(Pair(r - 1, c))
            queue.add(Pair(r + 1, c))
            queue.add(Pair(r, c - 1))
            queue.add(Pair(r, c + 1))
        }

        return group
    }

    /**
     * Move a piece (or group of pieces) from (pieceRow, pieceCol) onto (targetRow, targetCol).
     * If the piece at source is correctly placed and adjacent to other correct pieces,
     * move the entire connected group.
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
        if (pieceRow == targetRow && pieceCol == targetCol) return state // dropped on self → no-op

        val piece = state.grid[pieceRow][pieceCol]

        // Check if this piece is part of a connected correct group
        val isCorrect = piece.correctRow == pieceRow && piece.correctCol == pieceCol
        val group = if (isCorrect) {
            findConnectedCorrectGroup(state.grid, pieceRow, pieceCol, sz)
        } else {
            setOf(Pair(pieceRow, pieceCol))
        }

        // Calculate offset for group movement
        val offsetRow = targetRow - pieceRow
        val offsetCol = targetCol - pieceCol

        // Create new grid
        val newGrid: MutableList<MutableList<GridPiece>> =
            state.grid.map { it.toMutableList() }.toMutableList()

        // If moving a group, we need to handle the swap carefully
        if (group.size > 1) {
            // Collect all pieces in the group and their destinations
            val groupPieces = group.map { (r, c) -> newGrid[r][c] }
            val destinationCells = group.map { (r, c) -> Pair(r + offsetRow, c + offsetCol) }

            // Check if all destination cells are valid
            for ((destR, destC) in destinationCells) {
                if (destR !in 0 until sz || destC !in 0 until sz) return state
            }

            // Collect pieces at destination cells
            val destPieces = destinationCells.map { (r, c) -> newGrid[r][c] }

            // Clear source cells
            for ((r, c) in group) {
                newGrid[r][c] = GridPiece(id = "empty-$r-$c", correctRow = -1, correctCol = -1)
            }

            // Place group pieces at destinations
            for (i in group.indices) {
                val (destR, destC) = destinationCells[i]
                newGrid[destR][destC] = groupPieces[i]
            }

            // Place destination pieces at source cells (swap)
            val sourceList = group.toList()
            for (i in destPieces.indices) {
                val (srcR, srcC) = sourceList[i]
                newGrid[srcR][srcC] = destPieces[i]
            }
        } else {
            // Single piece: simple swap
            val tmp = newGrid[pieceRow][pieceCol]
            newGrid[pieceRow][pieceCol] = newGrid[targetRow][targetCol]
            newGrid[targetRow][targetCol] = tmp
        }

        // Count correct pieces
        var correct = 0
        for (r in 0 until sz) {
            for (c in 0 until sz) {
                val p = newGrid[r][c]
                if (p.correctRow == r && p.correctCol == c) correct++
            }
        }

        // Check win condition: all pieces in correct positions
        val isWon = correct == state.totalPieces

        return state.copy(
            grid = newGrid,
            correctCount = correct,
            isWon = isWon,
        )
    }
}
