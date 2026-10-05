package com.reflex.app.util

import com.reflex.app.data.MonthlyMode
import com.reflex.app.data.RecurrenceBasis
import com.reflex.app.data.RecurrenceEndType
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.RecurrenceUnit
import com.reflex.app.data.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object RecurrenceCalculator {

    fun computeNextOccurrence(
        task: Task,
        completionTimeMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Task? {
        if (task.recurrenceFrequency == RecurrenceFrequency.NONE) return null

        val currentCount = task.recurrenceOccurrenceCount + 1

        // Check end condition: AFTER_OCCURRENCES
        if (task.recurrenceEndType == RecurrenceEndType.AFTER_OCCURRENCES &&
            task.recurrenceEndOccurrences > 0 &&
            currentCount >= task.recurrenceEndOccurrences
        ) {
            return null
        }

        val completionDate = Instant.ofEpochMilli(completionTimeMillis).atZone(zoneId).toLocalDate()

        // Base date calculation
        val baseDate: LocalDate = if (task.recurrenceBasis == RecurrenceBasis.FROM_COMPLETION_DATE) {
            completionDate
        } else {
            val dueMillis = task.dueDate ?: completionTimeMillis
            Instant.ofEpochMilli(dueMillis).atZone(zoneId).toLocalDate()
        }

        val baseTime: LocalTime? = if (task.dueTime != null) {
            Instant.ofEpochMilli(task.dueTime).atZone(zoneId).toLocalTime()
        } else null

        var nextDate: LocalDate = when (task.recurrenceFrequency) {
            RecurrenceFrequency.DAILY -> {
                baseDate.plusDays(task.recurrenceInterval.coerceAtLeast(1).toLong())
            }
            RecurrenceFrequency.WEEKLY -> {
                calculateNextWeeklyDate(baseDate, task.recurrenceInterval, task.recurrenceDaysOfWeek)
            }
            RecurrenceFrequency.MONTHLY -> {
                calculateNextMonthlyDate(baseDate, task.recurrenceInterval, task.recurrenceMonthlyMode, task, zoneId)
            }
            RecurrenceFrequency.YEARLY -> {
                baseDate.plusYears(task.recurrenceInterval.coerceAtLeast(1).toLong())
            }
            RecurrenceFrequency.CUSTOM -> {
                when (task.recurrenceUnit) {
                    RecurrenceUnit.DAY -> baseDate.plusDays(task.recurrenceInterval.coerceAtLeast(1).toLong())
                    RecurrenceUnit.WEEK -> calculateNextWeeklyDate(baseDate, task.recurrenceInterval, task.recurrenceDaysOfWeek)
                    RecurrenceUnit.MONTH -> calculateNextMonthlyDate(baseDate, task.recurrenceInterval, task.recurrenceMonthlyMode, task, zoneId)
                    RecurrenceUnit.YEAR -> baseDate.plusYears(task.recurrenceInterval.coerceAtLeast(1).toLong())
                }
            }
            RecurrenceFrequency.NONE -> return null
        }

        // If completing late under FROM_DUE_DATE basis, roll forward so next occurrence is not overdue
        if (task.recurrenceBasis == RecurrenceBasis.FROM_DUE_DATE && nextDate.isBefore(completionDate)) {
            var rollDate = nextDate
            var safetyCycles = 0
            while (rollDate.isBefore(completionDate) && safetyCycles < 1000) {
                rollDate = when (task.recurrenceFrequency) {
                    RecurrenceFrequency.DAILY -> rollDate.plusDays(task.recurrenceInterval.coerceAtLeast(1).toLong())
                    RecurrenceFrequency.WEEKLY -> calculateNextWeeklyDate(rollDate, task.recurrenceInterval, task.recurrenceDaysOfWeek)
                    RecurrenceFrequency.MONTHLY -> calculateNextMonthlyDate(rollDate, task.recurrenceInterval, task.recurrenceMonthlyMode, task, zoneId)
                    RecurrenceFrequency.YEARLY -> rollDate.plusYears(task.recurrenceInterval.coerceAtLeast(1).toLong())
                    RecurrenceFrequency.CUSTOM -> when (task.recurrenceUnit) {
                        RecurrenceUnit.DAY -> rollDate.plusDays(task.recurrenceInterval.coerceAtLeast(1).toLong())
                        RecurrenceUnit.WEEK -> calculateNextWeeklyDate(rollDate, task.recurrenceInterval, task.recurrenceDaysOfWeek)
                        RecurrenceUnit.MONTH -> calculateNextMonthlyDate(rollDate, task.recurrenceInterval, task.recurrenceMonthlyMode, task, zoneId)
                        RecurrenceUnit.YEAR -> rollDate.plusYears(task.recurrenceInterval.coerceAtLeast(1).toLong())
                    }
                    else -> rollDate.plusDays(1)
                }
                safetyCycles++
            }
            nextDate = rollDate
        }

        // Check end condition: ON_DATE
        if (task.recurrenceEndType == RecurrenceEndType.ON_DATE && task.recurrenceEndDate != null) {
            val endDate = Instant.ofEpochMilli(task.recurrenceEndDate).atZone(zoneId).toLocalDate()
            if (nextDate.isAfter(endDate)) {
                return null
            }
        }

        val nextDueDateMillis = nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nextDueTimeMillis = if (baseTime != null) {
            LocalDateTime.of(nextDate, baseTime).atZone(zoneId).toInstant().toEpochMilli()
        } else null

        val nextReminderTimeMillis = when {
            nextDueTimeMillis != null -> nextDueTimeMillis
            task.reminderTime != null -> {
                val originalReminderTime = Instant.ofEpochMilli(task.reminderTime).atZone(zoneId).toLocalTime()
                LocalDateTime.of(nextDate, originalReminderTime).atZone(zoneId).toInstant().toEpochMilli()
            }
            else -> LocalDateTime.of(nextDate, LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
        }

        return task.copy(
            id = 0,
            isCompleted = false,
            completedAt = null,
            dueDate = nextDueDateMillis,
            dueTime = nextDueTimeMillis,
            reminderTime = nextReminderTimeMillis,
            recurrenceOccurrenceCount = currentCount
        )
    }

    private fun calculateNextWeeklyDate(baseDate: LocalDate, interval: Int, daysOfWeekStr: String?): LocalDate {
        if (!daysOfWeekStr.isNullOrBlank()) {
            val targetDays = daysOfWeekStr.split(",")
                .mapNotNull { dayName ->
                    runCatching { DayOfWeek.valueOf(dayName.trim().uppercase()) }.getOrNull()
                }.toSet()

            if (targetDays.isNotEmpty()) {
                val currentDow = baseDate.dayOfWeek
                // Look for remaining days in the same week
                var sameWeekCheck = baseDate.plusDays(1)
                while (sameWeekCheck.dayOfWeek.value > currentDow.value) {
                    if (targetDays.contains(sameWeekCheck.dayOfWeek)) {
                        return sameWeekCheck
                    }
                    sameWeekCheck = sameWeekCheck.plusDays(1)
                }

                // If no more days in current week, jump to the first matching day in the next cycle week
                val weeksToJump = interval.coerceAtLeast(1).toLong()
                // Find start of week for baseDate
                val startOfNextCycleWeek = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(weeksToJump)

                var checkDate = startOfNextCycleWeek
                for (i in 0..6) {
                    if (targetDays.contains(checkDate.dayOfWeek)) {
                        return checkDate
                    }
                    checkDate = checkDate.plusDays(1)
                }
            }
        }
        return baseDate.plusWeeks(interval.coerceAtLeast(1).toLong())
    }

    private fun calculateNextMonthlyDate(
        baseDate: LocalDate,
        interval: Int,
        mode: MonthlyMode,
        task: Task? = null,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LocalDate {
        val nextMonthDate = baseDate.plusMonths(interval.coerceAtLeast(1).toLong())
        if (mode == MonthlyMode.SAME_WEEKDAY_POS) {
            val dayOfWeek = baseDate.dayOfWeek
            val weekOfMonth = (baseDate.dayOfMonth - 1) / 7 + 1
            val adjuster = if (weekOfMonth >= 5) {
                TemporalAdjusters.lastInMonth(dayOfWeek)
            } else {
                TemporalAdjusters.dayOfWeekInMonth(weekOfMonth, dayOfWeek)
            }
            return nextMonthDate.with(adjuster)
        }

        // Preserve original day of month (e.g. Jan 31 -> Feb 28 -> March 31, or leap day Feb 29)
        val originalDayOfMonth = if (task != null) {
            Instant.ofEpochMilli(task.createdAt).atZone(zoneId).toLocalDate().dayOfMonth
        } else {
            baseDate.dayOfMonth
        }
        val daysInMonth = java.time.YearMonth.from(nextMonthDate).lengthOfMonth()
        val targetDay = minOf(originalDayOfMonth, daysInMonth)
        return nextMonthDate.withDayOfMonth(targetDay)
    }
}
