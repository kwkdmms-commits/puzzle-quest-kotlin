package com.pingsama.puzzlequest.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingsama.puzzlequest.audio.AudioManager
import com.pingsama.puzzlequest.game.GameEngine
import com.pingsama.puzzlequest.game.LeaderboardManager
import com.pingsama.puzzlequest.game.LevelSystem
import com.pingsama.puzzlequest.game.formatTime
import com.pingsama.puzzlequest.ui.theme.BodyFamily
import com.pingsama.puzzlequest.ui.theme.Coral
import com.pingsama.puzzlequest.ui.theme.CoralLight
import com.pingsama.puzzlequest.ui.theme.DisplayFamily
import com.pingsama.puzzlequest.ui.theme.OrangeWarm
import com.pingsama.puzzlequest.ui.theme.Teal
import com.pingsama.puzzlequest.ui.theme.TextDark
import com.pingsama.puzzlequest.ui.theme.TextMuted
import com.pingsama.puzzlequest.ui.theme.YellowWarm
import kotlinx.coroutines.delay

private const val TAG = "GameScreen"

/**
 * Active gameplay screen — port of `client/src/pages/GameScreenV2.tsx`.
 *
 * Layout (top → bottom):
 *  1. Header row — "Level N" on the left, "Moves" + countdown on the right
 *  2. Flex spacer (top)
 *  3. Pieces-placed counter
 *  4. The puzzle board (centered)
 *  5. Drag instructions
 *  6. Button row: Home • Restart • Hint • More Time (directly under puzzle)
 *  7. Flex spacer (middle)
 *  8. Ad banner space (fixed 60dp)
 *
 * Overlays: hint preview, win popup, lose popup, restart confirmation.
 */
