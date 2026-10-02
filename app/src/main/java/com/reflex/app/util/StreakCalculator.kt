package com.reflex.app.util

import com.reflex.app.data.CompletionLog
import com.reflex.app.data.Routine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class StreakResult(
    val currentStreak: Int,
    val bestStreak: Int
)

object StreakCalculator {

    fun calculateStreaks(logs: List<CompletionLog>, routine: Routine? = null): StreakResult {
        val scheduledDays = routine?.scheduledDaysSet ?: emptySet()

        if (scheduledDays.isEmpty()) {
            // Unscheduled / Manual Routine: any calendar day with isCompleted == true
            val completedLogs = logs.filter { it.isCompleted }
            if (completedLogs.isEmpty()) {
                return StreakResult(currentStreak = 0, bestStreak = 0)
            }

            val completedDates: Set<LocalDate> = completedLogs.map { log ->
                Instant.ofEpochMilli(log.dateCompleted)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }.toSet()

            val sortedDates = completedDates.sorted()
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            var currentStreak = 0
            var checkDate = when {
                completedDates.contains(today) -> today
                completedDates.contains(yesterday) -> yesterday
                else -> null
            }

            while (checkDate != null && completedDates.contains(checkDate)) {
                currentStreak++
                checkDate = checkDate.minusDays(1)
            }

            var bestStreak = 0
            var tempStreak = 0
            var prevDate: LocalDate? = null

            for (date in sortedDates) {
                if (prevDate == null) {
                    tempStreak = 1
                } else if (date == prevDate.plusDays(1)) {
                    tempStreak++
                } else {
                    tempStreak = 1
                }
                if (tempStreak > bestStreak) {
                    bestStreak = tempStreak
                }
                prevDate = date
            }

            return StreakResult(currentStreak = currentStreak, bestStreak = bestStreak)
        } else {
            // Schedule-Aware Streak Logic with 1-Day Grace & Explicit Skips
            val logsByDate: Map<LocalDate, List<CompletionLog>> = logs.groupBy { log ->
                Instant.ofEpochMilli(log.dateCompleted)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }

            val today = LocalDate.now()
            var currentStreak = 0
            var checkDate = today

            // If today is a scheduled day and not yet logged, start checking from yesterday so today's pending state doesn't break current streak
            val todayLogs = logsByDate[today] ?: emptyList()
            if (scheduledDays.contains(today.dayOfWeek) && todayLogs.isEmpty()) {
                checkDate = today.minusDays(1)
            }

            while (true) {
                if (scheduledDays.contains(checkDate.dayOfWeek)) {
                    val dayLogs = logsByDate[checkDate] ?: emptyList()
                    val hasCompletedOnTime = dayLogs.any { it.isCompleted }
                    val hasSkipped = dayLogs.any { it.isSkipped }

                    // Grace check: was checkDate completed by end-of-day checkDate + 1?
                    val nextDayLogs = logsByDate[checkDate.plusDays(1)] ?: emptyList()
                    val hasCompletedLate = !hasCompletedOnTime && !hasSkipped && nextDayLogs.any { it.isCompleted }

                    if (hasCompletedOnTime || hasSkipped || hasCompletedLate) {
                        currentStreak++
                    } else {
                        // Missed scheduled day without grace or skip -> streak breaks
                        break
                    }
                }
                checkDate = checkDate.minusDays(1)

                // Safety bound to avoid infinite loop
                if (checkDate.isBefore(today.minusYears(1))) break
            }

            // Calculate Best Streak across historical scheduled days
            val earliestDate = logsByDate.keys.minOrNull() ?: today
            var tempStreak = 0
            var bestStreak = 0
            var evalDate = earliestDate

            while (!evalDate.isAfter(today)) {
                if (scheduledDays.contains(evalDate.dayOfWeek)) {
                    val dayLogs = logsByDate[evalDate] ?: emptyList()
                    val hasCompletedOnTime = dayLogs.any { it.isCompleted }
                    val hasSkipped = dayLogs.any { it.isSkipped }
                    val nextDayLogs = logsByDate[evalDate.plusDays(1)] ?: emptyList()
                    val hasCompletedLate = !hasCompletedOnTime && !hasSkipped && nextDayLogs.any { it.isCompleted }

                    if (hasCompletedOnTime || hasSkipped || hasCompletedLate) {
                        tempStreak++
                        if (tempStreak > bestStreak) bestStreak = tempStreak
                    } else {
                        tempStreak = 0
                    }
                }
                evalDate = evalDate.plusDays(1)
            }

            return StreakResult(currentStreak = currentStreak, bestStreak = bestStreak)
        }
    }

    fun calculateFocusStreak(sessions: List<com.reflex.app.data.FocusSession>): StreakResult {
        val completedSessions = sessions.filter { it.completed }
        if (completedSessions.isEmpty()) {
            return StreakResult(currentStreak = 0, bestStreak = 0)
        }

        val completedDates: Set<LocalDate> = completedSessions.map { session ->
            Instant.ofEpochMilli(session.startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }.toSet()

        val sortedDates = completedDates.sorted()
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)

        var currentStreak = 0
        var checkDate = when {
            completedDates.contains(today) -> today
            completedDates.contains(yesterday) -> yesterday
            else -> null
        }

        while (checkDate != null && completedDates.contains(checkDate)) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        var bestStreak = 0
        var tempStreak = 0
        var prevDate: LocalDate? = null

        for (date in sortedDates) {
            if (prevDate == null) {
                tempStreak = 1
            } else if (date == prevDate.plusDays(1)) {
                tempStreak++
            } else {
                tempStreak = 1
            }
            if (tempStreak > bestStreak) {
                bestStreak = tempStreak
            }
            prevDate = date
        }

        return StreakResult(currentStreak = currentStreak, bestStreak = bestStreak)
    }
}
