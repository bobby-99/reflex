package com.reflex.app.util

import com.reflex.app.data.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

data class HabitDayProgress(
    val completedCount: Int,
    val totalCount: Int,
    val fraction: Float,
    val percentage: Int
)

data class StreakStats(
    val currentStreak: Int,
    val bestStreak: Int
)

data class TopHabitItem(
    val habit: Habit,
    val completedDays: Int,
    val eligibleDays: Int,
    val ratePercentage: Int
)

data class HabitToWorkOnItem(
    val habit: Habit,
    val missedCount: Int,
    val windowDays: Int,
    val daysSinceLastCompleted: Int?, // null if never this month, 0 if today, 1 if yesterday, etc.
    val monthlyRatePercentage: Int,
    val last7DayStatus: List<Boolean> // true if completed on that day, false if missed or before start
)

data class MonthSnapshotStats(
    val totalCheckIns: Int,
    val completionRatePercentage: Int,
    val bestStreakThisMonth: Int,
    val perfectDays: Int
)

data class HeatmapDay(
    val epochDay: Long,
    val localDate: LocalDate,
    val level: Int, // 0..4
    val completedCount: Int,
    val totalActiveHabits: Int
)

data class HeatmapWeek(
    val days: List<HeatmapDay?> // size 7: Sunday (index 0) to Saturday (index 6)
)

data class HeatmapData(
    val weeks: List<HeatmapWeek>,
    val monthLabels: List<Pair<Int, String>> // week index to short month name (e.g. "Jan", "Feb")
)

data class HabitAnalysisItem(
    val title: String,
    val description: String,
    val highlight: String
)

object HabitCalculations {

    fun computeStreakStats(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        todayEpochDay: Long
    ): StreakStats {
        if (habits.isEmpty()) return StreakStats(0, 0)

        // Find earliest start date or earliest log
        val habitMinStart = habits.minOfOrNull { it.startEpochDay } ?: todayEpochDay
        val logMinDay = logs.keys.minOfOrNull { it.second } ?: todayEpochDay
        val minDay = minOf(habitMinStart, logMinDay, todayEpochDay)

        fun isActiveDay(d: Long): Boolean {
            return habits.any { habit ->
                habit.startEpochDay <= d && habit.isCompleted(logs[Pair(habit.id, d)])
            }
        }

        // Current streak:
        // A day is active if at least 1 habit is done.
        // If today has none yet, count from yesterday.
        val todayActive = isActiveDay(todayEpochDay)
        var streak = 0
        var checkDay = if (todayActive) todayEpochDay else todayEpochDay - 1

        while (checkDay >= minDay && isActiveDay(checkDay)) {
            streak++
            checkDay--
        }

        // Best streak over full history:
        var best = 0
        var currentRun = 0
        for (d in minDay..todayEpochDay) {
            if (isActiveDay(d)) {
                currentRun++
                if (currentRun > best) {
                    best = currentRun
                }
            } else {
                currentRun = 0
            }
        }

        return StreakStats(
            currentStreak = streak,
            bestStreak = maxOf(best, streak)
        )
    }

    fun computeDailyProgress(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        epochDay: Long
    ): HabitDayProgress {
        val activeHabits = habits.filter { it.startEpochDay <= epochDay }
        if (activeHabits.isEmpty()) {
            return HabitDayProgress(0, 0, 0f, 0)
        }
        val completedCount = activeHabits.count { habit ->
            habit.isCompleted(logs[Pair(habit.id, epochDay)])
        }
        val fraction = completedCount.toFloat() / activeHabits.size.toFloat()
        val percentage = (fraction * 100f).roundToInt()
        return HabitDayProgress(
            completedCount = completedCount,
            totalCount = activeHabits.size,
            fraction = fraction,
            percentage = percentage
        )
    }

