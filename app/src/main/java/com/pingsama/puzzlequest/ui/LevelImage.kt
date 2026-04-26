package com.pingsama.puzzlequest.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * One level's image, ready to be drawn:
 *  • [source] is the full image (used for the hint overlay and the win-popup preview).
 *  • [pieces] is sliced as `pieces[correctRow][correctCol]` so PuzzleBoard can hand each
 *    piece its own bitmap by the position it BELONGS in (which travels with the piece
 *    even after shuffling).
 */
data class LevelImage(
    val source: ImageBitmap,
    val pieces: List<List<ImageBitmap>>,
)

/**
 * Load a WebP from `assets/`, slice it into [gridSize]×[gridSize] pieces on the IO
 * dispatcher, and return the result. Returns `null` while loading.
 */
@Composable
fun rememberLevelImage(assetPath: String, gridSize: Int): LevelImage? {
    val context = LocalContext.current.applicationContext
    var image by remember(assetPath, gridSize) { mutableStateOf<LevelImage?>(null) }

    LaunchedEffect(assetPath, gridSize) {
        image = withContext(Dispatchers.IO) {
            runCatching {
                val bitmap: Bitmap = context.assets.open(assetPath).use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return@runCatching null

                // Make sure the source is exactly divisible by gridSize so every piece
                // is the same size — crop a few pixels off the edges if necessary.
                val side = (bitmap.width.coerceAtMost(bitmap.height) / gridSize) * gridSize
                val squared: Bitmap =
                    if (bitmap.width == side && bitmap.height == side) bitmap
                    else Bitmap.createBitmap(bitmap, 0, 0, side, side)

                val pieceSize = side / gridSize
                val pieces = List(gridSize) { row ->
                    List(gridSize) { col ->
                        Bitmap.createBitmap(
                            squared, col * pieceSize, row * pieceSize, pieceSize, pieceSize,
                        ).asImageBitmap()
                    }
                }
                LevelImage(source = squared.asImageBitmap(), pieces = pieces)
            }.getOrNull()
        }
    }

    return image
}
