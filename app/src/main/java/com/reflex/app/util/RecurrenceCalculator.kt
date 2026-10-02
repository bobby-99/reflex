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

    fun computeNextOccurrence(task: Task, completionTimeMillis: Long = System.currentTimeMillis()): Task? {
        if (task.recurrenceFrequency == RecurrenceFrequency.NONE) return null

        val currentCount = task.recurrenceOccurrenceCount + 1

        // Check end condition: AFTER_OCCURRENCES
        if (task.recurrenceEndType == RecurrenceEndType.AFTER_OCCURRENCES &&
            task.recurrenceEndOccurrences > 0 &&
            currentCount >= task.recurrenceEndOccurrences
        ) {
            return null
        }

        // Base date calculation
        val baseDate: LocalDate = if (task.recurrenceBasis == RecurrenceBasis.FROM_COMPLETION_DATE) {
            Instant.ofEpochMilli(completionTimeMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            val dueMillis = task.dueDate ?: completionTimeMillis
            Instant.ofEpochMilli(dueMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        }

        val baseTime: LocalTime? = if (task.dueTime != null) {
            Instant.ofEpochMilli(task.dueTime).atZone(ZoneId.systemDefault()).toLocalTime()
        } else null

        val nextDate: LocalDate = when (task.recurrenceFrequency) {
            RecurrenceFrequency.DAILY -> {
                baseDate.plusDays(task.recurrenceInterval.coerceAtLeast(1).toLong())
            }
            RecurrenceFrequency.WEEKLY -> {
                calculateNextWeeklyDate(baseDate, task.recurrenceInterval, task.recurrenceDaysOfWeek)
            }
            RecurrenceFrequency.MONTHLY -> {
                calculateNextMonthlyDate(baseDate, task.recurrenceInterval, task.recurrenceMonthlyMode)
            }
            RecurrenceFrequency.YEARLY -> {
                baseDate.plusYears(task.recurrenceInterval.coerceAtLeast(1).toLong())
            }
            RecurrenceFrequency.CUSTOM -> {
                when (task.recurrenceUnit) {
                    RecurrenceUnit.DAY -> baseDate.plusDays(task.recurrenceInterval.coerceAtLeast(1).toLong())
                    RecurrenceUnit.WEEK -> calculateNextWeeklyDate(baseDate, task.recurrenceInterval, task.recurrenceDaysOfWeek)
                    RecurrenceUnit.MONTH -> calculateNextMonthlyDate(baseDate, task.recurrenceInterval, task.recurrenceMonthlyMode)
                    RecurrenceUnit.YEAR -> baseDate.plusYears(task.recurrenceInterval.coerceAtLeast(1).toLong())
                }
            }
            RecurrenceFrequency.NONE -> return null
        }

        // Check end condition: ON_DATE
        if (task.recurrenceEndType == RecurrenceEndType.ON_DATE && task.recurrenceEndDate != null) {
            val endDate = Instant.ofEpochMilli(task.recurrenceEndDate).atZone(ZoneId.systemDefault()).toLocalDate()
            if (nextDate.isAfter(endDate)) {
                return null
            }
        }

        val nextDueDateMillis = nextDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextDueTimeMillis = if (baseTime != null) {
            LocalDateTime.of(nextDate, baseTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null

        val nextReminderTimeMillis = when {
            nextDueTimeMillis != null -> nextDueTimeMillis
            task.reminderTime != null -> {
                val originalReminderTime = Instant.ofEpochMilli(task.reminderTime).atZone(ZoneId.systemDefault()).toLocalTime()
                LocalDateTime.of(nextDate, originalReminderTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            else -> LocalDateTime.of(nextDate, LocalTime.of(9, 0)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
                var checkDate = baseDate.plusDays(1)
                for (i in 1..365) {
                    if (targetDays.contains(checkDate.dayOfWeek)) {
                        return checkDate
                    }
                    checkDate = checkDate.plusDays(1)
                }
            }
        }
        return baseDate.plusWeeks(interval.coerceAtLeast(1).toLong())
    }

    private fun calculateNextMonthlyDate(baseDate: LocalDate, interval: Int, mode: MonthlyMode): LocalDate {
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
        return nextMonthDate
    }
}
