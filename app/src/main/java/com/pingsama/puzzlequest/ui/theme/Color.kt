package com.pingsama.puzzlequest.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Brand palette ported from the original web app's CSS variables and tailwind classes.
 * Source of truth: client/src/index.css (--primary, --accent, --secondary, --chart-4)
 * and the gradient bg-blue-50 → bg-pink-100 used on screen wrappers.
 */

val Coral = Color(0xFFFF6B6B)         // --accent / --destructive
val CoralLight = Color(0xFFFF8E8E)
val CoralDeep = Color(0xFFFF5252)
val Teal = Color(0xFF4ECDC4)          // --primary
val TealDark = Color(0xFF3DBFB7)
val YellowWarm = Color(0xFFFFE66D)    // --secondary
val OrangeWarm = Color(0xFFFFD93D)
val BoardMint = Color(0xFF95E1D3)     // --chart-4 — puzzle board base
val BgTop = Color(0xFFEFF6FB)         // approximation of tailwind's blue-50
val BgBottom = Color(0xFFFAF1F5)      // approximation of pink-50/100 mix
val WinBgTop = Color(0xFFEAF4FB)
val WinBgBottom = Color(0xFFF5EEFB)
val TextDark = Color(0xFF2C3E50)      // --foreground
val TextMuted = Color(0xFF8794A1)
val LockedGlow = Color(0xFF22D3EE)    // tailwind cyan-400 — locked-piece halo
