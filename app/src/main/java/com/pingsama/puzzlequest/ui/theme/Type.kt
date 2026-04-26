package com.pingsama.puzzlequest.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The original web app uses Fredoka (display) and Poppins (body) from Google Fonts.
 * We use system sans-serif here so the app builds and runs offline with no extra setup.
 *
 * To get the exact original feel, swap [DisplayFamily] and [BodyFamily] for downloadable
 * Google Fonts:
 *
 *   1. Add to app/build.gradle.kts:
 *        implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
 *   2. Drop Google Play Services font certs into res/values/font_certs.xml
 *      (see https://developers.google.com/fonts/docs/android).
 *   3. Replace the two values below with:
 *        val DisplayFamily = FontFamily(
 *            Font(GoogleFont("Fredoka"), provider, FontWeight.Bold)
 *        )
 *        val BodyFamily = FontFamily(
 *            Font(GoogleFont("Poppins"), provider, FontWeight.Normal),
 *            Font(GoogleFont("Poppins"), provider, FontWeight.SemiBold)
 *        )
 */
val DisplayFamily: FontFamily = FontFamily.SansSerif
val BodyFamily: FontFamily = FontFamily.SansSerif

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,     fontSize = 48.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,    fontSize = 32.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,    fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,   fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,   fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal,         fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal,        fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = BodyFamily, fontWeight = FontWeight.Normal,         fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.Bold,       fontSize = 14.sp),
)
