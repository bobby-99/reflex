package com.reflex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens

/**
 * Reusable Copper Line Chart Component.
 * Draws smooth cubic Bézier splines with vertical copper gradient fill and hairline guide lines.
 */
@Composable
fun CopperLineChart(
    dataPoints: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 160.dp,
    lineColor: Color = CopperPrimary,
    lineWidth: Dp = 2.dp,
    showGrid: Boolean = true
) {
    if (dataPoints.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant

    ReflexCard(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = ReflexTokens.SpaceSm)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val chartHeight = size.height
                val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val minVal = (dataPoints.minOrNull() ?: 0f).coerceAtLeast(0f)
                val range = (maxVal - minVal).let { if (it == 0f) 1f else it }

                // Draw 3 subtle horizontal guide lines
                if (showGrid) {
                    val guideCount = 3
                    for (i in 0..guideCount) {
                        val y = chartHeight * (i.toFloat() / guideCount)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }
                }

                if (dataPoints.size == 1) {
                    val y = chartHeight - ((dataPoints[0] - minVal) / range * chartHeight)
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(width / 2f, y))
                    return@Canvas
                }

                val stepX = width / (dataPoints.size - 1)
                val strokePath = Path()
                val fillPath = Path()

                val firstY = chartHeight - ((dataPoints[0] - minVal) / range * (chartHeight - 16.dp.toPx())) - 8.dp.toPx()
                strokePath.moveTo(0f, firstY)
                fillPath.moveTo(0f, chartHeight)
                fillPath.lineTo(0f, firstY)

                for (i in 1 until dataPoints.size) {
                    val prevX = (i - 1) * stepX
                    val prevY = chartHeight - ((dataPoints[i - 1] - minVal) / range * (chartHeight - 16.dp.toPx())) - 8.dp.toPx()
                    val curX = i * stepX
                    val curY = chartHeight - ((dataPoints[i] - minVal) / range * (chartHeight - 16.dp.toPx())) - 8.dp.toPx()

                    // Cubic Bézier control points
                    val controlX1 = prevX + (curX - prevX) / 2f
                    val controlY1 = prevY
                    val controlX2 = prevX + (curX - prevX) / 2f
                    val controlY2 = curY

                    strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, curX, curY)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, curX, curY)
                }

                fillPath.lineTo(width, chartHeight)
                fillPath.close()

                // Draw vertical gradient fill under curve
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.28f),
                        lineColor.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = chartHeight
                )
                drawPath(path = fillPath, brush = gradientBrush)

                // Draw active copper curve line
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(
                        width = lineWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}
