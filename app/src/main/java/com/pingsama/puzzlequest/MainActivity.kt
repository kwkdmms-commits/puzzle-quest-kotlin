package com.pingsama.puzzlequest

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.pingsama.puzzlequest.ads.AdManager
import com.pingsama.puzzlequest.game.LevelSystem
import com.pingsama.puzzlequest.ui.GameScreen
import com.pingsama.puzzlequest.ui.MainMenuScreen
import com.pingsama.puzzlequest.ui.theme.BgBottom
import com.pingsama.puzzlequest.ui.theme.BgTop
import com.pingsama.puzzlequest.ui.theme.PuzzleQuestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize AdMob (fail-safe)
        try {
            AdManager.initialize(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to initialize AdMob", e)
            // App continues even if AdMob fails
        }

        setContent {
            PuzzleQuestTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val app = applicationContext as PuzzleQuestApp
                    PuzzleQuestRoot(app)
                }
            }
        }
    }
}

@Composable
private fun PuzzleQuestRoot(app: PuzzleQuestApp) {
    var currentLevel by remember { mutableIntStateOf(app.leaderboard.currentLevel()) }
    var inMenu by remember { mutableStateOf(true) }
    val activity = LocalContext.current as? Activity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        if (inMenu) {
            MainMenuScreen(
                currentLevel = currentLevel,
                onPlay = { inMenu = false },
                onQuit = { activity?.finishAndRemoveTask() },
            )
        } else {
            GameScreen(
                currentLevel = currentLevel,
                audio = app.audio,
                leaderboard = app.leaderboard,
                onLevelComplete = {
                    val next = LevelSystem.nextLevel(currentLevel)
                    currentLevel = next
                    app.leaderboard.saveCurrentLevel(next)
                },
                onBackToMenu = { inMenu = true },
            )
        }
    }
}
