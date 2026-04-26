package com.pingsama.puzzlequest.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pingsama.puzzlequest.ui.theme.DisplayFamily

/**
 * Pill-shaped action button styled like the buttons in the reference screenshot:
 *  • horizontal gradient fill
 *  • white (or dark) bold sans-serif label
 *  • optional emoji glyph above the label (renders as a small icon row)
 *  • soft drop shadow + fully-rounded corners
 *
 * The original web app rendered these via Tailwind classes; we re-create the look
 * pixel-for-pixel so the Android UI matches the reference image you provided.
 */
@Composable
fun PillButton(
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    textColor: Color = Color.White,
    enabled: Boolean = true,
    height: Int = 56,
    fontSize: Int = 16,
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .height(height.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .clip(shape)
            .background(brush = Brush.horizontalGradient(gradient), shape = shape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Text(
                    text = icon,
                    fontSize = (fontSize + 4).sp,
                    color = textColor,
                )
            }
            Text(
                text = label,
                style = TextStyle(
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize.sp,
                    color = textColor,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}

/**
 * Round soft button used for in-game close buttons (the ✕ on the hint overlay).
 */
@Composable
fun RoundCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color(0xFFFF5252),
    contentColor: Color = Color.White,
    sizeDp: Int = 40,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(6.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\u2715", // ✕
            color = contentColor,
            style = TextStyle(
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (sizeDp / 2).sp,
            ),
        )
    }
}

/**
 * Light border, rounded card used for the win/lose/restart popups.
 */
@Composable
fun PopupCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(24.dp, RoundedCornerShape(28.dp), clip = false)
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(24.dp),
    ) { content() }
}

/** A simple horizontally-laid-out row helper used by popup actions. */
@Composable
fun ActionRow(content: @Composable () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier,
        content = { content() }
    )
}