    fun computeTopHabits(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        today: LocalDate
    ): List<TopHabitItem> {
        if (habits.isEmpty()) return emptyList()

        val firstEpochDayOfMonth = today.withDayOfMonth(1).toEpochDay()
        val todayEpochDay = today.toEpochDay()

        return habits.map { habit ->
            val startDay = maxOf(firstEpochDayOfMonth, habit.startEpochDay)
            val eligibleDays = if (startDay <= todayEpochDay) {
                (todayEpochDay - startDay + 1).toInt()
            } else {
                0
            }

            val completedDays = if (eligibleDays > 0) {
                (startDay..todayEpochDay).count { d ->
                    habit.isCompleted(logs[Pair(habit.id, d)])
                }
            } else {
                0
            }

            val ratePercentage = if (eligibleDays > 0) {
                ((completedDays.toFloat() / eligibleDays.toFloat()) * 100f).roundToInt()
            } else {
                0
            }

            TopHabitItem(
                habit = habit,
                completedDays = completedDays,
                eligibleDays = eligibleDays,
                ratePercentage = ratePercentage
            )
        }
        .sortedWith(
            compareByDescending<TopHabitItem> { it.ratePercentage }
                .thenByDescending { it.completedDays }
                .thenBy { it.habit.sortOrder }
        )
        .take(3)
    }

    fun computeHabitsToWorkOn(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        today: LocalDate
    ): List<HabitToWorkOnItem> {
        if (habits.isEmpty()) return emptyList()

        val todayEpochDay = today.toEpochDay()
        val firstEpochDayOfMonth = today.withDayOfMonth(1).toEpochDay()

        // "Omit any habit created fewer than 7 days ago"
        val eligibleHabits = habits.filter { habit ->
            todayEpochDay - habit.startEpochDay >= 6
        }
        if (eligibleHabits.isEmpty()) return emptyList()

        return eligibleHabits.map { habit ->
            val startDay = maxOf(firstEpochDayOfMonth, habit.startEpochDay)
            val eligibleDaysThisMonth = if (startDay <= todayEpochDay) {
                (todayEpochDay - startDay + 1).toInt()
            } else {
                0
            }

            val completedDaysThisMonth = if (eligibleDaysThisMonth > 0) {
                (startDay..todayEpochDay).count { d ->
                    habit.isCompleted(logs[Pair(habit.id, d)])
                }
            } else {
                0
            }

            val monthlyRate = if (eligibleDaysThisMonth > 0) {
                ((completedDaysThisMonth.toFloat() / eligibleDaysThisMonth.toFloat()) * 100f).roundToInt()
            } else {
                0
            }

            // Lookback window: up to last 7 days
            val lookbackStart = maxOf(habit.startEpochDay, todayEpochDay - 6)
            val windowDays = (todayEpochDay - lookbackStart + 1).toInt()
            val missedCount = (lookbackStart..todayEpochDay).count { d ->
                !habit.isCompleted(logs[Pair(habit.id, d)])
            }

            // Days since last completed this month
            var daysSince: Int? = null
            for (d in todayEpochDay downTo startDay) {
                if (habit.isCompleted(logs[Pair(habit.id, d)])) {
                    daysSince = (todayEpochDay - d).toInt()
                    break
                }
            }

            // 7-day sparkline status ending today
            val last7 = (todayEpochDay - 6..todayEpochDay).map { d ->
                if (d < habit.startEpochDay) {
                    false
                } else {
                    habit.isCompleted(logs[Pair(habit.id, d)])
                }
            }

            HabitToWorkOnItem(
                habit = habit,
                missedCount = missedCount,
                windowDays = windowDays,
                daysSinceLastCompleted = daysSince,
                monthlyRatePercentage = monthlyRate,
                last7DayStatus = last7
            )
        }
        .sortedWith(
            compareBy<HabitToWorkOnItem> { it.monthlyRatePercentage }
                .thenByDescending { it.missedCount }
                .thenBy { it.habit.sortOrder }
        )
        .take(3)
    }

    fun computeMonthSnapshot(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        today: LocalDate
    ): MonthSnapshotStats {
        if (habits.isEmpty()) return MonthSnapshotStats(0, 0, 0, 0)

        val firstEpochDayOfMonth = today.withDayOfMonth(1).toEpochDay()
        val todayEpochDay = today.toEpochDay()

        var totalCheckIns = 0
        var totalEligible = 0
        var perfectDays = 0

        var monthBestStreak = 0
        var currentRun = 0

        for (d in firstEpochDayOfMonth..todayEpochDay) {
            val activeHabits = habits.filter { it.startEpochDay <= d }
            if (activeHabits.isNotEmpty()) {
                val done = activeHabits.count { it.isCompleted(logs[Pair(it.id, d)]) }
                totalCheckIns += done
                totalEligible += activeHabits.size
                if (done > 0) {
                    currentRun++
                    if (currentRun > monthBestStreak) monthBestStreak = currentRun
                } else {
                    currentRun = 0
                }
                if (done == activeHabits.size) {
                    perfectDays++
                }
            } else {
                currentRun = 0
            }
        }

        val rate = if (totalEligible > 0) {
            ((totalCheckIns.toFloat() / totalEligible.toFloat()) * 100f).roundToInt()
        } else {
            0
        }

        return MonthSnapshotStats(
            totalCheckIns = totalCheckIns,
            completionRatePercentage = rate,
            bestStreakThisMonth = monthBestStreak,
            perfectDays = perfectDays
        )
    }

