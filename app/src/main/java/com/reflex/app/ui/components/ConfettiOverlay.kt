package com.reflex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity

private class ConfettiParticleData(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val w: Float,
    val h: Float,
    var r: Float,
    val vr: Float,
    val color: Color
)

/**
 * 130-particle native physics confetti reproducing reflex-home.html confetti().
 * Fires from dual bottom emitters, with gravity, drag, angular spin, and smooth 3.4s fade-out.
 * Zero touch interception (pure visual overlay).
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    val density = LocalDensity.current.density
    var animProgress by remember { mutableFloatStateOf(0f) }
    var particles by remember { mutableStateOf<List<ConfettiParticleData>?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        LaunchedEffect(width, height) {
            if (width <= 0 || height <= 0) return@LaunchedEffect
            val leftX = width * 0.15f
            val rightX = width * 0.85f
            val startY = height * 0.75f
            val colors = listOf(
                Color(0xFFD9A184), // Copper
                Color(0xFFE3C27A), // Gold
                Color(0xFF86C9A4), // Sage / Green
                Color(0xFF948B83)  // Tertiary
            )
            val pList = List(130) { k ->
                val isLeft = k % 2 == 1
                ConfettiParticleData(
                    x = if (isLeft) leftX else rightX,
                    y = startY,
                    vx = (if (isLeft) 1f else -1f) * (2f + (Math.random().toFloat() * 7f)) * density,
                    vy = -(9f + (Math.random().toFloat() * 12f)) * density,
                    w = (6f + (Math.random().toFloat() * 6f)) * density,
                    h = (3f + (Math.random().toFloat() * 4f)) * density,
                    r = (Math.random().toFloat() * 6f),
                    vr = ((Math.random().toFloat() - 0.5f) * 0.4f),
                    color = colors[k % colors.size]
                )
            }
            particles = pList

            val t0 = withFrameNanos { it }
            var elapsedMs = 0f
            while (elapsedMs < 3400f) {
                withFrameNanos { nowNanos ->
                    elapsedMs = (nowNanos - t0) / 1_000_000f
                    pList.forEach { p ->
                        p.vy += 0.38f * density
                        p.vx *= 0.99f
                        p.x += p.vx
                        p.y += p.vy
                        p.r += p.vr
                    }
                    animProgress = elapsedMs
                }
            }
            particles = null
        }

        val currentParticles = particles
        if (currentParticles != null && animProgress < 3400f) {
            val globalAlpha = ((3400f - animProgress) / 900f).coerceIn(0f, 1f)
            Canvas(modifier = Modifier.fillMaxSize()) {
                @Suppress("UNUSED_VARIABLE")
                val progress = animProgress
                currentParticles.forEach { p ->
                    withTransform({
                        translate(p.x, p.y)
                        rotate(Math.toDegrees(p.r.toDouble()).toFloat())
                    }) {
                        drawRect(
                            color = p.color.copy(alpha = globalAlpha),
                            topLeft = Offset(-p.w / 2f, -p.h / 2f),
                            size = Size(p.w, p.h)
                        )
                    }
                }
            }
        }
    }
}
