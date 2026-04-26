package com.puzzlequest.game.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.puzzlequest.game.data.Level
import kotlin.math.min

class PuzzlePieceManager(private val context: Context, private val level: Level) {
    private val gridSize = level.gridSize
    private val pieceBitmaps = mutableMapOf<String, Bitmap>()
    private var fullImageBitmap: Bitmap? = null

    init {
        loadAndSliceImage()
    }

    private fun loadAndSliceImage() {
        // Load the full image from resources
        var bitmap = BitmapFactory.decodeResource(context.resources, level.imageResId)
        
        bitmap?.let {
            // Scale image to square first (use smaller dimension)
            val squareSize = min(it.width, it.height)
            val scaledBitmap = if (it.width != squareSize || it.height != squareSize) {
                val startX = (it.width - squareSize) / 2
                val startY = (it.height - squareSize) / 2
                Bitmap.createBitmap(it, startX, startY, squareSize, squareSize)
            } else {
                it
            }
            
            fullImageBitmap = scaledBitmap
            
            // Calculate piece size based on square image and grid size
            val pieceSize = squareSize / gridSize

            // Slice the image into pieces
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val x = col * pieceSize
                    val y = row * pieceSize
                    
                    // Extract the piece bitmap
                    val pieceBitmap = Bitmap.createBitmap(scaledBitmap, x, y, pieceSize, pieceSize)
                    val pieceId = "piece-$row-$col"
                    pieceBitmaps[pieceId] = pieceBitmap
                }
            }
        }
    }

    fun getPieceBitmap(pieceId: String): Bitmap? {
        return pieceBitmaps[pieceId]
    }

    fun getGridSize(): Int {
        return gridSize
    }

    fun cleanup() {
        pieceBitmaps.values.forEach { it.recycle() }
        pieceBitmaps.clear()
        fullImageBitmap?.recycle()
        fullImageBitmap = null
    }
}
