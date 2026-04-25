package com.puzzlequest.game.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.puzzlequest.game.data.Level
import com.puzzlequest.game.data.PuzzlePiece

class PuzzlePieceManager(private val context: Context, private val level: Level) {
    private val gridSize = level.gridSize
    private val pieceBitmaps = mutableMapOf<Int, Bitmap>()
    private var fullImageBitmap: Bitmap? = null
    private var pieceSize = 0

    init {
        loadAndSliceImage()
    }

    private fun loadAndSliceImage() {
        // Load the full image from resources
        fullImageBitmap = BitmapFactory.decodeResource(context.resources, level.imageResId)
        
        fullImageBitmap?.let { bitmap ->
            // Calculate piece size based on image dimensions and grid size
            val pieceWidth = bitmap.width / gridSize
            val pieceHeight = bitmap.height / gridSize
            pieceSize = minOf(pieceWidth, pieceHeight)

            // Slice the image into pieces
            var pieceId = 0
            for (row in 0 until gridSize) {
                for (col in 0 until gridSize) {
                    val x = col * pieceWidth
                    val y = row * pieceHeight
                    
                    // Extract the piece bitmap
                    val pieceBitmap = Bitmap.createBitmap(bitmap, x, y, pieceWidth, pieceHeight)
                    pieceBitmaps[pieceId] = pieceBitmap
                    pieceId++
                }
            }
        }
    }

    fun getPieceBitmap(pieceId: Int): Bitmap? {
        return pieceBitmaps[pieceId]
    }

    fun getPieceSize(): Int {
        return pieceSize
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
