package com.pingsama.puzzlequest.ui

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

/**
 * Active gameplay screen — port of `client/src/pages/GameScreenV2.tsx`.
 *
 * Layout (top → bottom):
 *  1. Header row — "Level N" on the left, "Moves" + countdown on the right
 *  2. Pieces-placed counter
 *  3. The puzzle board (grows to fill available square space)
 *  4. Drag instructions
 *  5. Pill button row: Hint • Restart • Home • More Time
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
    var showWin by remember(currentLevel) { mutableStateOf(false) }
    var showLose by remember(currentLevel) { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }

    // Persist the current level whenever it advances.
    LaunchedEffect(currentLevel) { leaderboard.saveCurrentLevel(currentLevel) }

    // Countdown timer — runs only when no popup is showing.
    LaunchedEffect(currentLevel, showWin, showLose) {
        if (showWin || showLose) return@LaunchedEffect
        while (timeRemaining > 0) {
            delay(1000L)
            timeRemaining -= 1
        }
        if (timeRemaining <= 0) showLose = true
    }

    // Helper: hard-reset the current level (used by Restart and the lose popup).
    fun resetLevel() {
        gameState = GameEngine.initialize("level-$currentLevel", levelConfig.gridSize)
        moveCount = 0
        timeRemaining = timeLimit
        moreTimeUsed = false
        showHint = false
        showWin = false
        showLose = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ----- 1. Header -----
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Level $currentLevel",
                    style = TextStyle(
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        brush = Brush.horizontalGradient(listOf(Coral, Teal)),
                    ),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Moves: $moveCount",
                        color = Coral,
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = formatTime(timeRemaining),
                        color = Teal,
                        fontFamily = BodyFamily,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ----- 2. Pieces counter -----
            Text(
                text = "${gameState.lockedCount} / ${gameState.totalPieces} Pieces Placed",
                color = Teal,
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )

            Spacer(Modifier.height(8.dp))

            // ----- 3. Puzzle board (fills the available square) -----
            PuzzleBoard(
                gameState = gameState,
                pieceBitmaps = levelImage?.pieces,
                onMove = { newState ->
                    gameState = newState
                    moveCount += 1
                },
                onLockSound = { audio.playSnap() },
                onSwapSound = { audio.playSwap() },
                onWin = {
                    audio.playWin()
                    val timeUsed = timeLimit - timeRemaining
                    leaderboard.saveScore(levelConfig.gridSize, moveCount, timeUsed)
                    leaderboard.savePuzzlesCompleted(leaderboard.puzzlesCompleted() + 1)
                    showWin = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "\uD83C\uDFAF  Drag pieces. Drop on correct spot to lock.",
                color = TextMuted,
                fontFamily = BodyFamily,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.weight(1f))

            // ----- 5. Bottom button row -----
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillButton(
                    label = "Hint",
                    icon = "\uD83D\uDCA1", // 💡
                    gradient = listOf(YellowWarm, OrangeWarm),
                    textColor = TextDark,
                    onClick = { audio.playHint(); showHint = true },
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
                    label = "More Time",
                    icon = "\u23F1", // ⏱
                    gradient = if (moreTimeUsed)
                        listOf(Color(0xFFEDEDED), Color(0xFFEDEDED))
                    else listOf(OrangeWarm, Coral),
                    textColor = TextDark,
                    onClick = {
                        if (!moreTimeUsed) {
                            timeRemaining = (timeRemaining + 60).coerceAtMost(timeLimit + 300)
                            moreTimeUsed = true
                        }
                    },
                    enabled = !moreTimeUsed,
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 11,
                )
            }
        }

        // ----- Overlays -----
        if (showHint && levelImage != null) {
            HintOverlay(image = levelImage.source, onClose = { showHint = false })
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
        modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
        contentAlignment = Alignment.Center,
    ) {
        PopupCard(modifier = Modifier.fillMaxWidth(0.9f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83C\uDF89", fontSize = 44.sp) // 🎉
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Puzzle Complete!",
                    style = TextStyle(
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        brush = Brush.horizontalGradient(listOf(Coral, Teal)),
                        textAlign = TextAlign.Center,
                    ),
                )
                Spacer(Modifier.height(12.dp))

                if (completedImage != null) {
                    Image(
                        bitmap = completedImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp)),
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F7FF))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Moves: $moves",
                        color = Coral,
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "Time: ${formatTime(timeUsed)}",
                        color = Teal,
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    if (bestMoves != null && bestTimeUsed != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Best: $bestMoves moves in ${formatTime(bestTimeUsed)}",
                            color = TextMuted,
                            fontFamily = BodyFamily,
                            fontSize = 11.sp,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Great job! You've completed $puzzlesCompleted puzzle" +
                            if (puzzlesCompleted == 1) "!" else "s!",
                    color = TextMuted,
                    fontFamily = BodyFamily,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(14.dp))

                PillButton(
                    label = "Next Level",
                    icon = "\u27A1", // ➡
                    gradient = listOf(Teal, Color(0xFF44B8B0)),
                    onClick = onNext,
                    modifier = Modifier.fillMaxWidth(),
                    height = 52,
                    fontSize = 14,
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    label = "Retry Level",
                    icon = "\uD83D\uDD04", // 🔄
                    gradient = listOf(Coral, CoralLight),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    height = 52,
                    fontSize = 14,
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    label = "Back to Menu",
                    gradient = listOf(Color(0xFFE6E8EB), Color(0xFFD9DCE0)),
                    textColor = TextDark,
                    onClick = onMenu,
                    modifier = Modifier.fillMaxWidth(),
                    height = 52,
                    fontSize = 14,
                )
            }
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
        modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
        contentAlignment = Alignment.Center,
    ) {
        PopupCard(modifier = Modifier.fillMaxWidth(0.85f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83D\uDE22", fontSize = 44.sp) // 😢
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "You Lost",
                    style = TextStyle(
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        brush = Brush.horizontalGradient(listOf(Coral, CoralLight)),
                    ),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Time's up! You didn't complete the puzzle in time.",
                    color = TextMuted,
                    fontFamily = BodyFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))

                PillButton(
                    label = "Restart Level",
                    icon = "\uD83D\uDD04",
                    gradient = listOf(Coral, CoralLight),
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    height = 52,
                    fontSize = 14,
                )
                if (moreTimeAvailable) {
                    Spacer(Modifier.height(8.dp))
                    PillButton(
                        label = "More Time",
                        icon = "\u23F1",
                        gradient = listOf(YellowWarm, OrangeWarm),
                        textColor = TextDark,
                        onClick = onMoreTime,
                        modifier = Modifier.fillMaxWidth(),
                        height = 52,
                        fontSize = 14,
                    )
                }
            }
        }
    }
}

@Composable
private fun RestartConfirmDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0x80000000)),
        contentAlignment = Alignment.Center,
    ) {
        PopupCard(modifier = Modifier.fillMaxWidth(0.85f)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("\uD83D\uDD04", fontSize = 36.sp) // 🔄
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Restart Level?",
                    color = TextDark,
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Your progress on this level will be reset.",
                    color = TextMuted,
                    fontFamily = BodyFamily,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                PillButton(
                    label = "Yes, Restart",
                    gradient = listOf(Coral, CoralLight),
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    height = 50,
                    fontSize = 14,
                )
                Spacer(Modifier.height(8.dp))
                PillButton(
                    label = "No, Cancel",
                    gradient = listOf(Color(0xFFE6E8EB), Color(0xFFD9DCE0)),
                    textColor = TextDark,
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    height = 50,
                    fontSize = 14,
                )
            }
        }
    }
}
