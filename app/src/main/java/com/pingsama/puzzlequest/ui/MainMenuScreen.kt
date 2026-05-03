package com.pingsama.puzzlequest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingsama.puzzlequest.ui.theme.BodyFamily
import com.pingsama.puzzlequest.ui.theme.Coral
import com.pingsama.puzzlequest.ui.theme.CoralLight
import com.pingsama.puzzlequest.ui.theme.DisplayFamily
import com.pingsama.puzzlequest.ui.theme.Teal
import com.pingsama.puzzlequest.ui.theme.TextMuted

/**
 * Main menu — mirrors `client/src/pages/MainMenu.tsx`.
 *  • Big two-tone title ("Puzzle Quest")
 *  • Tagline
 *  • "Play - Level N" button (coral gradient)
 *  • "Quit" button (light grey)
 *  • Footer copy at the bottom
 */
@Composable
fun MainMenuScreen(
    currentLevel: Int,
    onPlay: () -> Unit,
    onQuit: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ----- Title (gradient text emulated via a Brush on the TextStyle) -----
            Text(
                text = "Puzzle Quest",
                style = TextStyle(
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 52.sp,
                    brush = Brush.horizontalGradient(listOf(Coral, Teal)),
                    textAlign = TextAlign.Center,
                ),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Match the pieces. Complete the puzzle.",
                style = TextStyle(
                    fontFamily = BodyFamily,
                    fontSize = 14.sp,
                    color = TextMuted,
                ),
            )

            Spacer(Modifier.height(56.dp))

            PillButton(
                label = "Play  -  Level $currentLevel",
                icon = "\u25B6", // ▶
                gradient = listOf(Coral, CoralLight),
                onClick = onPlay,
                modifier = Modifier.fillMaxWidth(0.8f),
                height = 64,
                fontSize = 18,
            )

            Spacer(Modifier.height(16.dp))

            PillButton(
                label = "Quit",
                gradient = listOf(Color(0xFFE6E8EB), Color(0xFFD9DCE0)),
                textColor = Color(0xFF4A4F58),
                onClick = onQuit,
                modifier = Modifier.fillMaxWidth(0.8f),
                height = 64,
                fontSize = 18,
            )
        }

        Text(
            text = "50+ levels of puzzle fun",
            style = TextStyle(
                fontFamily = BodyFamily,
                fontSize = 12.sp,
                color = TextMuted,
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
        )
    }
}
