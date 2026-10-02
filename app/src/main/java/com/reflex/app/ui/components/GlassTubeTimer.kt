package com.reflex.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.theme.ReflexTokens
import kotlin.math.sin

@Composable
fun GlassTubeTimer(
    timeDisplay: String,
    phaseLabel: String,
    fillFraction: Float, // 0.0f (empty) to 1.0f (full)
    isUnbounded: Boolean = false,
    isPulsing: Boolean = false,
    size: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidWaveTransition")
    
    // Wave animation offset
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )

    // Pulse animation for long open flow sessions
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = this.size.minDimension
            val radius = canvasSize / 2f
            val center = Offset(canvasSize / 2f, canvasSize / 2f)

            // Stroke width = ~14% of radius
            val strokeWidth = radius * 0.28f
            val outerRadius = radius - 4.dp.toPx()
            val innerRadius = outerRadius - strokeWidth
            val midRadius = (outerRadius + innerRadius) / 2f

            // 1. Draw Translucent Glass Tube Wall Background
            drawCircle(
                color = surface.copy(alpha = 0.85f),
                radius = midRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // 2. Draw Liquid Fill inside the Tube Ring
            val clampedFraction = fillFraction.coerceIn(0.001f, 1.0f)
            val sweepAngle = 360f * clampedFraction
            val startAngle = -90f // Starts at top (12 o'clock)

            val liquidColor = if (isPulsing) primary.copy(alpha = pulseAlpha) else primary

            // Liquid ring arc fill
            drawArc(
                color = liquidColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - midRadius, center.y - midRadius),
                size = Size(midRadius * 2f, midRadius * 2f),
                style = Stroke(width = strokeWidth - 4.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Animated Liquid Surface Wave Edge
            if (clampedFraction in 0.05f..0.95f) {
                val endAngleRad = Math.toRadians((startAngle + sweepAngle).toDouble()).toFloat()
                val waveX = center.x + midRadius * kotlin.math.cos(endAngleRad)
                val waveY = center.y + midRadius * kotlin.math.sin(endAngleRad)
                
                val waveRippleRadius = (strokeWidth / 2f) + sin(waveOffset.toDouble()).toFloat() * 3.dp.toPx()

                drawCircle(
                    color = primary,
                    radius = waveRippleRadius.coerceAtLeast(4.dp.toPx()),
                    center = Offset(waveX, waveY)
                )
            }

            // 4. Draw Outer and Inner ink Border Outlines (Glass Outline)
            drawCircle(
                color = ink,
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = ink,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center Display: Time readout & Phase Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = ReflexTokens.SpaceLg)
        ) {
            BigNumberDisplay(
                value = timeDisplay,
                fontSize = 80.sp
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
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
