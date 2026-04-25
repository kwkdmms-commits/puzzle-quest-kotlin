package com.puzzlequest.game.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.puzzlequest.game.data.PuzzlePiece
import com.puzzlequest.game.engine.GameEngine

class PuzzleBoardView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var gameEngine: GameEngine? = null
    private var onPieceLocked: (() -> Unit)? = null
    private var draggedPieceId: Int? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var boardInitialized = false

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 1f
    }

    private val lockedBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = 3f
    }

    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    fun setGameEngine(engine: GameEngine) {
        this.gameEngine = engine
        invalidate()
    }

    fun setOnPieceLocked(callback: () -> Unit) {
        this.onPieceLocked = callback
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Initialize board dimensions for the game engine
        if (!boardInitialized && gameEngine != null) {
            val boardSize = minOf(w, h)
            val offsetX = (w - boardSize) / 2f
            val offsetY = (h - boardSize) / 2f
            gameEngine?.setBoardDimensions(boardSize, offsetX, offsetY)
            boardInitialized = true
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        gameEngine?.let { engine ->
            val pieceManager = engine.getPieceManager()
            val pieceSize = engine.getPieceSize()
            val gridSize = pieceManager.getGridSize()

            // Draw grid lines
            drawGrid(canvas, gridSize, pieceSize)

            // Draw pieces (locked pieces on top)
            val unlockedPieces = engine.getPieces().filter { !it.isLocked }
            val lockedPieces = engine.getPieces().filter { it.isLocked }

            unlockedPieces.forEach { piece ->
                drawPuzzlePiece(canvas, piece, pieceManager, pieceSize)
            }

            lockedPieces.forEach { piece ->
                drawPuzzlePiece(canvas, piece, pieceManager, pieceSize)
            }
        }
    }

    private fun drawGrid(canvas: Canvas, gridSize: Int, pieceSize: Int) {
        // Draw grid lines to show the puzzle grid
        for (i in 0..gridSize) {
            val pos = i * pieceSize
            canvas.drawLine(pos.toFloat(), 0f, pos.toFloat(), (gridSize * pieceSize).toFloat(), gridPaint)
            canvas.drawLine(0f, pos.toFloat(), (gridSize * pieceSize).toFloat(), pos.toFloat(), gridPaint)
        }
    }

    private fun drawPuzzlePiece(canvas: Canvas, piece: PuzzlePiece, pieceManager: com.puzzlequest.game.engine.PuzzlePieceManager, pieceSize: Int) {
        val bitmap = pieceManager.getPieceBitmap(piece.id)
        
        if (bitmap != null) {
            // Draw the bitmap piece
            canvas.drawBitmap(
                bitmap,
                piece.currentX,
                piece.currentY,
                null
            )
        }

        // Draw border
        val border = if (piece.isLocked) lockedBorderPaint else borderPaint
        canvas.drawRect(
            piece.currentX,
            piece.currentY,
            piece.currentX + pieceSize,
            piece.currentY + pieceSize,
            border
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                draggedPieceId = findPieceAt(event.x, event.y)
                if (draggedPieceId != null) {
                    val piece = gameEngine?.getPieces()?.find { it.id == draggedPieceId }
                    if (piece != null && !piece.isLocked) {
                        dragOffsetX = event.x - piece.currentX
                        dragOffsetY = event.y - piece.currentY
                    } else {
                        draggedPieceId = null
                    }
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggedPieceId != null) {
                    val newX = event.x - dragOffsetX
                    val newY = event.y - dragOffsetY
                    gameEngine?.movePiece(draggedPieceId!!, newX, newY)
                    invalidate()
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (draggedPieceId != null) {
                    val locked = gameEngine?.checkAndLockPiece(draggedPieceId!!)
                    if (locked == true) {
                        onPieceLocked?.invoke()
                    }
                    draggedPieceId = null
                    invalidate()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun findPieceAt(x: Float, y: Float): Int? {
        val pieceSize = gameEngine?.getPieceSize() ?: return null
        // Find the topmost piece at this position (reverse order to get the last drawn piece)
        return gameEngine?.getPieces()?.findLast { piece ->
            x >= piece.currentX && x <= piece.currentX + pieceSize &&
                    y >= piece.currentY && y <= piece.currentY + pieceSize
        }?.id
    }
}
