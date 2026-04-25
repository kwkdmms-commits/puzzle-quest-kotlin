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

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.GRAY
    }

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = 2f
    }

    private val lockedBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = 4f
    }

    fun setGameEngine(engine: GameEngine) {
        this.gameEngine = engine
        invalidate()
    }

    fun setOnPieceLocked(callback: () -> Unit) {
        this.onPieceLocked = callback
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        gameEngine?.getPieces()?.forEach { piece ->
            drawPuzzlePiece(canvas, piece)
        }
    }

    private fun drawPuzzlePiece(canvas: Canvas, piece: PuzzlePiece) {
        val size = 100f

        // Draw piece background
        paint.color = if (piece.isLocked) Color.GREEN else Color.LTGRAY
        canvas.drawRect(
            piece.currentX,
            piece.currentY,
            piece.currentX + size,
            piece.currentY + size,
            paint
        )

        // Draw border
        val border = if (piece.isLocked) lockedBorderPaint else borderPaint
        canvas.drawRect(
            piece.currentX,
            piece.currentY,
            piece.currentX + size,
            piece.currentY + size,
            border
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                draggedPieceId = findPieceAt(event.x, event.y)
                if (draggedPieceId != null) {
                    val piece = gameEngine?.getPieces()?.find { it.id == draggedPieceId }
                    if (piece != null) {
                        dragOffsetX = event.x - piece.currentX
                        dragOffsetY = event.y - piece.currentY
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
        return gameEngine?.getPieces()?.findLast { piece ->
            x >= piece.currentX && x <= piece.currentX + 100f &&
                    y >= piece.currentY && y <= piece.currentY + 100f
        }?.id
    }
}
