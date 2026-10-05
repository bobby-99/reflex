package com.reflex.app

import com.reflex.app.data.CompletionLog
import com.reflex.app.data.MonthlyMode
import com.reflex.app.data.RecurrenceBasis
import com.reflex.app.data.RecurrenceEndType
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.Routine
import com.reflex.app.data.Task
import com.reflex.app.util.RecurrenceCalculator
import com.reflex.app.util.StreakCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class StreakAndRecurrenceEdgeCasesTest {

    private val utcZone = ZoneId.of("UTC")

    // --- StreakCalculator Edge Cases ---

    @Test
    fun testScheduleAwareStreaksWithOffDays() {
        // Routine scheduled only on MONDAY and THURSDAY
        val routine = Routine(
            id = 1L,
            name = "Gym",
            scheduledDays = "MONDAY,THURSDAY"
        )

        // 2026-10-05 is Monday, 2026-10-08 is Thursday
        val mondayEpoch = ZonedDateTime.of(2026, 10, 5, 10, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val thursdayEpoch = ZonedDateTime.of(2026, 10, 8, 10, 0, 0, 0, utcZone).toInstant().toEpochMilli()

        val logs = listOf(
            CompletionLog(routineId = 1L, dateCompleted = mondayEpoch, totalTimeTakenSeconds = 300, stepsCompletedCount = 5, isCompleted = true),
            CompletionLog(routineId = 1L, dateCompleted = thursdayEpoch, totalTimeTakenSeconds = 300, stepsCompletedCount = 5, isCompleted = true)
        )

        // Evaluated on Thursday
        val result = StreakCalculator.calculateStreaks(
            logs = logs,
            routine = routine,
            zoneId = utcZone,
            referenceDate = LocalDate.of(2026, 10, 8)
        )

        assertEquals("Streak should be 2 despite Tuesday and Wednesday being off-days", 2, result.currentStreak)
        assertEquals(2, result.bestStreak)
    }

    @Test
    fun testGraceDayAllowsLateCompletionWithoutDoubleCounting() {
        // Routine scheduled only on MONDAY
        val routine = Routine(
            id = 2L,
            name = "Weekly Review",
            scheduledDays = "MONDAY"
        )

        // Missed on Monday (2026-10-05), completed on Tuesday (2026-10-06)
        val tuesdayEpoch = ZonedDateTime.of(2026, 10, 6, 9, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val logs = listOf(
            CompletionLog(routineId = 2L, dateCompleted = tuesdayEpoch, totalTimeTakenSeconds = 600, stepsCompletedCount = 3, isCompleted = true)
        )

        // Evaluated on Wednesday (2026-10-07)
        val result = StreakCalculator.calculateStreaks(
            logs = logs,
            routine = routine,
            zoneId = utcZone,
            referenceDate = LocalDate.of(2026, 10, 7)
        )

        assertEquals("Grace day should count Tuesday log as completing Monday routine", 1, result.currentStreak)
    }

    @Test
    fun testSkipTodayPreservesStreak() {
        val routine = Routine(
            id = 3L,
            name = "Daily Reading",
            scheduledDays = "MONDAY,TUESDAY,WEDNESDAY"
        )

        // Monday completed, Tuesday skipped, Wednesday completed
        val monEpoch = ZonedDateTime.of(2026, 10, 5, 8, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val tueEpoch = ZonedDateTime.of(2026, 10, 6, 8, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val wedEpoch = ZonedDateTime.of(2026, 10, 7, 8, 0, 0, 0, utcZone).toInstant().toEpochMilli()

        val logs = listOf(
            CompletionLog(routineId = 3L, dateCompleted = monEpoch, totalTimeTakenSeconds = 120, stepsCompletedCount = 1, isCompleted = true, isSkipped = false),
            CompletionLog(routineId = 3L, dateCompleted = tueEpoch, totalTimeTakenSeconds = 0, stepsCompletedCount = 0, isCompleted = false, isSkipped = true),
            CompletionLog(routineId = 3L, dateCompleted = wedEpoch, totalTimeTakenSeconds = 120, stepsCompletedCount = 1, isCompleted = true, isSkipped = false)
        )

        val result = StreakCalculator.calculateStreaks(
            logs = logs,
            routine = routine,
            zoneId = utcZone,
            referenceDate = LocalDate.of(2026, 10, 7)
        )

        assertEquals("Explicit skip on Tuesday must preserve streak across Monday to Wednesday", 3, result.currentStreak)
    }

    @Test
    fun testTimezoneChangeMidStreak() {
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        val newYorkZone = ZoneId.of("America/New_York")

        // User completed task in Tokyo on Oct 5 at 23:30 Tokyo time
        // Oct 5 23:30 JST = Oct 5 14:30 UTC = Oct 5 10:30 EDT
        val jstEpoch = ZonedDateTime.of(2026, 10, 5, 23, 30, 0, 0, tokyoZone).toInstant().toEpochMilli()

        // User next completed task in New York on Oct 6 at 18:00 NY time
        val nyEpoch = ZonedDateTime.of(2026, 10, 6, 18, 0, 0, 0, newYorkZone).toInstant().toEpochMilli()

        val logs = listOf(
            CompletionLog(routineId = 4L, dateCompleted = jstEpoch, totalTimeTakenSeconds = 60, stepsCompletedCount = 1, isCompleted = true),
            CompletionLog(routineId = 4L, dateCompleted = nyEpoch, totalTimeTakenSeconds = 60, stepsCompletedCount = 1, isCompleted = true)
        )

        // Evaluated in New York on Oct 6
        val result = StreakCalculator.calculateStreaks(
            logs = logs,
            routine = null, // unscheduled daily
            zoneId = newYorkZone,
            referenceDate = LocalDate.of(2026, 10, 6)
        )

        assertEquals("Streak should be continuous across international flight", 2, result.currentStreak)
    }

    // --- RecurrenceCalculator Edge Cases ---

    @Test
    fun testMonthlyRecurrencePreservesJan31AcrossShorterMonths() {
        val jan31 = ZonedDateTime.of(2026, 1, 31, 9, 0, 0, 0, utcZone)
        val initialTask = Task(
            id = 101L,
            title = "Pay Rent",
            dueDate = jan31.toInstant().toEpochMilli(),
            createdAt = jan31.toInstant().toEpochMilli(),
            recurrenceFrequency = RecurrenceFrequency.MONTHLY,
            recurrenceInterval = 1,
            recurrenceMonthlyMode = MonthlyMode.SAME_DATE
        )

        // 1st occurrence: Jan 31 -> next is Feb 28
        val febTask = RecurrenceCalculator.computeNextOccurrence(initialTask, completionTimeMillis = jan31.toInstant().toEpochMilli(), zoneId = utcZone)
        assertNotNull(febTask)
        val febDate = Instant.ofEpochMilli(febTask!!.dueDate!!).atZone(utcZone).toLocalDate()
        assertEquals(LocalDate.of(2026, 2, 28), febDate)

        // When completing the Feb 28 task, next occurrence MUST return to March 31!
        val marchTask = RecurrenceCalculator.computeNextOccurrence(febTask, completionTimeMillis = ZonedDateTime.of(2026, 2, 28, 9, 0, 0, 0, utcZone).toInstant().toEpochMilli(), zoneId = utcZone)
        assertNotNull(marchTask)
        val marchDate = Instant.ofEpochMilli(marchTask!!.dueDate!!).atZone(utcZone).toLocalDate()
        assertEquals("March occurrence must be March 31st, not March 28th!", LocalDate.of(2026, 3, 31), marchDate)

        // When completing the March 31 task, next is April 30
        val aprilTask = RecurrenceCalculator.computeNextOccurrence(marchTask, completionTimeMillis = ZonedDateTime.of(2026, 3, 31, 9, 0, 0, 0, utcZone).toInstant().toEpochMilli(), zoneId = utcZone)
        val aprilDate = Instant.ofEpochMilli(aprilTask!!.dueDate!!).atZone(utcZone).toLocalDate()
        assertEquals(LocalDate.of(2026, 4, 30), aprilDate)

        // When completing April 30 task, next returns to May 31
        val mayTask = RecurrenceCalculator.computeNextOccurrence(aprilTask, completionTimeMillis = ZonedDateTime.of(2026, 4, 30, 9, 0, 0, 0, utcZone).toInstant().toEpochMilli(), zoneId = utcZone)
        val mayDate = Instant.ofEpochMilli(mayTask!!.dueDate!!).atZone(utcZone).toLocalDate()
        assertEquals(LocalDate.of(2026, 5, 31), mayDate)
    }

    @Test
    fun testLeapDayRecurrencePreservesFeb29() {
        val leapDay2024 = ZonedDateTime.of(2024, 2, 29, 12, 0, 0, 0, utcZone)
        val task = Task(
            id = 202L,
            title = "Leap Day Celebration",
            dueDate = leapDay2024.toInstant().toEpochMilli(),
            createdAt = leapDay2024.toInstant().toEpochMilli(),
            recurrenceFrequency = RecurrenceFrequency.YEARLY,
            recurrenceInterval = 1
        )

        // 2025 (non-leap year) -> Feb 28
        val task2025 = RecurrenceCalculator.computeNextOccurrence(task, completionTimeMillis = leapDay2024.toInstant().toEpochMilli(), zoneId = utcZone)
        assertNotNull(task2025)
        val date2025 = Instant.ofEpochMilli(task2025!!.dueDate!!).atZone(utcZone).toLocalDate()
        assertEquals(LocalDate.of(2025, 2, 28), date2025)
    }

    @Test
    fun testCompletingRepeatingTaskLateRollsForwardToUpcomingCycle() {
        // Task was due 2026-10-01 (Daily). Completed late on 2026-10-05.
        val dueOct1 = ZonedDateTime.of(2026, 10, 1, 9, 0, 0, 0, utcZone).toInstant().toEpochMilli()
        val completionOct5 = ZonedDateTime.of(2026, 10, 5, 14, 0, 0, 0, utcZone).toInstant().toEpochMilli()

        val task = Task(
            id = 303L,
            title = "Daily Journal",
            dueDate = dueOct1,
            recurrenceFrequency = RecurrenceFrequency.DAILY,
            recurrenceInterval = 1,
            recurrenceBasis = RecurrenceBasis.FROM_DUE_DATE
        )

        val nextTask = RecurrenceCalculator.computeNextOccurrence(task, completionTimeMillis = completionOct5, zoneId = utcZone)
        assertNotNull(nextTask)
        val nextDate = Instant.ofEpochMilli(nextTask!!.dueDate!!).atZone(utcZone).toLocalDate()

        // It must NOT be Oct 2 (which is in the past). It should be on or after Oct 5!
        assertTrue("Next occurrence must not be in the past relative to completion date", !nextDate.isBefore(LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun testRecurrenceEndConditionAfterOccurrences() {
        val task = Task(
            id = 404L,
            title = "Physical Therapy",
            dueDate = ZonedDateTime.of(2026, 10, 5, 9, 0, 0, 0, utcZone).toInstant().toEpochMilli(),
            recurrenceFrequency = RecurrenceFrequency.DAILY,
            recurrenceEndType = RecurrenceEndType.AFTER_OCCURRENCES,
            recurrenceEndOccurrences = 3,
            recurrenceOccurrenceCount = 2 // Current completion is the 3rd occurrence
        )

        val nextTask = RecurrenceCalculator.computeNextOccurrence(task, zoneId = utcZone)
        assertNull("Should return null when reaching recurrenceEndOccurrences limit", nextTask)
    }
}
