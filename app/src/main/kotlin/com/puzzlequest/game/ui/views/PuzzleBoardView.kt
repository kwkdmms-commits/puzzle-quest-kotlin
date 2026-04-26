package com.puzzlequest.game.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.puzzlequest.game.engine.GameEngine

class PuzzleBoardView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var gameEngine: GameEngine? = null
    private var onPieceLocked: (() -> Unit)? = null
    private var onStateChange: (() -> Unit)? = null
    
    private var draggedCell: Pair<Int, Int>? = null
    private var boardInitialized = false

    private val gridPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    private val lockedBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.CYAN
        strokeWidth = 3f
    }

    private val normalBorderPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.DKGRAY
        strokeWidth = 1f
    }

    fun setGameEngine(engine: GameEngine) {
        this.gameEngine = engine
        invalidate()
    }

    fun setOnPieceLocked(callback: () -> Unit) {
        this.onPieceLocked = callback
    }

    fun setOnStateChange(callback: () -> Unit) {
        this.onStateChange = callback
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        if (!boardInitialized && gameEngine != null) {
            gameEngine?.setBoardDimensions(w, h)
            boardInitialized = true
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        gameEngine?.let { engine ->
            val pieceManager = engine.getPieceManager()
            val pieceSize = engine.getPieceSize()
            val gridSize = engine.getGridSize()
            val boardSize = engine.getBoardSize()
            val gameState = engine.getGameState()

            // Draw board background
            canvas.drawRect(0f, 0f, boardSize.toFloat(), boardSize.toFloat(), Paint().apply {
                color = Color.parseColor("#95E1D3")
                style = Paint.Style.FILL
            })

            // Draw grid lines
            for (i in 0..gridSize) {
                val pos = i * pieceSize
                canvas.drawLine(pos.toFloat(), 0f, pos.toFloat(), boardSize.toFloat(), gridPaint)
                canvas.drawLine(0f, pos.toFloat(), boardSize.toFloat(), pos.toFloat(), gridPaint)
            }

            // Draw pieces (unlocked first, then locked on top)
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val piece = gameState.grid[row][col]
                    if (!piece.isLocked) {
                        drawPuzzlePiece(canvas, piece, row, col, pieceManager, pieceSize)
                    }
                }
            }

            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val piece = gameState.grid[row][col]
                    if (piece.isLocked) {
                        drawPuzzlePiece(canvas, piece, row, col, pieceManager, pieceSize)
                    }
                }
            }
        }
    }

    private fun drawPuzzlePiece(
        canvas: Canvas,
        piece: com.puzzlequest.game.engine.GridPiece,
        displayRow: Int,
        displayCol: Int,
        pieceManager: com.puzzlequest.game.engine.PuzzlePieceManager,
        pieceSize: Int
    ) {
        val bitmap = pieceManager.getPieceBitmap(piece.id)
        
        if (bitmap != null) {
            val x = displayCol * pieceSize
            val y = displayRow * pieceSize
            
            // Draw the bitmap piece scaled to exact piece size
            canvas.drawBitmap(bitmap, null, android.graphics.Rect(x, y, x + pieceSize, y + pieceSize), null)
        }

        // Draw border
        val x = displayCol * pieceSize
        val y = displayRow * pieceSize
        val border = if (piece.isLocked) lockedBorderPaint else normalBorderPaint
        canvas.drawRect(x.toFloat(), y.toFloat(), (x + pieceSize).toFloat(), (y + pieceSize).toFloat(), border)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gameEngine ?: return false
        
        val pieceSize = gameEngine?.getPieceSize() ?: return false
        val gridSize = gameEngine?.getGridSize() ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val col = (event.x / pieceSize).toInt()
                val row = (event.y / pieceSize).toInt()
                
                if (row in 0 until gridSize && col in 0 until gridSize) {
                    val piece = gameEngine?.getGameState()?.grid?.get(row)?.get(col)
                    if (piece != null && !piece.isLocked) {
                        draggedCell = Pair(row, col)
                        invalidate()
                        return true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (draggedCell != null) {
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (draggedCell != null) {
                    val toCol = (event.x / pieceSize).toInt()
                    val toRow = (event.y / pieceSize).toInt()
                    
                    if (toRow in 0 until gridSize && toCol in 0 until gridSize) {
                        val (fromRow, fromCol) = draggedCell!!
                        gameEngine?.placePiece(fromRow, fromCol, toRow, toCol)
                        onStateChange?.invoke()
                        
                        // Check if piece was locked
                        val piece = gameEngine?.getGameState()?.grid?.get(toRow)?.get(toCol)
                        if (piece?.isLocked == true) {
                            onPieceLocked?.invoke()
                        }
                    }
                    
                    draggedCell = null
                    invalidate()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }
}
