package com.reflex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun StraightRingTimer(
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    progressFraction: Float, // 0f to 1f
    timeDisplay: String,
    phaseLabel: String,
    emoji: String? = null
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size.minDimension
            val strokeWidth = 30.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(canvasSize - strokeWidth, canvasSize - strokeWidth)
            val topLeft = Offset(inset, inset)

            // 1. Background Circular Track (PureWhite track, StrokeCap.Butt)
            drawArc(
                color = surface,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )

            // 2. Active Progress Arc with STRAIGHT 90-degree radial edges (StrokeCap.Butt)
            val clampedFraction = progressFraction.coerceIn(0f, 1f)
            val sweepAngle = clampedFraction * 360f
            val startAngle = -90f // 12 o'clock top center

            if (sweepAngle > 0f) {
                drawArc(
                    color = primary,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            }
        }

        // Center Content: Mode Emoji / Icon, Digital Time Readout, Phase Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!emoji.isNullOrEmpty()) {
                Text(
                    text = emoji,
                    fontSize = 36.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            BigNumberDisplay(
                value = timeDisplay,
                fontSize = if (emoji.isNullOrEmpty()) 76.sp else 64.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = phaseLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = ink.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
