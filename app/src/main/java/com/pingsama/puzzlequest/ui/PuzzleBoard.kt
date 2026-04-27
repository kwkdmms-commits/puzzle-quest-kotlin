package com.pingsama.puzzlequest.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pingsama.puzzlequest.game.GameEngine
import com.pingsama.puzzlequest.game.GameState
import com.pingsama.puzzlequest.ui.theme.BoardMint
import kotlin.math.min

/**
 * Drag-and-drop puzzle board.
 *
 *  • Press any piece → it lifts (alpha drops on the in-grid copy, a floating
 *    overlay follows your finger).
 *  • Release on a different cell → unconditional swap (the piece that was there
 *    moves to where this one came from). Correctness/locking does NOT block.
 *  • Release outside the board → cancelled, nothing changes.
 *
 * @param pieceBitmaps   Indexed by [correctRow][correctCol]. `null` while loading.
 */
@Composable
fun PuzzleBoard(
    gameState: GameState,
    pieceBitmaps: List<List<ImageBitmap>>?,
    onMove: (GameState) -> Unit,
    onSwapSound: () -> Unit,
    onWin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridSize = gameState.gridSize
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val boardSizeDp = min(maxWidth.value, maxHeight.value).dp
        val pieceSizeDp = boardSizeDp / gridSize
        val pieceSizePx = with(density) { pieceSizeDp.toPx() }

        var draggedCell by remember(gameState) { mutableStateOf<Pair<Int, Int>?>(null) }
        var pointerOffset by remember(gameState) { mutableStateOf(Offset.Zero) }

        // Subtle scale-in when a new level starts.
        var loaded by remember(gameState) { mutableStateOf(false) }
        LaunchedEffect(gameState) { loaded = true }
        val boardScale by animateFloatAsState(
            targetValue = if (loaded) 1f else 0.92f,
            animationSpec = tween(durationMillis = 220),
            label = "board-spawn",
        )

        Box(
            modifier = Modifier
                .size(boardSizeDp)
                .scale(boardScale)
                .background(BoardMint, RoundedCornerShape(16.dp))
                .pointerInput(gameState, pieceSizePx, gridSize) {
                    awaitPointerEventScope {
                        while (true) {
                            // Wait for a finger to come down on any in-bounds cell.
                            val downChange = awaitPointerEvent(PointerEventPass.Main)
                                .changes.firstOrNull { it.pressed && !it.previousPressed }
                                ?: continue

                            val startCol = (downChange.position.x / pieceSizePx).toInt()
                            val startRow = (downChange.position.y / pieceSizePx).toInt()
                            val inBounds = startRow in 0 until gridSize &&
                                           startCol in 0 until gridSize

                            val pointerId = downChange.id
                            if (!inBounds) {
                                // Wait for this finger to lift, then loop.
                                while (true) {
                                    val ev = awaitPointerEvent(PointerEventPass.Main)
                                    val ch = ev.changes.firstOrNull { it.id == pointerId } ?: break
                                    if (!ch.pressed) break
                                }
                                continue
                            }

                            draggedCell = startRow to startCol
                            pointerOffset = downChange.position
                            downChange.consume()

                            // Track movement until release.
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                                if (change.pressed) {
                                    pointerOffset = change.position
                                    change.consume()
                                } else {
                                    val dropCol = (change.position.x / pieceSizePx).toInt()
                                    val dropRow = (change.position.y / pieceSizePx).toInt()
                                    handleDrop(
                                        state = gameState,
                                        srcRow = startRow, srcCol = startCol,
                                        dropRow = dropRow, dropCol = dropCol,
                                        gridSize = gridSize,
                                        onMove = onMove,
                                        onSwapSound = onSwapSound,
                                        onWin = onWin,
                                    )
                                    change.consume()
                                    break
                                }
                            }
                            draggedCell = null
                        }
                    }
                },
        ) {
            // Draw all pieces in their current grid positions.
            if (pieceBitmaps != null) {
                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        val piece = gameState.grid[row][col]
                        val bitmap = pieceBitmaps[piece.correctRow][piece.correctCol]
                        val isDragged = draggedCell?.let { it.first == row && it.second == col } == true

                        Box(
                            modifier = Modifier
                                .size(pieceSizeDp)
                                .offset(x = pieceSizeDp * col, y = pieceSizeDp * row)
                                .alpha(if (isDragged) 0.25f else 1f),
                        ) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier.size(pieceSizeDp),
                            )
                        }
                    }
                }
            }

            // Floating "ghost" piece following the finger.
            val dragged = draggedCell
            if (dragged != null && pieceBitmaps != null) {
                val (dr, dc) = dragged
                val piece = gameState.grid[dr][dc]
                val bitmap = pieceBitmaps[piece.correctRow][piece.correctCol]
                Box(
                    modifier = Modifier
                        .size(pieceSizeDp)
                        .offset {
                            IntOffset(
                                (pointerOffset.x - pieceSizePx / 2f).toInt(),
                                (pointerOffset.y - pieceSizePx / 2f).toInt(),
                            )
                        }
                        .zIndex(20f)
                        .shadow(elevation = 14.dp, shape = RoundedCornerShape(8.dp), clip = false)
                        .clip(RoundedCornerShape(8.dp)),
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.size(pieceSizeDp),
                    )
                }
            }
        }
    }
}

private fun handleDrop(
    state: GameState,
    srcRow: Int, srcCol: Int,
    dropRow: Int, dropCol: Int,
    gridSize: Int,
    onMove: (GameState) -> Unit,
    onSwapSound: () -> Unit,
    onWin: () -> Unit,
) {
    if (dropRow !in 0 until gridSize || dropCol !in 0 until gridSize) return
    if (srcRow == dropRow && srcCol == dropCol) return

    val newState = GameEngine.placePiece(state, srcRow, srcCol, dropRow, dropCol)
    if (newState === state) return

    onMove(newState)
    onSwapSound()
    if (newState.isWon) onWin()
}
