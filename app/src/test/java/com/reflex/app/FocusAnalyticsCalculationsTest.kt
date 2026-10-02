package com.reflex.app

import com.reflex.app.data.BlockedAppEvent
import com.reflex.app.data.BlockingMode
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSession
import com.reflex.app.data.FocusSettings
import com.reflex.app.data.FocusTag
import com.reflex.app.data.Task
import com.reflex.app.viewmodel.AnalyticsRange
import com.reflex.app.viewmodel.CombinedDataSources
import com.reflex.app.viewmodel.CombinedUserSelections
import com.reflex.app.viewmodel.FocusAnalyticsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class FocusAnalyticsCalculationsTest {

    @Test
    fun testFormatMinutes() {
        assertEquals("0m", FocusAnalyticsViewModel.formatMinutes(0))
        assertEquals("0m", FocusAnalyticsViewModel.formatMinutes(-5))
        assertEquals("15m", FocusAnalyticsViewModel.formatMinutes(15))
        assertEquals("59m", FocusAnalyticsViewModel.formatMinutes(59))
        assertEquals("1h", FocusAnalyticsViewModel.formatMinutes(60))
        assertEquals("1h 15m", FocusAnalyticsViewModel.formatMinutes(75))
        assertEquals("2h", FocusAnalyticsViewModel.formatMinutes(120))
        assertEquals("2h 30m", FocusAnalyticsViewModel.formatMinutes(150))
    }

    @Test
    fun testFormatBestWindow() {
        assertEquals("9 to 11 am", FocusAnalyticsViewModel.formatBestWindow(9))
        assertEquals("11 am to 1 pm", FocusAnalyticsViewModel.formatBestWindow(11))
        assertEquals("12 to 2 pm", FocusAnalyticsViewModel.formatBestWindow(12))
        assertEquals("2 to 4 pm", FocusAnalyticsViewModel.formatBestWindow(14))
        assertEquals("10 pm to 12 am", FocusAnalyticsViewModel.formatBestWindow(22))
        assertEquals("11 pm to 1 am", FocusAnalyticsViewModel.formatBestWindow(23))
    }

    @Test
    fun testEmptyState() {
        val sel = CombinedUserSelections(
            range = AnalyticsRange.WEEK,
            selectedTagId = null,
            selectedBarIndex = null,
            selectedHeatmapIndex = null,
            goalMinutes = 120
        )
        val data = CombinedDataSources(
            sessions = emptyList(),
            tags = emptyList(),
            tasks = emptyList(),
            blockedEvents = emptyList(),
            focusSettings = FocusSettings(blockingMode = BlockingMode.OFF)
        )

        val result = FocusAnalyticsViewModel.computeAnalytics(sel, data, null)

        assertEquals("0m", result.focusTimeFormatted)
        assertEquals(0, result.sessionsCount)
        assertEquals("0m", result.averageFormatted)
        assertEquals(0, result.streakDays)
        assertEquals("0m", result.bestDayFormatted)
        assertEquals("-", result.finishedPercentStr)
        assertTrue(result.isBarChartEmpty)
        assertNull(result.bestWindowTip)
        assertEquals(365, result.heatmapDays.size)
        assertTrue(result.heatmapDays.all { it.level == 0 })
        assertFalse(result.showTagCard)
        assertFalse(result.distractionsBlocked.isVisible)
    }

    @Test
    fun testSessionDurationFilteringAndStreak() {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)

        val todayMs = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val yesterdayMs = yesterday.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()
        val twoDaysAgoMs = twoDaysAgo.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val sessions = listOf(
            // Session today >= 60s -> counted
            FocusSession(
                id = 1,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = todayMs,
                endTime = todayMs + 1500_000,
                plannedDurationSeconds = 1500,
                actualDurationSeconds = 1500, // 25m
                completedCycles = 1,
                completed = true
            ),
            // Session today < 60s -> should be ignored as not counted
            FocusSession(
                id = 2,
                mode = FocusMode.FLOW_TIMED,
                startTime = todayMs + 2000_000,
                endTime = todayMs + 2030_000,
                plannedDurationSeconds = 1800,
                actualDurationSeconds = 30, // 30s -> ignored
                completedCycles = 0,
                completed = false
            ),
            // Session yesterday >= 60s -> counted
            FocusSession(
                id = 3,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = yesterdayMs,
                endTime = yesterdayMs + 3600_000,
                plannedDurationSeconds = 1500,
                actualDurationSeconds = 3600, // 60m
                completedCycles = 1,
                completed = true
            ),
            // Session 2 days ago >= 60s -> counted
            FocusSession(
                id = 4,
                mode = FocusMode.FLOW_OPEN,
                startTime = twoDaysAgoMs,
                endTime = twoDaysAgoMs + 1800_000,
                plannedDurationSeconds = null,
                actualDurationSeconds = 1800, // 30m
                completedCycles = 1,
                completed = false // Ended early
            )
        )

        val sel = CombinedUserSelections(
            range = AnalyticsRange.WEEK,
            selectedTagId = null,
            selectedBarIndex = null,
            selectedHeatmapIndex = null,
            goalMinutes = 120
        )
        val data = CombinedDataSources(
            sessions = sessions,
            tags = emptyList(),
            tasks = emptyList(),
            blockedEvents = emptyList(),
            focusSettings = FocusSettings()
        )

        val result = FocusAnalyticsViewModel.computeAnalytics(sel, data, null)

        // Counted sessions: 3 (25m + 60m + 30m = 115m)
        assertEquals(3, result.sessionsCount)
        assertEquals("1h 55m", result.focusTimeFormatted)
        assertEquals("38m", result.averageFormatted) // 115 / 3 = 38m

        // Streak: today, yesterday, 2 days ago -> 3 consecutive days
        assertEquals(3, result.streakDays)

        // Best day: yesterday (60m)
        assertEquals("1h", result.bestDayFormatted)

        // Finished %: 2 completed out of 3 counted = 66%
        assertEquals("66%", result.finishedPercentStr)

        // Today focus: 25m of 2h = 20%
        assertEquals(25, result.todayFocusMinutes)
        assertEquals(20, result.todayGoalPercent)
    }

    @Test
    fun testTagFilterAppliesToAllCalculations() {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()
        val todayMs = today.atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli()

        val tagA = FocusTag(id = 101, name = "Deep Work", createdAt = 0)
        val tagB = FocusTag(id = 102, name = "Study", createdAt = 0)

        val sessions = listOf(
            FocusSession(
                id = 1,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = todayMs,
                endTime = todayMs + 1800_000,
                plannedDurationSeconds = 1800,
                actualDurationSeconds = 1800, // 30m
                completedCycles = 1,
                completed = true,
                tagId = 101 // Tag A
            ),
            FocusSession(
                id = 2,
                mode = FocusMode.FLOW_TIMED,
                startTime = todayMs + 2000_000,
                endTime = todayMs + 5600_000,
                plannedDurationSeconds = 3600,
                actualDurationSeconds = 3600, // 60m
                completedCycles = 1,
                completed = true,
                tagId = 102 // Tag B
            )
        )

        // Filter by Tag A
        val selA = CombinedUserSelections(
            range = AnalyticsRange.WEEK,
            selectedTagId = 101,
            selectedBarIndex = null,
            selectedHeatmapIndex = null,
            goalMinutes = 120
        )
        val data = CombinedDataSources(
            sessions = sessions,
            tags = listOf(tagA, tagB),
            tasks = emptyList(),
            blockedEvents = emptyList(),
            focusSettings = FocusSettings()
        )

        val resultA = FocusAnalyticsViewModel.computeAnalytics(selA, data, null)
        assertEquals(1, resultA.sessionsCount)
        assertEquals("30m", resultA.focusTimeFormatted)
        assertEquals(30, resultA.todayFocusMinutes)

        // Filter by Tag B
        val selB = selA.copy(selectedTagId = 102)
        val resultB = FocusAnalyticsViewModel.computeAnalytics(selB, data, null)
        assertEquals(1, resultB.sessionsCount)
        assertEquals("1h", resultB.focusTimeFormatted)
        assertEquals(60, resultB.todayFocusMinutes)

        // All Tags
        val selAll = selA.copy(selectedTagId = null)
        val resultAll = FocusAnalyticsViewModel.computeAnalytics(selAll, data, null)
        assertEquals(2, resultAll.sessionsCount)
        assertEquals("1h 30m", resultAll.focusTimeFormatted)
        assertEquals(90, resultAll.todayFocusMinutes)
    }

    @Test
    fun testBestWindowCalculation() {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()

        // Create 5 sessions between 9:00 and 10:30 am
        val sessions = (0 until 5).map { i ->
            val time = today.minusDays(i.toLong()).atTime(9, 15).atZone(zoneId).toInstant().toEpochMilli()
            FocusSession(
                id = (i + 1).toLong(),
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = time,
                endTime = time + 1500_000,
                plannedDurationSeconds = 1500,
                actualDurationSeconds = 1500,
                completedCycles = 1,
                completed = true
            )
        }

        val sel = CombinedUserSelections(
            range = AnalyticsRange.WEEK,
            selectedTagId = null,
            selectedBarIndex = null,
            selectedHeatmapIndex = null,
            goalMinutes = 120
        )
        val data = CombinedDataSources(
            sessions = sessions,
            tags = emptyList(),
            tasks = emptyList(),
            blockedEvents = emptyList(),
            focusSettings = FocusSettings()
        )

        val result = FocusAnalyticsViewModel.computeAnalytics(sel, data, null)

        assertNotNull(result.bestWindowTip)
        assertEquals("Best window: 9 to 11 am", result.bestWindowTip)
    }

    @Test
    fun testHeatmapIntensityLevels() {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now()

        val sessions = listOf(
            // 20 min -> Level 1
            FocusSession(
                id = 1,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = today.minusDays(1).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
                endTime = today.minusDays(1).atTime(10, 20).atZone(zoneId).toInstant().toEpochMilli(),
                plannedDurationSeconds = 1200,
                actualDurationSeconds = 1200,
                completedCycles = 1,
                completed = true
            ),
            // 45 min -> Level 2
            FocusSession(
                id = 2,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = today.minusDays(2).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
                endTime = today.minusDays(2).atTime(10, 45).atZone(zoneId).toInstant().toEpochMilli(),
                plannedDurationSeconds = 2700,
                actualDurationSeconds = 2700,
                completedCycles = 1,
                completed = true
            ),
            // 90 min -> Level 3
            FocusSession(
                id = 3,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = today.minusDays(3).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
                endTime = today.minusDays(3).atTime(11, 30).atZone(zoneId).toInstant().toEpochMilli(),
                plannedDurationSeconds = 5400,
                actualDurationSeconds = 5400,
                completedCycles = 1,
                completed = true
            ),
            // 150 min -> Level 4
            FocusSession(
                id = 4,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = today.minusDays(4).atTime(10, 0).atZone(zoneId).toInstant().toEpochMilli(),
                endTime = today.minusDays(4).atTime(12, 30).atZone(zoneId).toInstant().toEpochMilli(),
                plannedDurationSeconds = 9000,
                actualDurationSeconds = 9000,
                completedCycles = 1,
                completed = true
            )
        )

        val sel = CombinedUserSelections(
            range = AnalyticsRange.WEEK,
            selectedTagId = null,
            selectedBarIndex = null,
            selectedHeatmapIndex = null,
            goalMinutes = 120
        )
        val data = CombinedDataSources(
            sessions = sessions,
            tags = emptyList(),
            tasks = emptyList(),
            blockedEvents = emptyList(),
            focusSettings = FocusSettings()
        )

        val result = FocusAnalyticsViewModel.computeAnalytics(sel, data, null)
        val day1 = result.heatmapDays.find { it.date == today.minusDays(1) }
        val day2 = result.heatmapDays.find { it.date == today.minusDays(2) }
        val day3 = result.heatmapDays.find { it.date == today.minusDays(3) }
        val day4 = result.heatmapDays.find { it.date == today.minusDays(4) }
        val day0 = result.heatmapDays.find { it.date == today }

        assertEquals(1, day1?.level)
        assertEquals(2, day2?.level)
        assertEquals(3, day3?.level)
        assertEquals(4, day4?.level)
        assertEquals(0, day0?.level)
    }

    @Test
    fun testDistractionsBlocked() {
        val now = System.currentTimeMillis()
        val blockedEvents = listOf(
            BlockedAppEvent(packageName = "com.instagram.android", timestamp = now - 1000),
            BlockedAppEvent(packageName = "com.instagram.android", timestamp = now - 2000),
            BlockedAppEvent(packageName = "com.instagram.android", timestamp = now - 3000),
            BlockedAppEvent(packageName = "com.google.android.youtube", timestamp = now - 4000)
        )

        val sel = CombinedUserSelections(
            range = AnalyticsRange.WEEK,
            selectedTagId = null,
            selectedBarIndex = null,
            selectedHeatmapIndex = null,
            goalMinutes = 120
        )
        val data = CombinedDataSources(
            sessions = emptyList(),
            tags = emptyList(),
            tasks = emptyList(),
            blockedEvents = blockedEvents,
            focusSettings = FocusSettings(blockingMode = BlockingMode.BLOCK_LIST)
        )

        val result = FocusAnalyticsViewModel.computeAnalytics(sel, data, null)

        assertTrue(result.distractionsBlocked.isVisible)
        assertEquals(4, result.distractionsBlocked.attempts)
        assertEquals("8m", result.distractionsBlocked.keptFormatted)
        assertEquals(3, result.distractionsBlocked.mostBlockedCount)
    }
}