    fun computeConsistencyStats(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        today: LocalDate
    ): Triple<Int, Int, String> {
        // Returns Triple(activeDaysThisMonth, monthlyRatePercentage, bestDayOfWeekName)
        if (habits.isEmpty()) return Triple(0, 0, "None")

        val firstEpochDayOfMonth = today.withDayOfMonth(1).toEpochDay()
        val todayEpochDay = today.toEpochDay()

        var activeDays = 0
        var totalDone = 0
        var totalEligible = 0

        for (d in firstEpochDayOfMonth..todayEpochDay) {
            val activeHabits = habits.filter { it.startEpochDay <= d }
            if (activeHabits.isNotEmpty()) {
                val done = activeHabits.count { it.isCompleted(logs[Pair(it.id, d)]) }
                if (done > 0) activeDays++
                totalDone += done
                totalEligible += activeHabits.size
            }
        }

        val rate = if (totalEligible > 0) {
            ((totalDone.toFloat() / totalEligible.toFloat()) * 100f).roundToInt()
        } else {
            0
        }

        // Best day of week over last 60 days
        val lookbackStart = maxOf(todayEpochDay - 59, habits.minOf { it.startEpochDay })
        val dowDoneCounts = mutableMapOf<DayOfWeek, Int>()
        val dowTotalCounts = mutableMapOf<DayOfWeek, Int>()

        for (d in lookbackStart..todayEpochDay) {
            val date = LocalDate.ofEpochDay(d)
            val dow = date.dayOfWeek
            val activeHabits = habits.filter { it.startEpochDay <= d }
            if (activeHabits.isNotEmpty()) {
                val done = activeHabits.count { it.isCompleted(logs[Pair(it.id, d)]) }
                dowDoneCounts[dow] = (dowDoneCounts[dow] ?: 0) + done
                dowTotalCounts[dow] = (dowTotalCounts[dow] ?: 0) + activeHabits.size
            }
        }

        val bestDow = dowTotalCounts.keys.maxByOrNull { dow ->
            val total = dowTotalCounts[dow] ?: 1
            val done = dowDoneCounts[dow] ?: 0
            done.toFloat() / total.toFloat()
        }

        val bestDayName = bestDow?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: "None"
        return Triple(activeDays, rate, bestDayName)
    }

    fun computeHeatmapData(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        today: LocalDate
    ): HeatmapData {
        // 365 days ending at today
        val startDay = today.minusDays(364)
        val todayEpochDay = today.toEpochDay()
        val startEpochDay = startDay.toEpochDay()

        // Week columns with Sunday at top
        // DayOfWeek: SUNDAY % 7 = 0, MONDAY % 7 = 1, ..., SATURDAY % 7 = 6
        val initialPad = startDay.dayOfWeek.value % 7

        val weeks = mutableListOf<HeatmapWeek>()
        val currentWeek = arrayOfNulls<HeatmapDay>(7)
        var currentSlot = initialPad
        val monthLabels = mutableListOf<Pair<Int, String>>()
        var lastMonth: YearMonth? = null

        for (d in startEpochDay..todayEpochDay) {
            val date = LocalDate.ofEpochDay(d)
            val ym = YearMonth.from(date)
            if (lastMonth != ym && date.dayOfMonth <= 7) {
                monthLabels.add(Pair(weeks.size, ym.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())))
                lastMonth = ym
            }

            val activeHabits = habits.filter { it.startEpochDay <= d }
            val completedCount = if (activeHabits.isEmpty()) {
                0
            } else {
                activeHabits.count { it.isCompleted(logs[Pair(it.id, d)]) }
            }

            val fraction = if (activeHabits.isEmpty()) 0f else completedCount.toFloat() / activeHabits.size.toFloat()
            val level = when {
                activeHabits.isEmpty() || completedCount == 0 -> 0
                fraction >= 1.0f -> 4
                fraction <= 1.0f / 3.0f -> 1
                fraction <= 2.0f / 3.0f -> 2
                else -> 3
            }

            val heatmapDay = HeatmapDay(
                epochDay = d,
                localDate = date,
                level = level,
                completedCount = completedCount,
                totalActiveHabits = activeHabits.size
            )

            currentWeek[currentSlot] = heatmapDay
            currentSlot++

            if (currentSlot == 7) {
                weeks.add(HeatmapWeek(currentWeek.toList()))
                for (i in 0..6) currentWeek[i] = null
                currentSlot = 0
            }
        }

