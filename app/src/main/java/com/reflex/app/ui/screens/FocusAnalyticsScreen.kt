package com.reflex.app.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reflex.app.ReflexApplication
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusTag
import com.reflex.app.ui.components.AppPickerSheet
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.DarkTertiaryText
import com.reflex.app.ui.theme.LightTertiaryText
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.onCopper
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.ui.theme.sage
import com.reflex.app.viewmodel.AnalyticsBarItem
import com.reflex.app.viewmodel.AnalyticsRange
import com.reflex.app.viewmodel.FocusAnalyticsUiState
import com.reflex.app.viewmodel.FocusAnalyticsViewModel
import com.reflex.app.viewmodel.HeatmapDay
import com.reflex.app.viewmodel.SessionTypeItem
import com.reflex.app.viewmodel.TagStatItem
import com.reflex.app.viewmodel.TimeOfDayItem
import com.reflex.app.viewmodel.TopTaskItem

@Composable
fun FocusAnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FocusAnalyticsViewModel = viewModel(
        factory = FocusAnalyticsViewModel.Factory(
            (LocalContext.current.applicationContext as ReflexApplication),
            (LocalContext.current.applicationContext as ReflexApplication).repository
        )
    )
) {
    SetStatusBarAppearance()
    val context = LocalContext.current
    val app = context.applicationContext as ReflexApplication
    val uiState by viewModel.uiState.collectAsState()

    var showAppPickerSheet by remember { mutableStateOf(false) }
    val focusSettings by app.repository.getFocusSettings().collectAsState(initial = null)

    // Scroll state survives navigation and recreation
    val scrollState = rememberSaveable(saver = androidx.compose.foundation.ScrollState.Saver) { androidx.compose.foundation.ScrollState(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .reflexStatusBarPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 140.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar: Back button (44dp visual, 48dp target) + Title
            AnalyticsTopBar(
                title = "Focus analytics",
                onBackClick = onNavigateBack
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Range selector: segmented Week / Month / Year
            RangeSegmentedControl(
                selectedRange = uiState.range,
                onRangeSelected = { viewModel.setRange(it) }
            )

            // 2. Tag filter chips (hidden if no tags exist in DB)
            if (uiState.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                TagFilterRow(
                    tags = uiState.tags,
                    selectedTagId = uiState.selectedTagId,
                    onTagSelected = { viewModel.selectTag(it) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Three stat tiles: Focus time, Sessions, Average per session
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    value = uiState.focusTimeFormatted,
                    title = "Focus time",
                    subtitle = uiState.rangeSubtitle,
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = uiState.sessionsCount.toString(),
                    title = "Sessions",
                    subtitle = "completed",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = uiState.averageFormatted,
                    title = "Average",
                    subtitle = "per session",
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Daily goal card
            AnalyticsSectionHeader(
                title = "Daily goal",
                subtitle = "Today · adjust to what feels right"
            )
            DailyGoalCard(
                todayFocusFormatted = uiState.todayFocusMinutes.let { FocusAnalyticsViewModel.formatMinutes(it) },
                goalFormatted = FocusAnalyticsViewModel.formatMinutes(uiState.goalMinutes),
                percent = uiState.todayGoalPercent,
                onDecreaseGoal = { viewModel.stepGoal(-1) },
                onIncreaseGoal = { viewModel.stepGoal(1) }
            )

            // 5. Focus time bar chart
            AnalyticsSectionHeader(
                title = "Focus time",
                subtitle = if (uiState.range == AnalyticsRange.YEAR) "by month" else "by day · tap a bar"
            )
            BarChartCard(
                bars = uiState.bars,
                selectedBarIndex = uiState.selectedBarIndex,
                selectedBarValue = uiState.selectedBarValue,
                selectedBarLabel = uiState.selectedBarLabel,
                maxBarMinutes = uiState.maxBarMinutes,
                goalMinutes = uiState.goalMinutes,
                showGoalLine = uiState.range != AnalyticsRange.YEAR,
                isEmpty = uiState.isBarChartEmpty,
                onSelectBar = { viewModel.selectBar(it) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Three stat tiles: Streak, Best day, Finished %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatTile(
                    value = "${uiState.streakDays}d",
                    title = "Streak",
                    subtitle = "days in a row",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = uiState.bestDayFormatted,
                    title = "Best day",
                    subtitle = "longest total",
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    value = uiState.finishedPercentStr,
                    title = "Finished",
                    subtitle = "not ended early",
                    modifier = Modifier.weight(1f)
                )
            }

            // 7. Consistency heatmap (always last 365 days)
            AnalyticsSectionHeader(
                title = "Consistency heatmap",
                subtitle = "all sessions · last 365 days"
            )
            HeatmapCard(
                days = uiState.heatmapDays,
                startOffset = uiState.heatmapStartDayOfWeekOffset,
                selectedDay = uiState.selectedHeatmapDay,
                onSelectDay = { viewModel.selectHeatmapDay(it) }
            )

            // 8. Time of day card
            AnalyticsSectionHeader(
                title = "Time of day",
                subtitle = "when you focus best"
            )
            TimeOfDayCard(
                items = uiState.timeOfDayItems,
                bestWindowTip = uiState.bestWindowTip
            )

            // 9. Session types card
            AnalyticsSectionHeader(
                title = "Session types",
                subtitle = null
            )
            SessionTypesCard(
                sessionTypes = uiState.sessionTypes
            )

            // 10. Top tasks card
            AnalyticsSectionHeader(
                title = "Top tasks",
                subtitle = "where your focus went"
            )
            TopTasksCard(
                topTasks = uiState.topTasks
            )

            // 11. By tag card (hidden if no tags exist in database)
            if (uiState.showTagCard) {
                AnalyticsSectionHeader(
                    title = "By tag",
                    subtitle = null
                )
                ByTagCard(
                    tagStats = uiState.tagStats
                )
            }

            // 12. Distractions blocked card (visible only if app blocking ever enabled or events exist)
            if (uiState.distractionsBlocked.isVisible) {
                AnalyticsSectionHeader(
                    title = "Distractions blocked",
                    subtitle = "from app blocking · this week"
                )
                DistractionsBlockedCard(
                    attempts = uiState.distractionsBlocked.attempts,
                    keptFormatted = uiState.distractionsBlocked.keptFormatted,
                    mostBlockedApp = uiState.distractionsBlocked.mostBlockedApp,
                    mostBlockedCount = uiState.distractionsBlocked.mostBlockedCount,
                    onManageBlockingClick = { showAppPickerSheet = true }
                )
            }
        }

        // App picker bottom sheet for app blocking management
        if (showAppPickerSheet) {
            val settingsToUse = focusSettings ?: com.reflex.app.data.FocusSettings()
            AppPickerSheet(
                initialMode = settingsToUse.blockingMode,
                initialPackages = settingsToUse.selectedPackagesSet,
                onDismiss = { showAppPickerSheet = false },
                onSave = { newMode, newPackages ->
                    viewModel.updateFocusSettings(
                        settingsToUse.copy(
                            blockingMode = newMode,
                            selectedPackages = newPackages.joinToString(",")
                        )
                    )
                    showAppPickerSheet = false
                }
            )
        }
    }
}

// =========================================================================
// SUBCOMPONENTS MATCHING focus-analytics.html
// =========================================================================

@Composable
private fun AnalyticsTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        Text(
            text = title,
            fontFamily = Lora,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun RangeSegmentedControl(
    selectedRange: AnalyticsRange,
    onRangeSelected: (AnalyticsRange) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AnalyticsRange.entries.forEach { range ->
                val isSelected = range == selectedRange
                val label = when (range) {
                    AnalyticsRange.WEEK -> "Week"
                    AnalyticsRange.MONTH -> "Month"
                    AnalyticsRange.YEAR -> "Year"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (isSelected) CopperPrimary else Color.Transparent)
                        .clickable { onRangeSelected(range) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = Lora,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 15.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.onCopper else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TagFilterRow(
    tags: List<FocusTag>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        TagFilterChip(
            label = "All",
            isSelected = selectedTagId == null,
            onClick = { onTagSelected(null) }
        )

        // Each custom tag
        tags.forEach { tag ->
            TagFilterChip(
                label = tag.name,
                isSelected = selectedTagId == tag.id,
                onClick = { onTagSelected(tag.id) }
            )
        }
    }
}

@Composable
private fun TagFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) CopperContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontFamily = Lora,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                fontFamily = Lora,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontFamily = Lora,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AnalyticsSectionHeader(
    title: String,
    subtitle: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 10.dp)
    ) {
        Text(
            text = title,
            fontFamily = Lora,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

// 4. Daily Goal Card: 112dp ring, % center, Focused today, Stepper
@Composable
private fun DailyGoalCard(
    todayFocusFormatted: String,
    goalFormatted: String,
    percent: Int,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit
) {
    AnalyticsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 112dp Ring
            val inputColor = MaterialTheme.colorScheme.secondaryContainer
            val copper = CopperPrimary
            Box(
                modifier = Modifier.size(112.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(112.dp)) {
                    val strokeWidth = 11.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Track
                    drawCircle(
                        color = inputColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )

                    // Progress Arc
                    val sweep = (percent / 100f) * 360f
                    drawArc(
                        color = copper,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = "$percent%",
                    style = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        fontFeatureSettings = "tnum",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Right Info & Stepper
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Focused today",
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$todayFocusFormatted of $goalFormatted",
                    style = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Stepper: [-] [120m] [+]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable(onClick = onDecreaseGoal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "−",
                            fontFamily = Lora,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier.width(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = goalFormatted,
                            style = TextStyle(
                                fontFamily = Lora,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                fontFeatureSettings = "tnum",
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable(onClick = onIncreaseGoal),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontFamily = Lora,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// 5. Bar Chart Card: Tap or drag scrub, dashed goal line, labels
@Composable
private fun BarChartCard(
    bars: List<AnalyticsBarItem>,
    selectedBarIndex: Int,
    selectedBarValue: String,
    selectedBarLabel: String,
    maxBarMinutes: Int,
    goalMinutes: Int,
    showGoalLine: Boolean,
    isEmpty: Boolean,
    onSelectBar: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    AnalyticsCard {
        // Header: Selected bar value + date label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = selectedBarValue,
                style = TextStyle(
                    fontFamily = Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = selectedBarLabel,
                fontFamily = Lora,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chart area: 150dp height
        val isDark = isSystemInDarkTheme()
        val tertiaryBorder = MaterialTheme.colorScheme.outline
        val copper = CopperPrimary
        val inputTrack = MaterialTheme.colorScheme.secondaryContainer

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .pointerInput(bars) {
                    fun updateSelection(xOffset: Float) {
                        if (bars.isEmpty()) return
                        val barWidth = size.width / bars.size
                        val idx = (xOffset / barWidth).toInt().coerceIn(0, bars.size - 1)
                        if (idx != selectedBarIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectBar(idx)
                        }
                    }

                    detectTapGestures { offset ->
                        updateSelection(offset.x)
                    }
                }
                .pointerInput(bars) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val barWidth = size.width / bars.size
                        val idx = (change.position.x / barWidth).toInt().coerceIn(0, bars.size - 1)
                        if (idx != selectedBarIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectBar(idx)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = bars.size
                if (count == 0) return@Canvas
                val totalWidth = size.width
                val totalHeight = size.height
                val barGap = 5.dp.toPx()
                val barWidth = ((totalWidth - (count - 1) * barGap) / count).coerceAtLeast(4f)

                // 1. Draw dashed goal line (Week/Month only)
                if (showGoalLine && maxBarMinutes > 0) {
                    val goalFraction = (goalMinutes.toFloat() / maxBarMinutes.toFloat()).coerceIn(0f, 1f)
                    val goalY = totalHeight * (1f - goalFraction)
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    drawLine(
                        color = tertiaryBorder.copy(alpha = 0.75f),
                        start = Offset(0f, goalY),
                        end = Offset(totalWidth, goalY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = pathEffect
                    )
                }

                // 2. Draw bars
                bars.forEachIndexed { i, bar ->
                    val x = i * (barWidth + barGap)
                    val fraction = if (maxBarMinutes > 0) {
                        (bar.valueMinutes.toFloat() / maxBarMinutes.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val barHeight = (fraction * totalHeight).coerceAtLeast(6.dp.toPx())
                    val y = totalHeight - barHeight

                    val isSelected = i == selectedBarIndex
                    val isMet = bar.isMetGoal && showGoalLine

                    val fillColor = when {
                        isSelected -> copper
                        isMet -> copper.copy(alpha = 0.72f)
                        else -> copper.copy(alpha = 0.38f)
                    }

                    // Bar fill
                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Selected bar outline
                    if (isSelected) {
                        val outlineOffset = 2.dp.toPx()
                        val outlineWidth = 2.dp.toPx()
                        drawRoundRect(
                            color = copper.copy(alpha = 0.30f),
                            topLeft = Offset(x - outlineOffset, y - outlineOffset),
                            size = Size(barWidth + outlineOffset * 2, barHeight + outlineOffset * 2),
                            cornerRadius = CornerRadius((barWidth + outlineOffset * 2) / 2f, (barWidth + outlineOffset * 2) / 2f),
                            style = Stroke(width = outlineWidth)
                        )
                    }
                }
            }
        }

        // Labels under bars
        Spacer(modifier = Modifier.height(8.dp))
        val textMeasurer = rememberTextMeasurer()
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val count = bars.size
            if (count == 0) return@Canvas
            val totalWidth = size.width
            val barGap = 5.dp.toPx()
            val barWidth = ((totalWidth - (count - 1) * barGap) / count).coerceAtLeast(4f)

            bars.forEachIndexed { i, bar ->
                if (bar.label.isNotEmpty()) {
                    val measuredText = textMeasurer.measure(
                        text = bar.label,
                        style = TextStyle(
                            fontFamily = Lora,
                            fontSize = if (bar.label.length > 2) 11.sp else 12.sp,
                            color = labelColor
                        )
                    )
                    val barCenterX = i * (barWidth + barGap) + barWidth / 2f
                    val x = (barCenterX - measuredText.size.width / 2f).coerceIn(0f, totalWidth - measuredText.size.width)
                    drawText(
                        textLayoutResult = measuredText,
                        topLeft = Offset(x, 0f)
                    )
                }
            }
        }

        if (isEmpty) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No focus time recorded for this period",
                fontFamily = Lora,
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// 7. Consistency Heatmap Card: Single Canvas, 365 days, 7 rows x 53 cols, tap readout
@Composable
private fun HeatmapCard(
    days: List<HeatmapDay>,
    startOffset: Int,
    selectedDay: HeatmapDay?,
    onSelectDay: (Int) -> Unit
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    // Scroll to right side (newest week) automatically on first render
    LaunchedEffect(days.size) {
        if (days.isNotEmpty()) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    AnalyticsCard {
        // Readout if cell is selected
        if (selectedDay != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDay.formattedDate,
                    fontFamily = Lora,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = selectedDay.formattedDuration,
                    style = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CopperPrimary
                    )
                )
            }
        }

        // Horizontal scrolling single Canvas
        val cellSize = 12.dp
        val cellGap = 3.dp
        val totalCols = ((days.size + startOffset + 6) / 7).coerceAtLeast(53)
        val canvasWidthDp = (totalCols * 15 - 3).dp
        val canvasHeightDp = (7 * 15 - 3).dp // 102dp

        val inputColor = MaterialTheme.colorScheme.secondaryContainer
        val copper = CopperPrimary
        val selectedIndex = selectedDay?.index

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = canvasWidthDp, height = canvasHeightDp)
                    .pointerInput(days, startOffset) {
                        detectTapGestures { offset ->
                            val cellSizePx = with(density) { 12.dp.toPx() }
                            val cellGapPx = with(density) { 3.dp.toPx() }
                            val stepPx = cellSizePx + cellGapPx
                            val col = (offset.x / stepPx).toInt()
                            val row = (offset.y / stepPx).toInt()
                            val cellIdx = col * 7 + row - startOffset
                            if (cellIdx in days.indices) {
                                onSelectDay(cellIdx)
                            }
                        }
                    }
            ) {
                val cellSizePx = cellSize.toPx()
                val cellGapPx = cellGap.toPx()
                val stepPx = cellSizePx + cellGapPx
                val cornerRadius = CornerRadius(cellSizePx / 2f, cellSizePx / 2f)

                days.forEachIndexed { i, day ->
                    val pos = startOffset + i
                    val col = pos / 7
                    val row = pos % 7
                    val x = col * stepPx
                    val y = row * stepPx

                    val color = when (day.level) {
                        0 -> inputColor
                        1 -> copper.copy(alpha = 0.30f)
                        2 -> copper.copy(alpha = 0.55f)
                        3 -> copper.copy(alpha = 0.80f)
                        else -> copper
                    }

                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(cellSizePx, cellSizePx),
                        cornerRadius = cornerRadius
                    )

                    if (day.index == selectedIndex) {
                        drawRoundRect(
                            color = copper,
                            topLeft = Offset(x - 1.5f, y - 1.5f),
                            size = Size(cellSizePx + 3f, cellSizePx + 3f),
                            cornerRadius = CornerRadius((cellSizePx + 3f) / 2f, (cellSizePx + 3f) / 2f),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }
        }

        // Legend: Less [5 dots] More
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Less",
                fontFamily = Lora,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(5.dp))
            listOf(
                inputColor,
                copper.copy(alpha = 0.30f),
                copper.copy(alpha = 0.55f),
                copper.copy(alpha = 0.80f),
                copper
            ).forEach { dotColor ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "More",
                fontFamily = Lora,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 8. Time of Day Card
@Composable
private fun TimeOfDayCard(
    items: List<TimeOfDayItem>,
    bestWindowTip: String?
) {
    AnalyticsCard {
        items.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (index < items.size - 1) 16.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = item.timeRange,
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${item.percent}%",
                        style = TextStyle(
                            fontFamily = Lora,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar (height 10dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth((item.percent / 100f).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(999.dp))
                            .background(CopperPrimary)
                    )
                }
            }
        }

        // Tip Line (Best Window, shown only if >= 5 sessions)
        if (bestWindowTip != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = CopperContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = CopperPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = bestWindowTip,
                        fontFamily = Lora,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = CopperPrimary
                    )
                }
            }
        }
    }
}

// 9. Session Types Card
@Composable
private fun SessionTypesCard(
    sessionTypes: List<SessionTypeItem>
) {
    val copper = CopperPrimary
    val sageColor = MaterialTheme.colorScheme.sage
    val tertiaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    AnalyticsCard {
        // Stacked pill bar: height 14dp, rounded 999dp
        val totalSec = sessionTypes.sumOf { it.totalMinutes }.coerceAtLeast(1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                sessionTypes.forEach { item ->
                    val color = when (item.mode) {
                        FocusMode.CLASSIC_POMODORO -> copper
                        FocusMode.FLOW_TIMED -> sageColor
                        FocusMode.FLOW_OPEN -> tertiaryColor
                    }
                    if (item.percent > 0) {
                        Box(
                            modifier = Modifier
                                .weight(item.percent.toFloat())
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend rows
        sessionTypes.forEach { item ->
            val color = when (item.mode) {
                FocusMode.CLASSIC_POMODORO -> copper
                FocusMode.FLOW_TIMED -> sageColor
                FocusMode.FLOW_OPEN -> tertiaryColor
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = item.label,
                        fontFamily = Lora,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${item.formattedTime} · ${item.percent}%",
                    style = TextStyle(
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// 10. Top Tasks Card
@Composable
private fun TopTasksCard(
    topTasks: List<TopTaskItem>
) {
    AnalyticsCard {
        if (topTasks.isEmpty()) {
            Text(
                text = "No linked tasks in this range",
                fontFamily = Lora,
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            topTasks.forEachIndexed { index, task ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < topTasks.size - 1) 16.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = task.title,
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.formattedTime,
                            style = TextStyle(
                                fontFamily = Lora,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar proportional to largest
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(task.percentOfMax.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(999.dp))
                                .background(CopperPrimary)
                        )
                    }
                }
            }
        }
    }
}

// 11. By Tag Card (hidden if no tags exist)
@Composable
private fun ByTagCard(
    tagStats: List<TagStatItem>
) {
    AnalyticsCard {
        if (tagStats.isEmpty()) {
            Text(
                text = "No tagged sessions in this range",
                fontFamily = Lora,
                fontSize = 13.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            tagStats.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < tagStats.size - 1) 16.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = item.formattedTime,
                            style = TextStyle(
                                fontFamily = Lora,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.percentOfMax.coerceIn(0f, 1f))
                                .clip(RoundedCornerShape(999.dp))
                                .background(CopperPrimary)
                        )
                    }
                }
            }
        }
    }
}

// 12. Distractions Blocked Card
@Composable
private fun DistractionsBlockedCard(
    attempts: Int,
    keptFormatted: String,
    mostBlockedApp: String?,
    mostBlockedCount: Int,
    onManageBlockingClick: () -> Unit
) {
    AnalyticsCard {
        // 2 Stat Tiles: Attempts and Kept
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                value = attempts.toString(),
                title = "Attempts",
                subtitle = "blocked",
                modifier = Modifier.weight(1f)
            )
            StatTile(
                value = keptFormatted,
                title = "Kept",
                subtitle = "estimated",
                modifier = Modifier.weight(1f)
            )
        }

        if (!mostBlockedApp.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Most blocked: $mostBlockedApp, $mostBlockedCount times.",
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // "Manage app blocking" button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .clickable(onClick = onManageBlockingClick),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Manage app blocking",
                    fontFamily = Lora,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
