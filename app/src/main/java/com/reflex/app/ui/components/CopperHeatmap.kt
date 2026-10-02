package com.reflex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.theme.CopperHeatmap0
import com.reflex.app.ui.theme.CopperHeatmap1
import com.reflex.app.ui.theme.CopperHeatmap2
import com.reflex.app.ui.theme.CopperHeatmap3
import com.reflex.app.ui.theme.CopperHeatmap4
import com.reflex.app.ui.theme.ReflexTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Reusable Copper Activity Heatmap Component.
 * Implements a 12-week GitHub-style contribution matrix rendered with soft-cornered copper blocks.
 */
@Composable
fun CopperHeatmap(
    activityData: Map<LocalDate, Int>, // Date -> metric (e.g. minutes or task count)
    modifier: Modifier = Modifier,
    weeksCount: Int = 12,
    onDayClick: ((LocalDate, Int) -> Unit)? = null
) {
    val today = LocalDate.now()
    val startDate = remember(today, weeksCount) {
        today.minusWeeks((weeksCount - 1).toLong()).with(DayOfWeek.MONDAY)
    }

    val weeks = remember(startDate, weeksCount) {
        (0 until weeksCount).map { weekIdx ->
            val weekStart = startDate.plusWeeks(weekIdx.toLong())
            (0..6).map { dayIdx -> weekStart.plusDays(dayIdx.toLong()) }
        }
    }

    val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    val tileShape = RoundedCornerShape(ReflexTokens.HeatmapTileRadius)
    val tileBorder = MaterialTheme.colorScheme.outlineVariant

    ReflexCard(modifier = modifier.fillMaxWidth()) {
        Column {
            // Month labels row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.HeatmapTileSpacing)
            ) {
                Spacer(Modifier.width(18.dp)) // Offset matching day label column
                Spacer(Modifier.width(ReflexTokens.HeatmapTileSpacing))
                weeks.forEach { week ->
                    val isFirstOfWeek = week.first().dayOfMonth <= 7 || week == weeks.first()
                    val monthLabel = if (isFirstOfWeek) {
                        week.first().month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    } else ""

                    Box(
                        modifier = Modifier.size(
                            width = ReflexTokens.HeatmapTileSize,
                            height = 18.dp
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (monthLabel.isNotEmpty()) {
                            Text(
                                text = monthLabel,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // 7 Rows of days (Monday through Sunday)
            (0..6).forEach { dayOfWeek ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.HeatmapTileSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Day of week indicator
                    Box(
                        modifier = Modifier.size(width = 24.dp, height = ReflexTokens.HeatmapTileSize),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLabels[dayOfWeek],
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.width(ReflexTokens.HeatmapTileSpacing))

                    // Grid tiles
                    weeks.forEach { week ->
                        val date = week[dayOfWeek]
                        val isFuture = date.isAfter(today)
                        val value = if (!isFuture) activityData[date] ?: 0 else 0

                        val cellColor = when {
                            isFuture -> Color.Transparent
                            value == 0 -> CopperHeatmap0
                            value < 20 -> CopperHeatmap1
                            value < 45 -> CopperHeatmap2
                            value < 75 -> CopperHeatmap3
                            else -> CopperHeatmap4
                        }

                        Box(
                            modifier = Modifier
                                .size(ReflexTokens.HeatmapTileSize)
                                .clip(tileShape)
                                .background(cellColor)
                                .then(
                                    if (value == 0 && !isFuture) {
                                        Modifier.border(
                                            ReflexTokens.BorderHairline,
                                            tileBorder,
                                            tileShape
                                        )
                                    } else Modifier
                                )
                                .then(
                                    if (onDayClick != null && !isFuture) {
                                        Modifier.clickable { onDayClick(date, value) }
                                    } else Modifier
                                )
                        )
                    }
                }
                Spacer(Modifier.height(ReflexTokens.HeatmapTileSpacing))
            }
        }
    }
}
