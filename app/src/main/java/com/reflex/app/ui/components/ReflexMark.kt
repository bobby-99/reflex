package com.reflex.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pixel-faithful Reflex Brand Mark composable.
 *
 * Design space: 200x200 units in primary copper.
 * - Ring: open arc, center (100,100), radius 74, stroke 14, round caps.
 * - Dot: circle at (154.1, 49.5), radius 9 (sits in the gap, extends 2 units beyond ring's outer edge).
 * - Wave fill: clipped to circle of radius 60 at (100,100).
 *   - Front wave: surface near y=104, amplitude ~6, wavelength 100.
 *   - Back wave: surface near y=96 at 35% alpha.
 * - Animation: optional horizontal wave scrolling (1 wavelength / 3s) and outer glow rings + breathing halo.
 */
@Composable
fun ReflexMark(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    animated: Boolean = false,
    showGlowRings: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.primary,
    respectReducedMotion: Boolean = true
) {
    // Infinite transition for wave scrolling & pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "ReflexMarkTransition")

    val waveScroll by if (animated) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "WaveScroll"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    // Outer glow rings (3.6s cycle, offset by 1.2s each)
    val ringProgress by if (showGlowRings) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RingProgress"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    }

    // Halo breathing between 100% and 50% of 32% alpha over 3.6s
    val haloAlphaMultiplier by if (showGlowRings) {
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "HaloAlpha"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(1f) }
    }

    // Cached paths in 200x200 space
    val ringPath = remember {
        PathParser().parsePathString("M167.07 68.73A74 74 0 1 1 137 35.92").toPath()
    }
    val clipCircle = remember {
        Path().apply {
            addOval(Rect(center = Offset(100f, 100f), radius = 60f))
        }
    }

    val backWaveBaseSvg = remember {
        PathParser().parsePathString(
            "M-200 0Q-175 12 -150 0T-100 0T-50 0T0 0T50 0T100 0T150 0T200 0T250 0T300 0V120H-200Z"
        ).toPath()
    }

    val frontWaveBaseSvg = remember {
        PathParser().parsePathString(
            "M-200 0Q-175 -12 -150 0T-100 0T-50 0T0 0T50 0T100 0T150 0T200 0T250 0T300 0V120H-200Z"
        ).toPath()
    }

    Box(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { /* decorative mark hidden from accessibility */ },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val scale = this.size.minDimension / 200f

            withTransform({
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                // 1. Glow Halo & Pulsing Rings (Welcome page)
                if (showGlowRings) {
                    // Radial halo: hugging outside of the ring (r ≈ 81 to 130), transparent inside
                    val haloBrush = Brush.radialGradient(
                        0.62f to Color.Transparent,
                        0.67f to tint.copy(alpha = 0.32f * haloAlphaMultiplier),
                        1.0f to Color.Transparent,
                        center = Offset(100f, 100f),
                        radius = 130f
                    )
                    drawCircle(
                        brush = haloBrush,
                        radius = 130f,
                        center = Offset(100f, 100f)
                    )

                    // 3 Expanding Rings (radius 81 in 200 space, 2dp stroke, scale 1.0 -> 1.6, alpha 0.6 -> 0)
                    for (ringIdx in 0..2) {
                        val phase = (ringProgress - ringIdx * (1.2f / 3.6f) + 1f) % 1f
                        val currentScale = 1.0f + 0.6f * phase
                        val currentAlpha = 0.6f * (1f - phase)
                        if (currentAlpha > 0f) {
                            drawCircle(
                                color = tint.copy(alpha = currentAlpha),
                                radius = 81f * currentScale,
                                center = Offset(100f, 100f),
                                style = Stroke(width = 2f / scale, cap = StrokeCap.Round)
                            )
                        }
                    }
                }

                // 2. Wave Fill clipped to circle of radius 60 at (100, 100)
                clipPath(clipCircle) {
                    // Back wave at y=96 with 35% alpha
                    withTransform({
                        translate(left = 0f, top = 96f)
                    }) {
                        drawPath(
                            path = backWaveBaseSvg,
                            color = tint.copy(alpha = 0.35f)
                        )
                    }

                    // Front wave at y=104 with optional horizontal scrolling
                    val waveX = if (animated) waveScroll else 0f
                    withTransform({
                        translate(left = waveX, top = 104f)
                    }) {
                        drawPath(
                            path = frontWaveBaseSvg,
                            color = tint
                        )
                    }
                }

                // 3. Ring: radius 74, stroke 14, round caps
                drawPath(
                    path = ringPath,
                    color = tint,
                    style = Stroke(
                        width = 14f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // 4. Dot: circle at (154.1, 49.5), radius 9
                drawCircle(
                    color = tint,
                    radius = 9f,
                    center = Offset(154.1f, 49.5f)
                )
            }
        }
    }
}
