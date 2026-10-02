package com.reflex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unified, theme-adaptive timer ring component shared across Focus, Routine, and all timers.
 * Implements a sleek circular track with a smooth fading color gradient along the active progress arc.
 */
@Composable
fun RoundedRingTimer(
    modifier: Modifier = Modifier,
    size: Dp = 280.dp,
    strokeWidth: Dp = 24.dp,
    progressFraction: Float, // 0f to 1f
    timeDisplay: String,
    durationText: String? = null,
    emoji: String? = null,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    isPaused: Boolean = false,
    showPlayPauseIcon: Boolean = true,
    onPlayPauseClick: (() -> Unit)? = null
) {
    val clampedFraction = progressFraction.coerceIn(0f, 1f)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onPlayPauseClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onPlayPauseClick
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size.minDimension
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(canvasSize - strokePx, canvasSize - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // 1. Background Circular Track (Straight-cut circular track)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Butt)
            )

            // 2. Active Progress Arc with Minimal Theme-Accurate Gradient
            val sweepAngle = clampedFraction * 360f

            if (sweepAngle > 0.5f) {
                // Minimal clean gradient from 60% opacity to 100% theme color
                val tailColor = progressColor.copy(alpha = 0.55f)
                val headColor = progressColor

                val gradientStops = if (clampedFraction >= 0.999f) {
                    arrayOf(
                        0.0f to tailColor,
                        0.5f to headColor.copy(alpha = 0.8f),
                        1.0f to headColor
                    )
                } else {
                    val headStop = clampedFraction.coerceIn(0.01f, 1.0f)
                    arrayOf(
                        0.0f to tailColor,
                        headStop * 0.4f to headColor.copy(alpha = 0.75f),
                        headStop to headColor,
                        1.0f to tailColor
                    )
                }

                val sweepBrush = Brush.sweepGradient(
                    *gradientStops,
                    center = center
                )

                // Rotate canvas by -90 deg so sweep starts at 12 o'clock top center
                rotate(degrees = -90f) {
                    drawArc(
                        brush = sweepBrush,
                        startAngle = 0f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                    )
                }
            }
        }

        // 3. Center Info: Emoji, Large Digital Time Readout, Subtitle Label & Play/Pause Icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!emoji.isNullOrEmpty()) {
                OpenMojiIcon(emoji = emoji, size = 36.dp)
                Spacer(modifier = Modifier.height(2.dp))
            }

            BigNumberDisplay(
                value = timeDisplay,
                fontSize = if (emoji.isNullOrEmpty()) 58.sp else 50.sp
            )

            if (!durationText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.65f),
                    letterSpacing = 0.sp
                )
            }

            if (showPlayPauseIcon) {
                Spacer(modifier = Modifier.height(6.dp))
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPaused) "Paused" else "Running",
                    tint = contentColor.copy(alpha = 0.45f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