        if (currentSlot > 0) {
            weeks.add(HeatmapWeek(currentWeek.toList()))
        }

        return HeatmapData(weeks, monthLabels)
    }

    fun computeAnalysis(
        habits: List<Habit>,
        logs: Map<Pair<Long, Long>, Double>,
        today: LocalDate
    ): List<HabitAnalysisItem> {
        if (habits.isEmpty()) {
            return listOf(
                HabitAnalysisItem(
                    title = "No habits tracked yet",
                    description = "Create habits to start collecting data and generate automated consistency insights.",
                    highlight = "Getting started"
                )
            )
        }

        val items = mutableListOf<HabitAnalysisItem>()
        val todayEpochDay = today.toEpochDay()
        val firstEpochDayOfMonth = today.withDayOfMonth(1).toEpochDay()

        // 1. Best day of week
        val dowDoneCounts = mutableMapOf<DayOfWeek, Int>()
        val dowTotalCounts = mutableMapOf<DayOfWeek, Int>()
        for (d in maxOf(todayEpochDay - 59, habits.minOf { it.startEpochDay })..todayEpochDay) {
            val date = LocalDate.ofEpochDay(d)
            val dow = date.dayOfWeek
            val activeHabits = habits.filter { it.startEpochDay <= d }
            if (activeHabits.isNotEmpty()) {
                val done = activeHabits.count { it.isCompleted(logs[Pair(it.id, d)]) }
                dowDoneCounts[dow] = (dowDoneCounts[dow] ?: 0) + done
                dowTotalCounts[dow] = (dowTotalCounts[dow] ?: 0) + activeHabits.size
            }
        }

        val bestDow = dowTotalCounts.keys.maxByOrNull { dow ->
            val total = dowTotalCounts[dow] ?: 1
            val done = dowDoneCounts[dow] ?: 0
            done.toFloat() / total.toFloat()
        }

        if (bestDow != null) {
            val total = dowTotalCounts[bestDow] ?: 1
            val done = dowDoneCounts[bestDow] ?: 0
            val pct = ((done.toFloat() / total.toFloat()) * 100f).roundToInt()
            val dayName = bestDow.getDisplayName(TextStyle.FULL, Locale.getDefault())
            items.add(
                HabitAnalysisItem(
                    title = "Best day of week: $dayName",
                    description = "You achieve your highest consistency on ${dayName}s with an average completion rate of $pct%.",
                    highlight = "$pct%"
                )
            )
        }

        // 2. Strongest habit this month
        val topHabits = computeTopHabits(habits, logs, today)
        if (topHabits.isNotEmpty()) {
            val strongest = topHabits.first()
            items.add(
                HabitAnalysisItem(
                    title = "Strongest habit: ${strongest.habit.name}",
                    description = "${strongest.habit.name} leads with ${strongest.completedDays} of ${strongest.eligibleDays} days completed this month (${strongest.ratePercentage}%).",
                    highlight = "${strongest.ratePercentage}%"
                )
            )
        }

        // 3. Most missed habit (or habit to focus on)
        val lowHabits = computeHabitsToWorkOn(habits, logs, today)
        if (lowHabits.isNotEmpty()) {
            val mostMissed = lowHabits.first()
            val missedDesc = if (mostMissed.missedCount == 0) {
                "Fully on track over the last ${mostMissed.windowDays} days."
            } else {
                "Missed ${mostMissed.missedCount} of the last ${mostMissed.windowDays} days. Consider adjusting target or timing."
            }
            items.add(
                HabitAnalysisItem(
                    title = "Needs focus: ${mostMissed.habit.name}",
                    description = missedDesc,
                    highlight = "Missed ${mostMissed.missedCount}/${mostMissed.windowDays}d"
                )
            )
        } else if (habits.isNotEmpty()) {
            items.add(
                HabitAnalysisItem(
                    title = "All habits on track",
                    description = "All habits created more than 7 days ago maintain strong consistency rates.",
                    highlight = "Great momentum"
                )
            )
        }

        return items
    }
}
