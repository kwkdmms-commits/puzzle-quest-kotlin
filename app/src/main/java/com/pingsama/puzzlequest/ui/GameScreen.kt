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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.pingsama.puzzlequest.ads.InterstitialAdManager
import com.pingsama.puzzlequest.ads.RewardedAdManager
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
    var hintUnlockedInLevel by remember(currentLevel) { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showWin by remember(currentLevel) { mutableStateOf(false) }
    var showLose by remember(currentLevel) { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }

    val activity = LocalContext.current as? Activity
    var levelStartTime by remember(currentLevel) { mutableStateOf(System.currentTimeMillis()) }

    // Persist the current level whenever it advances.
    LaunchedEffect(currentLevel) {
        leaderboard.saveCurrentLevel(currentLevel)
        // Preload interstitial when entering a new level
        activity?.let { InterstitialAdManager.preloadInterstitial(it) }
        // Preload rewarded ad for Hint and More Time
        activity?.let { RewardedAdManager.preloadRewardedAd(it) }
        levelStartTime = System.currentTimeMillis()
    }

    // Countdown timer — runs only when no popup is showing.
    // Also tracks total gameplay time for interstitial ad conditions
    LaunchedEffect(currentLevel, showWin, showLose) {
        if (showWin || showLose) return@LaunchedEffect
        while (timeRemaining > 0) {
            delay(1000L)
            timeRemaining -= 1
            // Update gameplay time for interstitial ad tracking
            InterstitialAdManager.updateGameplayTime(1000L)
        }
        if (timeRemaining <= 0) showLose = true
    }

    // Helper: hard-reset the current level (used by Restart and the lose popup).
    fun resetLevel() {
        gameState = GameEngine.initialize("level-$currentLevel", levelConfig.gridSize)
        moveCount = 0
        timeRemaining = timeLimit
        moreTimeUsed = false
        hintUnlockedInLevel = false
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
        // ----- Main content: Header at top, centered gameplay area, buttons below -----
        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 60.dp + 8.dp),
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

            // ----- Centered gameplay area -----
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // ----- 2. Pieces counter -----
                Text(
                    text = "${gameState.correctCount} / ${gameState.totalPieces} Pieces Placed",
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
                    onSwapSound = { audio.playSnap() },
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

                Spacer(Modifier.height(4.dp))
                Text(
                    text = "\uD83C\uDFAF  Drag pieces. Drop on correct spot to lock.",
                    color = TextMuted,
                    fontFamily = BodyFamily,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(4.dp))

            // ----- 4. Bottom button row (reordered: Home - Restart - Hint - More Time) -----
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
                    onClick = {
                        // Record screen change for restart
                        InterstitialAdManager.recordScreenChange()
                        showRestartConfirm = true
                    },
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 13,
                )
                PillButton(
                    label = "Hint\n(Ad)",
                    icon = "\uD83D\uDCA1", // 💡
                    gradient = listOf(YellowWarm, OrangeWarm),
                    textColor = TextDark,
                    onClick = {
                        if (hintUnlockedInLevel) {
                            // Hint already unlocked in this level, show immediately
                            audio.playHint()
                            showHint = true
                        } else {
                            // Show rewarded ad first, then unlock hint
                            if (activity != null) {
                                RewardedAdManager.showRewardedAdIfReady(
                                    activity!!,
                                    onRewardEarned = {
                                        hintUnlockedInLevel = true
                                        audio.playHint()
                                        showHint = true
                                    },
                                    onAdNotReady = {
                                        Log.d("HINT", "Ad not ready, try again")
                                    }
                                )
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 13,
                )
                PillButton(
                    label = "More Time\n(Ad)",
                    icon = "\u23F1", // ⏱
                    gradient = if (moreTimeUsed)
                        listOf(Color(0xFFEDEDED), Color(0xFFEDEDED))
                    else listOf(OrangeWarm, Coral),
                    textColor = TextDark,
                    onClick = {
                        if (!moreTimeUsed) {
                            // Show rewarded ad first, then add time
                            if (activity != null) {
                                RewardedAdManager.showRewardedAdIfReady(
                                    activity!!,
                                    onRewardEarned = {
                                        timeRemaining = (timeRemaining + 60).coerceAtMost(timeLimit + 300)
                                        moreTimeUsed = true
                                    },
                                    onAdNotReady = {
                                        Log.d("MORE_TIME", "Ad not ready, try again")
                                    }
                                )
                            }
                        }
                    },
                    enabled = !moreTimeUsed,
                    modifier = Modifier.weight(1f),
                    height = 64,
                    fontSize = 11,
                )
            }
        }

        // ----- 5. Banner ad — pinned to bottom -----
        BannerAd(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(60.dp)
        )

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
                onNext = {
                    // Record screen change and check if interstitial should be shown
                    InterstitialAdManager.recordScreenChange()
                    if (InterstitialAdManager.shouldShowAd() && activity != null) {
                        InterstitialAdManager.showInterstitialIfReady(activity!!) {
                            onLevelComplete()
                        }
                    } else {
                        onLevelComplete()
                    }
                },
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
        // Inner Box sizes to its largest child (the 320 dp image) so the close
        // button can be aligned to its corner. We use `offset` (not `padding`)
        // for the slight outward nudge — Compose's padding modifier rejects
        // negative values at runtime, which is what crashed earlier.
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
                    .offset(x = 12.dp, y = (-12).dp),
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

// ============================================================================
// Banner Ad
// ============================================================================

/**
 * AdMob BANNER ad — renders a standard 320×50 banner using [AndroidView].
 *
 * ⚠️  The ad unit ID below is Google's PUBLIC TEST ID.
 *     Replace it with your real banner ad unit ID from the AdMob dashboard
 *     before publishing:
 *       https://apps.admob.com → Apps → your app → Ad units → Banner → Ad unit ID
 *
 * The composable observes the Activity lifecycle so AdView.resume() / pause() /
 * destroy() are called at the correct moments when the app is backgrounded or
 * the composable leaves the tree.
 */
@Composable
private fun BannerAd(
    adUnitId: String = "ca-app-pub-3940256099942544/6300978111", // ← test ID; swap before release
    modifier: Modifier = Modifier,
) {
    android.util.Log.d("ADMOB", "BannerAd composable reached")

    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var adLoadFailed by remember { mutableStateOf(false) }

    // Build AdView once; remembered for the lifetime of this composable.
    val adView = remember {
        android.util.Log.d("ADMOB", "Creating AdView")
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            this.adUnitId = adUnitId

            // Set up AdListener to track load status
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    android.util.Log.d("ADMOB", "Banner loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.e(
                        "ADMOB",
                        "Banner failed to load: code=\${error.code}, message=\${error.message}"
                    )
                    adLoadFailed = true
                }
            }

            android.util.Log.d("ADMOB", "Loading banner ad with unit ID: $adUnitId")
            loadAd(AdRequest.Builder().build())
        }
    }

    // Mirror lifecycle events so AdMob can manage network requests correctly.
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    android.util.Log.d("ADMOB", "AdView resumed")
                    adView.resume()
                }

                Lifecycle.Event.ON_PAUSE -> {
                    android.util.Log.d("ADMOB", "AdView paused")
                    adView.pause()
                }

                Lifecycle.Event.ON_DESTROY -> {
                    android.util.Log.d("ADMOB", "AdView destroyed")
                    adView.destroy()
                }

                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    // Show fallback text if ad failed to load
    if (adLoadFailed) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Ad failed to load", color = Color.Red, fontSize = 12.sp)
        }
    } else {
        AndroidView(
            factory = { adView },
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp),
        )
    }
}
