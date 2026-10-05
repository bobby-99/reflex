package com.reflex.app.ui.components

import android.provider.Settings
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

private val DefaultConfettiColors = listOf(
    Color(0xFFD9A184), // Copper
    Color(0xFFE3C27A), // Gold
    Color(0xFF86C9A4), // Sage / Green
    Color(0xFF948B83)  // Tertiary
)

private const val GRAVITY = 0.38f          // per 60fps frame, scaled by density (unchanged)
private const val DRAG = 0.99f             // horizontal drag per 60fps frame (unchanged)
private const val FRAME_NANOS_60 = 16_666_667f
private const val RAD_TO_DEG = 57.29578f

private class ConfettiPiece(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val w: Float,
    val h: Float,
    var rot: Float,        // radians
    val vrot: Float,       // radians per 60fps frame
    var flip: Float,       // tumble phase, simulates the piece turning over in the air
    val vflip: Float,
    val color: Color,
    val round: Boolean
)

/**
 * Native physics confetti, fired once when it enters composition.
 *
 * Same behavior as before: 130 pieces from two bottom emitters, gravity, drag, spin,
 * 3.4s total with a 900ms fade-out, and no touch interception (pure visual overlay).
 * Drop-in compatible: the original `ConfettiOverlay(modifier)` call still works, and the new
 * parameters are optional.
 *
 * What is better than the previous version:
 *  - Physics are time-based, so the fall speed is identical on 60, 90 and 120 Hz screens.
 *  - Rotation pivots around each piece. Before, the pivot was the canvas center, so pieces
 *    swung around a far-away point instead of spinning in place.
 *  - Per-frame state updates only invalidate drawing, not the whole composition.
 *  - Pieces tumble (the thin edge catches the light) and a few are round, so it looks less flat.
 *  - Off-screen pieces are skipped, emitters have a slight jitter, and the effect is skipped
 *    when the system animation scale is 0 (set [respectReducedMotion] = false to override).
 */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    colors: List<Color> = DefaultConfettiColors,
    particleCount: Int = 130,
    durationMs: Int = 3400,
    fadeMs: Int = 900,
    respectReducedMotion: Boolean = true
) {
    val density = LocalDensity.current.density
    val context = LocalContext.current
    val enabled = remember(context, respectReducedMotion) {
        !respectReducedMotion || Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
    }

    val pieces = remember { ArrayList<ConfettiPiece>(particleCount) }
    var active by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableFloatStateOf(0f) } // read only in the draw phase

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val ready = width > 0f && height > 0f

        LaunchedEffect(ready, enabled) {
            if (!ready || !enabled || colors.isEmpty()) return@LaunchedEffect

            val leftX = width * 0.15f
            val rightX = width * 0.85f
            val startY = height * 0.75f
            val jitter = 16f * density

            pieces.clear()
            repeat(particleCount) { k ->
                val fromLeft = k % 2 == 1
                pieces += ConfettiPiece(
                    x = (if (fromLeft) leftX else rightX) + (Random.nextFloat() - 0.5f) * jitter,
                    y = startY + (Random.nextFloat() - 0.5f) * jitter,
                    vx = (if (fromLeft) 1f else -1f) * (2f + Random.nextFloat() * 7f) * density,
                    vy = -(9f + Random.nextFloat() * 12f) * density,
                    w = (6f + Random.nextFloat() * 6f) * density,
                    h = (3f + Random.nextFloat() * 4f) * density,
                    rot = Random.nextFloat() * 6f,
                    vrot = (Random.nextFloat() - 0.5f) * 0.4f,
                    flip = Random.nextFloat() * 6.2831855f,
                    vflip = 0.06f + Random.nextFloat() * 0.18f,
                    color = colors[k % colors.size],
                    round = k % 5 == 4
                )
            }

            active = true
            val t0 = withFrameNanos { it }
            var last = t0
            while (true) {
                val now = withFrameNanos { it }
                // 1f == one frame at 60 fps, clamped so a hitch can't teleport pieces
                val k = ((now - last) / FRAME_NANOS_60).coerceIn(0f, 3f)
                last = now
                val ms = (now - t0) / 1_000_000f

                val drag = DRAG.pow(k)
                for (i in pieces.indices) {
                    val p = pieces[i]
                    p.vy += GRAVITY * density * k
                    p.vx *= drag
                    p.x += p.vx * k + sin(p.flip * 0.5f) * 0.35f * density * k // gentle sway
                    p.y += p.vy * k
                    p.rot += p.vrot * k
                    p.flip += p.vflip * k
                }
                elapsedMs = ms
                if (ms >= durationMs) break
            }
            active = false
            pieces.clear()
        }

        if (active) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val globalAlpha = ((durationMs - elapsedMs) / fadeMs).coerceIn(0f, 1f)
                val cullY = size.height + 40f * density
                for (i in pieces.indices) {
                    val p = pieces[i]
                    if (p.y > cullY) continue
                    val face = abs(cos(p.flip))                 // 1 = flat to the camera, 0 = edge-on
                    val alpha = globalAlpha * (0.7f + 0.3f * face)
                    val color = p.color.copy(alpha = alpha)
                    if (p.round) {
                        drawCircle(color = color, radius = p.w * 0.3f, center = Offset(p.x, p.y))
                    } else {
                        val drawH = p.h * (0.25f + 0.75f * face)
                        rotate(degrees = p.rot * RAD_TO_DEG, pivot = Offset(p.x, p.y)) {
                            drawRect(
                                color = color,
                                topLeft = Offset(p.x - p.w / 2f, p.y - drawH / 2f),
                                size = Size(p.w, drawH)
                            )
                        }
                    }
                }
            }
        }
    }
}
