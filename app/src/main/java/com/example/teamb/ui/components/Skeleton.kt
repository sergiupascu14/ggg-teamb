package com.example.teamb.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.teamb.ui.theme.LocalAppColors

/**
 * An animated shimmer [Brush] that sweeps a light highlight across a muted base. Theme-aware (light
 * vs dark) so skeleton placeholders read correctly in both modes.
 */
@Composable
fun rememberShimmerBrush(sweepWidth: Float = 600f): Brush {
    val dark = LocalAppColors.current.isDark
    val base = if (dark) Color(0xFF1B2838) else Color(0xFFE6ECF3)
    val highlight = if (dark) Color(0xFF2C3E54) else Color(0xFFF5F8FC)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -sweepWidth,
        targetValue = sweepWidth * 2,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerX",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(x, 0f),
        end = Offset(x + sweepWidth, 0f),
    )
}

/** A single shimmering placeholder block — size it with [modifier]. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Box(modifier.clip(shape).background(rememberShimmerBrush()))
}
