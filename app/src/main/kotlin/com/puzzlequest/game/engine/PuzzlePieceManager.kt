package com.puzzlequest.game.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.puzzlequest.game.data.Level

class PuzzlePieceManager(private val context: Context, private val level: Level) {
    private val gridSize = level.gridSize
    private val pieceBitmaps = mutableMapOf<String, Bitmap>()
    private var fullImageBitmap: Bitmap? = null
    private var pieceWidthOriginal = 0
    private var pieceHeightOriginal = 0

    init {
        loadAndSliceImage()
    }

    private fun loadAndSliceImage() {
        // Load the full image from resources
        fullImageBitmap = BitmapFactory.decodeResource(context.resources, level.imageResId)
        
        fullImageBitmap?.let { bitmap ->
            // Calculate piece size based on image dimensions and grid size
            pieceWidthOriginal = bitmap.width / gridSize
            pieceHeightOriginal = bitmap.height / gridSize

            // Slice the image into pieces
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val x = col * pieceWidthOriginal
                    val y = row * pieceHeightOriginal
                    
                    // Extract the piece bitmap
                    val pieceBitmap = Bitmap.createBitmap(bitmap, x, y, pieceWidthOriginal, pieceHeightOriginal)
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
