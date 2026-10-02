package com.reflex.app

import com.reflex.app.data.Habit
import com.reflex.app.data.HabitKind
import com.reflex.app.util.HabitCalculations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitCalculationsTest {

    @Test
    fun testHabitCompletionLogic() {
        val checkOff = Habit(
            id = 1L,
            name = "Workout",
            type = HabitKind.CHECK_OFF.name,
            target = 1.0,
            startEpochDay = 100L
        )
        assertFalse(checkOff.isCompleted(null))
        assertFalse(checkOff.isCompleted(0.0))
        assertTrue(checkOff.isCompleted(1.0))
        assertTrue(checkOff.isCompleted(2.0))

        val measurable = Habit(
            id = 2L,
            name = "Read",
            type = HabitKind.MEASURABLE.name,
            target = 20.0,
            unit = "pages",
            step = 5.0,
            startEpochDay = 100L
        )
        assertFalse(measurable.isCompleted(null))
        assertFalse(measurable.isCompleted(15.0))
        assertTrue(measurable.isCompleted(20.0))
        assertTrue(measurable.isCompleted(25.0))
        assertEquals(0.5f, measurable.progressFraction(10.0), 0.01f)
        assertEquals(1.0f, measurable.progressFraction(25.0), 0.01f)

        // Stepper calculations
        assertEquals(5.0, measurable.nextStepValue(0.0, 1), 0.01)
        assertEquals(0.0, measurable.nextStepValue(5.0, -1), 0.01)
        assertEquals(0.0, measurable.nextStepValue(0.0, -1), 0.01) // doesn't go below 0
        assertEquals(60.0, measurable.nextStepValue(60.0, 1), 0.01) // capped at target * 3 = 60

        val limit = Habit(
            id = 3L,
            name = "Social Media",
            type = HabitKind.LIMIT.name,
            target = 45.0,
            unit = "min",
            step = 15.0,
            startEpochDay = 100L
        )
        assertFalse(limit.isCompleted(null)) // not logged = not done
        assertTrue(limit.isCompleted(30.0))  // <= target = done
        assertTrue(limit.isCompleted(45.0))  // <= target = done
        assertFalse(limit.isCompleted(60.0)) // > target = over limit (not done)
    }

    @Test
    fun testZeroHabits() {
        val today = LocalDate.of(2026, 9, 30)
        val stats = HabitCalculations.computeStreakStats(emptyList(), emptyMap(), today.toEpochDay())
        assertEquals(0, stats.currentStreak)
        assertEquals(0, stats.bestStreak)

        val progress = HabitCalculations.computeDailyProgress(emptyList(), emptyMap(), today.toEpochDay())
        assertEquals(0, progress.completedCount)
        assertEquals(0, progress.totalCount)
        assertEquals(0, progress.percentage)

        val top = HabitCalculations.computeTopHabits(emptyList(), emptyMap(), today)
        assertTrue(top.isEmpty())

        val workOn = HabitCalculations.computeHabitsToWorkOn(emptyList(), emptyMap(), today)
        assertTrue(workOn.isEmpty())

        val snapshot = HabitCalculations.computeMonthSnapshot(emptyList(), emptyMap(), today)
        assertEquals(0, snapshot.totalCheckIns)
        assertEquals(0, snapshot.completionRatePercentage)
    }

    @Test
    fun testStreakWhenTodayIsLoggedVsEmpty() {
        val today = LocalDate.of(2026, 9, 30)
        val todayEpoch = today.toEpochDay()
        val habit = Habit(
            id = 1L,
            name = "Workout",
            type = HabitKind.CHECK_OFF.name,
            target = 1.0,
            startEpochDay = todayEpoch - 10
        )

        // Case A: Today is logged, and yesterday was logged
        val logsToday = mapOf(
            Pair(1L, todayEpoch) to 1.0,
            Pair(1L, todayEpoch - 1) to 1.0,
            Pair(1L, todayEpoch - 2) to 1.0
        )
        val statsA = HabitCalculations.computeStreakStats(listOf(habit), logsToday, todayEpoch)
        assertEquals(3, statsA.currentStreak)
        assertEquals(3, statsA.bestStreak)

        // Case B: Today is not logged yet, but yesterday was logged
        val logsYesterday = mapOf(
            Pair(1L, todayEpoch - 1) to 1.0,
            Pair(1L, todayEpoch - 2) to 1.0
        )
        val statsB = HabitCalculations.computeStreakStats(listOf(habit), logsYesterday, todayEpoch)
        // Count from yesterday when today has none yet!
        assertEquals(2, statsB.currentStreak)
        assertEquals(2, statsB.bestStreak)

        // Case C: Neither today nor yesterday was logged
        val logsOlder = mapOf(
            Pair(1L, todayEpoch - 2) to 1.0
        )
        val statsC = HabitCalculations.computeStreakStats(listOf(habit), logsOlder, todayEpoch)
        assertEquals(0, statsC.currentStreak)
        assertEquals(1, statsC.bestStreak)
    }

    @Test
    fun testStreakAcrossMonthAndYearBoundaries() {
        // Test transition from Dec 31 to Jan 1
        val newYearDay = LocalDate.of(2026, 1, 2)
        val newYearEpoch = newYearDay.toEpochDay()

        val habit = Habit(
            id = 1L,
            name = "Workout",
            type = HabitKind.CHECK_OFF.name,
            target = 1.0,
            startEpochDay = newYearEpoch - 5
        )

        val logs = mapOf(
            Pair(1L, newYearEpoch) to 1.0,             // Jan 2
            Pair(1L, newYearEpoch - 1) to 1.0,         // Jan 1
            Pair(1L, newYearEpoch - 2) to 1.0,         // Dec 31
            Pair(1L, newYearEpoch - 3) to 1.0          // Dec 30
        )

        val stats = HabitCalculations.computeStreakStats(listOf(habit), logs, newYearEpoch)
        assertEquals(4, stats.currentStreak)
        assertEquals(4, stats.bestStreak)
    }

    @Test
    fun testDenominatorsOnHabitCreatedMidMonth() {
        // Today is Sept 20
        val today = LocalDate.of(2026, 9, 20)
        val todayEpoch = today.toEpochDay()

        // Habit was created on Sept 15 (startEpochDay = todayEpoch - 5)
        val midMonthHabit = Habit(
            id = 1L,
            name = "Meditate",
            type = HabitKind.CHECK_OFF.name,
            target = 1.0,
            startEpochDay = todayEpoch - 5 // Sept 15
        )

        // Logged on Sept 15, 16, 17, 18, 19, 20 (all 6 eligible days)
        val logs = (0..5).associate { offset ->
            Pair(1L, todayEpoch - offset) to 1.0
        }

        val topHabits = HabitCalculations.computeTopHabits(listOf(midMonthHabit), logs, today)
        assertEquals(1, topHabits.size)
        val item = topHabits[0]
        // Eligible days must be 6 (Sept 15 to Sept 20), NOT 20 days!
        assertEquals(6, item.eligibleDays)
        assertEquals(6, item.completedDays)
        assertEquals(100, item.ratePercentage)
    }

    @Test
    fun testHeatmapLevels() {
        val today = LocalDate.of(2026, 9, 30)
        val h1 = Habit(id = 1L, name = "H1", type = HabitKind.CHECK_OFF.name, target = 1.0, startEpochDay = today.toEpochDay() - 10)
        val h2 = Habit(id = 2L, name = "H2", type = HabitKind.CHECK_OFF.name, target = 1.0, startEpochDay = today.toEpochDay() - 10)
        val h3 = Habit(id = 3L, name = "H3", type = HabitKind.CHECK_OFF.name, target = 1.0, startEpochDay = today.toEpochDay() - 10)
        val habits = listOf(h1, h2, h3)

        // Day with 0 done -> level 0
        // Day with 1 of 3 (33%) -> level 1
        // Day with 2 of 3 (66%) -> level 2
        // Day with 3 of 3 (100%) -> level 4
        val d0 = today.toEpochDay() - 3
        val d1 = today.toEpochDay() - 2
        val d2 = today.toEpochDay() - 1
        val d3 = today.toEpochDay()

        val logs = mapOf(
            Pair(1L, d1) to 1.0,

            Pair(1L, d2) to 1.0,
            Pair(2L, d2) to 1.0,

            Pair(1L, d3) to 1.0,
            Pair(2L, d3) to 1.0,
            Pair(3L, d3) to 1.0
        )

        val heatmap = HabitCalculations.computeHeatmapData(habits, logs, today)
        val flatDays = heatmap.weeks.flatMap { it.days }.filterNotNull()

        val day0 = flatDays.first { it.epochDay == d0 }
        val day1 = flatDays.first { it.epochDay == d1 }
        val day2 = flatDays.first { it.epochDay == d2 }
        val day3 = flatDays.first { it.epochDay == d3 }

        assertEquals(0, day0.level)
        assertEquals(1, day1.level)
        assertEquals(2, day2.level)
        assertEquals(4, day3.level)
    }

    @Test
    fun testHabitsToWorkOnExcludesUnder7DaysOld() {
        val today = LocalDate.of(2026, 9, 30)
        val todayEpoch = today.toEpochDay()

        val youngHabit = Habit(
            id = 1L,
            name = "Newbie",
            type = HabitKind.CHECK_OFF.name,
            target = 1.0,
            startEpochDay = todayEpoch - 3 // only 4 days old
        )

        val matureHabit = Habit(
            id = 2L,
            name = "Veteran",
            type = HabitKind.CHECK_OFF.name,
            target = 1.0,
            startEpochDay = todayEpoch - 14 // 15 days old
        )

        val workOn = HabitCalculations.computeHabitsToWorkOn(listOf(youngHabit, matureHabit), emptyMap(), today)
        // youngHabit must be omitted!
        assertEquals(1, workOn.size)
        assertEquals(2L, workOn[0].habit.id)
    }
}
