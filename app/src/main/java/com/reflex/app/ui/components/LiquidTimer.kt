package com.reflex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.breakLiquidBottom
import com.reflex.app.ui.theme.breakLiquidTop
import com.reflex.app.ui.theme.glare
import com.reflex.app.ui.theme.liquidBottom
import com.reflex.app.ui.theme.liquidTop
import com.reflex.app.ui.theme.onLiquid
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Liquid sphere timer replicating the exact Reflex Design System v3.0 / reflex-focus.html specification.
 *
 * Glass: Circle of radius r = S/2 - 4dp, filled with surface, 1.5dp border stroke.
 * Liquid: Two sinus waves with horizontal gradient and tilt, smoothly draining.
 * Typography: Tabular Lora Bold digits, centered, drawn twice (ink over background, onLiquid over liquid).
 * Glare: Highlight arc at 194.4° with 54° sweep.
 */
@Composable
fun LiquidTimer(
    ratio: () -> Float,          // read in the draw phase only, so it never recomposes per frame
    timeText: String,            // "24:37", recomposes once per second
    label: String,
    status: String,
    isBreak: Boolean,
    isActive: Boolean,           // running -> faster waves
    modifier: Modifier = Modifier,
    animateWaves: Boolean = run {
        val context = LocalContext.current
        remember {
            try {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) > 0f
            } catch (_: Exception) {
                true
            }
        }
    }
) {
    val c = MaterialTheme.colorScheme
    val top = if (isBreak) c.breakLiquidTop else c.liquidTop
    val bottom = if (isBreak) c.breakLiquidBottom else c.liquidBottom
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val density = LocalDensity.current

    val phase = remember { mutableFloatStateOf(0f) }
    val clock = remember { mutableFloatStateOf(0f) }
    val active by rememberUpdatedState(isActive)
    val ratioProvider by rememberUpdatedState(ratio)

    // Physics-based continuous liquid drain tracking
    val smoothRatio = remember { mutableFloatStateOf(ratio().coerceIn(0f, 1f)) }
    var lastTarget by remember { mutableFloatStateOf(smoothRatio.floatValue) }
    var animStartVal by remember { mutableFloatStateOf(smoothRatio.floatValue) }
    var animStartTimeNanos by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var animDurationNanos by remember { androidx.compose.runtime.mutableLongStateOf(1_000_000_000L) }
    var isContinuousCaller by remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(animateWaves) {
        if (!animateWaves) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
                    clock.floatValue += dt
                    phase.floatValue += dt * if (active) 2.4f else 1.1f

                    val currentTarget = ratioProvider().coerceIn(0f, 1f)
                    if (currentTarget != lastTarget) {
                        val delta = kotlin.math.abs(currentTarget - lastTarget)
                        val elapsedSinceLast = now - animStartTimeNanos
                        // If caller updates frequently (>10Hz) with tiny deltas, it's already continuous
                        if (elapsedSinceLast < 100_000_000L && delta < 0.015f) {
                            isContinuousCaller = true
                            smoothRatio.floatValue = currentTarget
                        } else {
                            isContinuousCaller = false
                            animStartVal = smoothRatio.floatValue
                            animStartTimeNanos = now
                            // If a large jump occurs (like step skip/reset), transition in 350ms;
                            // if it's a 1-second countdown tick, glide steadily over 1000ms
                            animDurationNanos = if (delta > 0.08f) 350_000_000L else 1_000_000_000L
                        }
                        lastTarget = currentTarget
                    }

                    if (!isContinuousCaller) {
                        if (animDurationNanos > 0L) {
                            val progress = ((now - animStartTimeNanos).toFloat() / animDurationNanos.toFloat()).coerceIn(0f, 1f)
                            smoothRatio.floatValue = animStartVal + (lastTarget - animStartVal) * progress
                        } else {
                            smoothRatio.floatValue = lastTarget
                        }
                    }
                } else {
                    lastTarget = ratioProvider().coerceIn(0f, 1f)
                    smoothRatio.floatValue = lastTarget
                    animStartTimeNanos = now
                }
                last = now
            }
        }
    }

    BoxWithConstraints(
        modifier
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label, $timeText remaining, $status"
            }
    ) {
        val sizePx = with(density) { maxWidth.toPx() }
        val smallPx = maxOf(with(density) { 13.sp.toPx() }, sizePx * 0.05f)
        val baseStyle = remember(sizePx) {
            TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = with(density) { (sizePx * 0.26f).toSp() },
                fontFeatureSettings = "tnum"
            )
        }

        // Shrink the digits when the string has hours so it always fits inside the glass.
        val fit = remember(timeText.length, baseStyle, sizePx) {
            val cellB = "0123456789".maxOf { measurer.measure(it.toString(), baseStyle).size.width }
            val colonB = measurer.measure(":", baseStyle).size.width
            val total = timeText.fold(0) { a, ch -> a + if (ch == ':') colonB else cellB }
            if (total > 0) min(1f, (sizePx * 0.78f) / total) else 1f
        }
        val digitPx = sizePx * 0.26f * fit
        val digitStyle = remember(baseStyle, fit) {
            baseStyle.copy(fontSize = with(density) { digitPx.toSp() })
        }
        val smallStyle = remember(smallPx) {
            TextStyle(
                fontFamily = Lora,
                fontWeight = FontWeight.Medium,
                fontSize = with(density) { smallPx.toSp() }
            )
        }

        // Measure once and reuse every frame. No measure() calls in the draw phase.
        val glyphs = remember(digitStyle) {
            "0123456789:".associateWith { measurer.measure(it.toString(), digitStyle) }
        }
        val cell = remember(glyphs) { "0123456789".maxOf { glyphs.getValue(it).size.width } }
        val colonW = glyphs.getValue(':').size.width
        val labelLayout = remember(label, smallStyle) { measurer.measure(label, smallStyle) }
        val statusLayout = remember(status, smallStyle) { measurer.measure(status, smallStyle) }
        val circle = remember(sizePx) {
            Path().apply {
                addOval(Rect(center = Offset(sizePx / 2f, sizePx / 2f), radius = sizePx / 2f - 4f * density.density))
            }
        }
        val back = remember { Path() }
        val front = remember { Path() }

        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = cx - 4.dp.toPx()
            val p = phase.floatValue
            val rr = smoothRatio.floatValue.coerceIn(0f, 1f)
            val amp = if (animateWaves) size.width * 0.02f * min(1f, rr * 8f) else 0f
            val level = (cy + r + amp * 3f) - rr * (2f * r + amp * 6f)
            val tilt = sin(clock.floatValue / 1.1f) * amp * 0.55f
            val k = (2f * PI.toFloat()) / (size.width * 0.85f)
            val step = 4.dp.toPx()
            val floor = cy + r + 6.dp.toPx()

            fun wave(path: Path, ph: Float, a: Float, off: Float) {
                path.reset()
                var x = cx - r - 2f
                var first = true
                while (x <= cx + r + 2f) {
                    val y = level + off + a * sin(k * x + ph) + a * 0.3f * sin(k * x * 2.1f - ph * 1.3f) +
                        tilt * (x - cx) / r
                    if (first) {
                        path.moveTo(x, y)
                        first = false
                    } else {
                        path.lineTo(x, y)
                    }
                    x += step
                }
                path.lineTo(cx + r + 2f, floor)
                path.lineTo(cx - r - 2f, floor)
                path.close()
            }

            fun texts(color: Color) {
                var total = 0f
                timeText.forEach { total += if (it == ':') colonW else cell }
                var x = cx - total / 2f
                timeText.forEach { ch ->
                    val w = if (ch == ':') colonW else cell
                    val l = glyphs.getValue(ch)
                    drawText(l, color, Offset(x + (w - l.size.width) / 2f, cy - l.size.height / 2f))
                    x += w
                }
                drawText(
                    labelLayout,
                    color,
                    Offset(cx - labelLayout.size.width / 2f, cy - digitPx * 0.78f - labelLayout.size.height / 2f),
                    alpha = 0.8f
                )
                drawText(
                    statusLayout,
                    color,
                    Offset(cx - statusLayout.size.width / 2f, cy + digitPx * 0.78f - statusLayout.size.height / 2f),
                    alpha = 0.8f
                )
            }

            drawPath(circle, c.surface)
            drawPath(circle, c.outline, style = Stroke(1.5.dp.toPx()))

            val brush = Brush.verticalGradient(listOf(top, bottom), startY = level - amp, endY = cy + r)
            wave(back, p + 1.8f, amp * 1.2f, amp * 0.4f)
            wave(front, p, amp, 0f)
            clipPath(circle) {
                drawPath(back, brush, alpha = 0.5f)
                drawPath(front, brush)
            }

            texts(c.onSurface)                                         // pass 1: over everything
            clipPath(circle) { clipPath(front) { texts(c.onLiquid) } } // pass 2: only where liquid is

            val gr = r - 9.dp.toPx()
            drawArc(
                c.glare,
                194.4f,
                54f,
                false,
                Offset(cx - gr, cy - gr),
                Size(gr * 2f, gr * 2f),
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
