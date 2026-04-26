package com.pingsama.puzzlequest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PuzzleColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = BoardMint,
    onPrimaryContainer = TextDark,

    secondary = YellowWarm,
    onSecondary = TextDark,
    secondaryContainer = OrangeWarm,
    onSecondaryContainer = TextDark,

    tertiary = Coral,
    onTertiary = Color.White,

    background = BgTop,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,

    error = Coral,
    onError = Color.White,
)

@Composable
fun PuzzleQuestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PuzzleColors,
        typography = AppTypography,
        content = content
    )
}