@Composable
fun GameScreen(
    currentLevel: Int,
    audio: AudioManager,
    leaderboard: LeaderboardManager,
    onLevelComplete: () -> Unit,
    onBackToMenu: () -> Unit,
) {
    val levelConfig = remember(currentLevel) { LevelSystem.configFor(currentLevel) }
    val timeLimit = remember(currentLevel) { LevelSystem.timeLimitFor(currentLevel) }
    val assetPath = remember(currentLevel) { LevelSystem.assetForLevel(currentLevel) }
    val levelImage = rememberLevelImage(assetPath, levelConfig.gridSize)

    // All gameplay state is keyed on currentLevel so it auto-resets per level.
    var gameState by remember(currentLevel) {
        mutableStateOf(GameEngine.initialize("level-$currentLevel", levelConfig.gridSize))
    }
    var moveCount by remember(currentLevel) { mutableIntStateOf(0) }
    var timeRemaining by remember(currentLevel) { mutableIntStateOf(timeLimit) }
    var moreTimeUsed by remember(currentLevel) { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var hintError by remember { mutableStateOf<String?>(null) }
    var showWin by remember(currentLevel) { mutableStateOf(false) }
    var showLose by remember(currentLevel) { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }

    // Persist the current level whenever it advances.
    LaunchedEffect(currentLevel) { leaderboard.saveCurrentLevel(currentLevel) }

    // Countdown timer — runs only when no popup is showing.
    LaunchedEffect(showWin, showLose) {
        while (!showWin && !showLose && timeRemaining > 0) {
            delay(1000)
            timeRemaining--
            if (timeRemaining == 0) showLose = true
        }
    }

    fun resetLevel() {
        gameState = GameEngine.initialize("level-$currentLevel", levelConfig.gridSize)
        moveCount = 0
        timeRemaining = timeLimit
        moreTimeUsed = false
        showWin = false
        showLose = false
        showHint = false
        hintError = null
    }

    fun handleHintClick() {
        try {
            Log.d(TAG, "HINT_CLICKED")
            audio.playHint()
            
            if (levelImage == null) {
                Log.w(TAG, "HINT_FAILED: levelImage is null (still loading)")
                hintError = "Hint image is still loading. Please try again."
                return
            }
            
            if (levelImage.source == null) {
                Log.w(TAG, "HINT_FAILED: levelImage.source is null")
                hintError = "Hint image failed to load."
                return
            }
            
            Log.d(TAG, "HINT_IMAGE_LOADED")
            Log.d(TAG, "HINT_DIALOG_OPEN")
            showHint = true
            hintError = null
        } catch (e: Exception) {
            Log.e(TAG, "HINT_FAILED: ${e.javaClass.simpleName}: ${e.message}", e)
            hintError = "Hint unavailable: ${e.message ?: "Unknown error"}"
            showHint = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF0F8FF), Color(0xFFE6F2FF))
                )
            )
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ----- 1. Header row -----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Level $currentLevel",
                    style = TextStyle(
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Coral,
                    ),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Moves: $moveCount",
                        style = TextStyle(
                            fontFamily = DisplayFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Coral,
                        ),
                    )
                    Text(
                        text = formatTime(timeRemaining),
                        style = TextStyle(
                            fontFamily = BodyFamily,
                            fontSize = 14.sp,
                            color = Teal,
                        ),
                    )
                }
            }

            // ----- 2. Flex spacer (top) -----
            Spacer(Modifier.weight(0.3f))

            // ----- 3. Pieces counter -----
            Text(
                text = "${gameState.lockedCount} / ${gameState.totalPieces} Pieces Placed",
                style = TextStyle(
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Teal,
                ),
            )

            Spacer(Modifier.height(12.dp))

            // ----- 4. Puzzle board -----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(horizontal = 8.dp),
            ) {
                PuzzleBoard(
                    gameState = gameState,
                    pieceBitmaps = levelImage?.pieces,
                    onMove = { moveCount++ },
                    onLockSound = { audio.playSnap() },
                    onSwapSound = { audio.playSwap() },
                    onWin = { showWin = true },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ----- 5. Instructions -----
            Text(
                text = "\uD83C\uDFAF  Drag pieces. Drop on correct spot to lock.",
                color = TextMuted,
                fontFamily = BodyFamily,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))

            // ----- 6. Button row (directly under puzzle) -----
            // Order: Home - Restart - Hint - More Time
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillButton(
                    label = "Home",
                    icon = "\uD83C\uDFE0", // 🏠
                    gradient = listOf(Color(0xFFE6E8EB), Color(0xFFD9DCE0)),
                    textColor = TextDark,
                    onClick = onBackToMenu,
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 13,
                )
                PillButton(
                    label = "Restart",
                    icon = "\uD83D\uDD04", // 🔄
                    gradient = listOf(Coral, CoralLight),
                    onClick = { showRestartConfirm = true },
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 13,
                )
                PillButton(
                    label = "Hint",
                    icon = "\uD83D\uDCA1", // 💡
                    gradient = listOf(YellowWarm, OrangeWarm),
                    textColor = TextDark,
                    onClick = { handleHintClick() },
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 13,
                )
                PillButton(
                    label = "More Time",
                    icon = "\u23F1", // ⏱
                    gradient = if (moreTimeUsed)
                        listOf(Color(0xFFEDEDED), Color(0xFFEDEDED))
                    else
                        listOf(Coral, CoralLight),
                    textColor = if (moreTimeUsed) TextMuted else Color.White,
                    onClick = {
                        if (!moreTimeUsed) {
                            timeRemaining = 60
                            moreTimeUsed = true
                            showLose = false
                        }
                    },
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 13,
                    enabled = !moreTimeUsed,
                )
            }

            // ----- 7. Flex spacer (middle) -----
            Spacer(Modifier.weight(1f))

            // ----- 8. Ad banner space (fixed height) -----
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color.Transparent)
            )
        }

        // ----- Overlays -----
        if (showHint && levelImage != null) {
            HintOverlay(image = levelImage.source, onClose = { showHint = false })
        }

        if (hintError != null) {
            AlertDialog(
                onDismissRequest = { hintError = null },
                title = { Text("Hint Unavailable") },
                text = { Text(hintError ?: "Unknown error") },
                confirmButton = {
                    PillButton(
                        label = "OK",
                        gradient = listOf(Coral, CoralLight),
                        onClick = { hintError = null },
                        height = 40,
                        fontSize = 12,
                    )
                },
            )
        }

        if (showWin) {
            WinPopup(
                completedImage = levelImage?.source,
                moves = moveCount,
                timeUsed = timeLimit - timeRemaining,
                puzzlesCompleted = leaderboard.puzzlesCompleted(),
                bestMoves = leaderboard.bestMoves(levelConfig.gridSize),
                bestTimeUsed = leaderboard.bestTimeUsed(levelConfig.gridSize),
                onNext = onLevelComplete,
                onRetry = { resetLevel() },
                onMenu = onBackToMenu,
            )
        }

        if (showLose) {
            LosePopup(
                onRestart = { resetLevel() },
                onMoreTime = {
                    if (!moreTimeUsed) {
                        timeRemaining = 60
                        moreTimeUsed = true
                        showLose = false
                    }
                },
                moreTimeAvailable = !moreTimeUsed,
            )
        }

        if (showRestartConfirm) {
            RestartConfirmDialog(
                onConfirm = { showRestartConfirm = false; resetLevel() },
                onCancel = { showRestartConfirm = false },
            )
        }
    }
}

// ============================================================================
// Overlays
// ============================================================================

@Composable
private fun HintOverlay(image: ImageBitmap, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Box {
            Image(
                bitmap = image,
                contentDescription = "Hint",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(320.dp)
                    .clip(RoundedCornerShape(20.dp)),
            )
            RoundCloseButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = (-12).dp, top = (-12).dp),
            )
        }
    }
}

@Composable
private fun WinPopup(
    completedImage: ImageBitmap?,
    moves: Int,
    timeUsed: Int,
    puzzlesCompleted: Int,
    bestMoves: Int?,
    bestTimeUsed: Int?,
    onNext: () -> Unit,
    onRetry: () -> Unit,
    onMenu: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🎉 Level Complete!",
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Teal,
            )
            Spacer(Modifier.height(16.dp))

            if (completedImage != null) {
                Image(
                    bitmap = completedImage,
                    contentDescription = "Completed puzzle",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Spacer(Modifier.height(16.dp))
            }

            Text(
                text = "Moves: $moves",
                fontFamily = BodyFamily,
                fontSize = 14.sp,
                color = TextDark,
            )
            Text(
                text = "Time: ${formatTime(timeUsed)}",
                fontFamily = BodyFamily,
                fontSize = 14.sp,
                color = TextDark,
            )

            if (bestMoves != null) {
                Text(
                    text = "Best Moves: $bestMoves",
                    fontFamily = BodyFamily,
                    fontSize = 12.sp,
                    color = TextMuted,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PillButton(
                    label = "Retry",
                    gradient = listOf(Color(0xFFE6E8EB), Color(0xFFD9DCE0)),
                    textColor = TextDark,
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    height = 48,
                    fontSize = 14,
                )
                PillButton(
                    label = "Next",
                    gradient = listOf(Teal, Color(0xFF20B2AA)),
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    height = 48,
                    fontSize = 14,
                )
            }

            Spacer(Modifier.height(8.dp))

            PillButton(
                label = "Menu",
                gradient = listOf(Color(0xFFE6E8EB), Color(0xFFD9DCE0)),
                textColor = TextDark,
                onClick = onMenu,
                modifier = Modifier.fillMaxWidth(),
                height = 48,
                fontSize = 14,
            )
        }
    }
}

@Composable
private fun LosePopup(
    onRestart: () -> Unit,
    onMoreTime: () -> Unit,
    moreTimeAvailable: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(24.dp))
                .padding(24.dp)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "⏰ Time's Up!",
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Coral,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "You ran out of time. Try again!",
                fontFamily = BodyFamily,
                fontSize = 14.sp,
                color = TextDark,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            PillButton(
                label = "Restart",
                icon = "🔄",
                gradient = listOf(Coral, CoralLight),
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(),
                height = 48,
                fontSize = 14,
            )

            if (moreTimeAvailable) {
                Spacer(Modifier.height(8.dp))
                PillButton(
                    label = "More Time",
                    icon = "⏱",
                    gradient = listOf(OrangeWarm, Color(0xFFFF9500)),
                    onClick = onMoreTime,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48,
                    fontSize = 14,
                )
            }
        }
    }
}
