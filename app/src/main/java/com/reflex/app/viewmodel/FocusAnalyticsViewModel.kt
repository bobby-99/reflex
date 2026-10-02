package com.reflex.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.BlockedAppEvent
import com.reflex.app.data.BlockingMode
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSession
import com.reflex.app.data.FocusSettings
import com.reflex.app.data.FocusTag
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.Task
import com.reflex.app.util.CalendarPreferenceRepository
import com.reflex.app.util.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class AnalyticsRange {
    WEEK,
    MONTH,
    YEAR
}

data class AnalyticsBarItem(
    val index: Int,
    val valueMinutes: Int,
    val label: String,          // Under bar: S, M, T... or day number, or month initial
    val headlineValue: String,  // E.g. "1h 20m"
    val headlineDate: String,   // E.g. "Wed, Oct 1" or "October 2026"
    val isMetGoal: Boolean
)

data class HeatmapDay(
    val index: Int,
    val date: LocalDate,
    val minutes: Int,
    val level: Int,             // 0..4
    val formattedDuration: String,
    val formattedDate: String
)

data class TimeOfDayItem(
    val title: String,          // "Morning", "Afternoon", "Evening", "Night"
    val timeRange: String,      // "5 am to 12 pm", "12 to 5 pm", "5 to 10 pm", "10 pm to 5 am"
    val percent: Int,           // 0..100
    val totalMinutes: Int
)

data class SessionTypeItem(
    val mode: FocusMode,
    val label: String,          // "Pomodoro", "Timed flow", "Open flow"
    val percent: Int,           // 0..100
    val totalMinutes: Int,
    val formattedTime: String
)

data class TopTaskItem(
    val title: String,
    val totalMinutes: Int,
    val formattedTime: String,
    val percentOfMax: Float      // 0f..1f relative to highest
)

data class TagStatItem(
    val tagId: Long?,
    val name: String,
    val totalMinutes: Int,
    val formattedTime: String,
    val percentOfMax: Float
)

data class DistractionsBlockedState(
    val attempts: Int = 0,
    val keptMinutes: Int = 0,
    val keptFormatted: String = "0m",
    val mostBlockedApp: String? = null,
    val mostBlockedCount: Int = 0,
    val isVisible: Boolean = false
)

data class CombinedUserSelections(
    val range: AnalyticsRange,
    val selectedTagId: Long?,
    val selectedBarIndex: Int?,
    val selectedHeatmapIndex: Int?,
    val goalMinutes: Int
)

data class CombinedDataSources(
    val sessions: List<FocusSession>,
    val tags: List<FocusTag>,
    val tasks: List<Task>,
    val blockedEvents: List<BlockedAppEvent>,
    val focusSettings: FocusSettings?
)

data class FocusAnalyticsUiState(
    val range: AnalyticsRange = AnalyticsRange.WEEK,
    val tags: List<FocusTag> = emptyList(),
    val selectedTagId: Long? = null,
    val goalMinutes: Int = 120,
    val todayFocusMinutes: Int = 0,
    val todayGoalPercent: Int = 0,
    val todayGoalFormatted: String = "0m of 2h",

    // Stat tiles (row 1)
    val focusTimeFormatted: String = "0m",
    val rangeSubtitle: String = "7 days",
    val sessionsCount: Int = 0,
    val averageFormatted: String = "0m",

    // Bar chart
    val bars: List<AnalyticsBarItem> = emptyList(),
    val selectedBarIndex: Int = 0,
    val selectedBarValue: String = "0m",
    val selectedBarLabel: String = "",
    val maxBarMinutes: Int = 120,
    val isBarChartEmpty: Boolean = true,

    // Stat tiles (row 2)
    val streakDays: Int = 0,
    val bestDayFormatted: String = "0m",
    val finishedPercentStr: String = "-",

    // Heatmap (last 365 days)
    val heatmapDays: List<HeatmapDay> = emptyList(),
    val heatmapStartDayOfWeekOffset: Int = 0,
    val selectedHeatmapDay: HeatmapDay? = null,

    // Time of day
    val timeOfDayItems: List<TimeOfDayItem> = emptyList(),
    val bestWindowTip: String? = null,

    // Session types
    val sessionTypes: List<SessionTypeItem> = emptyList(),

    // Top tasks
    val topTasks: List<TopTaskItem> = emptyList(),

    // By tag
    val tagStats: List<TagStatItem> = emptyList(),
    val showTagCard: Boolean = false,

    // Distractions blocked
    val distractionsBlocked: DistractionsBlockedState = DistractionsBlockedState(),

    val isLoading: Boolean = false
)

class FocusAnalyticsViewModel(
    application: Application,
    private val repository: ReflexRepository
) : AndroidViewModel(application) {

    private val _range = MutableStateFlow(AnalyticsRange.WEEK)
    private val _selectedTagId = MutableStateFlow<Long?>(null)
    private val _selectedBarIndex = MutableStateFlow<Int?>(null)
    private val _selectedHeatmapIndex = MutableStateFlow<Int?>(null)
    private val _goalMinutes = MutableStateFlow(SettingsRepository.getFocusGoalMinutes(application))

    // Query last 366 days of sessions up to today end-of-day
    private val todayLocalDate = LocalDate.now()
    private val zoneId = ZoneId.systemDefault()
    private val startOf365DaysMs = todayLocalDate.minusDays(365).atStartOfDay(zoneId).toInstant().toEpochMilli()
    private val endOfTodayMs = todayLocalDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1

    private val _sessionsFlow = repository.getFocusSessionsInDateRangeWithTag(startOf365DaysMs, endOfTodayMs, null)
    private val _tagsFlow = repository.getAllFocusTags()
    private val _tasksFlow = repository.getAllTasks()
    private val _sevenDaysAgoMs = System.currentTimeMillis() - 7 * 24 * 3600 * 1000L
    private val _blockedEventsFlow = repository.getBlockedAppEventsSince(_sevenDaysAgoMs)
    private val _settingsFlow = repository.getFocusSettings()

    val uiState: StateFlow<FocusAnalyticsUiState> = combine(
        combine(_range, _selectedTagId, _selectedBarIndex, _selectedHeatmapIndex, _goalMinutes) { r, tagId, barIdx, hmIdx, goal ->
            CombinedUserSelections(r, tagId, barIdx, hmIdx, goal)
        },
        combine(_sessionsFlow, _tagsFlow, _tasksFlow, _blockedEventsFlow, _settingsFlow) { sessions, tags, tasks, blocked, settings ->
            CombinedDataSources(sessions, tags, tasks, blocked, settings)
        }
    ) { sel, data ->
        computeAnalytics(sel, data, getApplication())
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FocusAnalyticsUiState(isLoading = true)
        )

    fun setRange(range: AnalyticsRange) {
        _range.value = range
        _selectedBarIndex.value = null // reset selection to latest
    }

    fun selectTag(tagId: Long?) {
        _selectedTagId.value = tagId
        _selectedBarIndex.value = null
    }

    fun selectBar(index: Int) {
        _selectedBarIndex.value = index
    }

    fun selectHeatmapDay(index: Int) {
        _selectedHeatmapIndex.value = index
    }

    fun stepGoal(deltaSteps: Int) {
        val next = SettingsRepository.stepFocusGoal(getApplication(), deltaSteps)
        _goalMinutes.value = next
    }

    fun setGoalMinutes(minutes: Int) {
        SettingsRepository.setFocusGoalMinutes(getApplication(), minutes)
        _goalMinutes.value = SettingsRepository.getFocusGoalMinutes(getApplication())
    }

    fun updateFocusSettings(updated: FocusSettings) {
        viewModelScope.launch {
            repository.saveFocusSettings(updated)
        }
    }

    companion object {
        fun formatMinutes(mins: Int): String {
            if (mins <= 0) return "0m"
            val h = mins / 60
            val m = mins % 60
            return when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }

        fun formatDurationSeconds(sec: Long): String {
            val mins = (sec / 60).toInt()
            return formatMinutes(mins)
        }

        fun formatBestWindow(startHour: Int): String {
            val endHour = (startHour + 2) % 24
            val startAmpm = if (startHour < 12) "am" else "pm"
            val endAmpm = if (endHour < 12) "am" else "pm"
            val start12 = if (startHour % 12 == 0) 12 else startHour % 12
            val end12 = if (endHour % 12 == 0) 12 else endHour % 12
            return if (startAmpm == endAmpm) {
                "$start12 to $end12 $endAmpm"
            } else {
                "$start12 $startAmpm to $end12 $endAmpm"
            }
        }

        fun resolveAppName(context: Context?, packageName: String): String {
            if (context != null) {
                try {
                    val pm = context.packageManager
                    val info = pm.getApplicationInfo(packageName, 0)
                    return pm.getApplicationLabel(info).toString()
                } catch (_: Exception) {}
            }
            return packageName.substringAfterLast('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

        fun computeAnalytics(
            sel: CombinedUserSelections,
            data: CombinedDataSources,
            context: Context? = null
        ): FocusAnalyticsUiState {
            val today = LocalDate.now()
            val zoneId = ZoneId.systemDefault()
            val goalMinutes = sel.goalMinutes

            // Tag filtering: applies to every calculation, chart and heatmap
            val tagFilteredSessions = if (sel.selectedTagId != null) {
                data.sessions.filter { it.tagId == sel.selectedTagId }
            } else {
                data.sessions
            }

            // Counted sessions: actualDurationSeconds >= 60
            val countedSessions = tagFilteredSessions.filter { it.actualDurationSeconds >= 60 }

            // Group counted sessions by LocalDate
            val sessionsByDate = countedSessions.groupBy { session ->
                Instant.ofEpochMilli(session.startTime).atZone(zoneId).toLocalDate()
            }

            // Today's focus
            val todaySessions = sessionsByDate[today] ?: emptyList()
            val todayFocusSeconds = todaySessions.sumOf { it.actualDurationSeconds.toLong() }
            val todayFocusMinutes = (todayFocusSeconds / 60).toInt()
            val todayGoalPercent = if (goalMinutes > 0) {
                ((todayFocusMinutes * 100) / goalMinutes).coerceIn(0, 100)
            } else 0
            val todayGoalFormatted = "${formatMinutes(todayFocusMinutes)} of ${formatMinutes(goalMinutes)}"

            // Range-specific sessions
            val rangeSessions = when (sel.range) {
                AnalyticsRange.WEEK -> {
                    val start = today.minusDays(6)
                    countedSessions.filter {
                        val d = Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalDate()
                        !d.isBefore(start) && !d.isAfter(today)
                    }
                }
                AnalyticsRange.MONTH -> {
                    val start = today.minusDays(29)
                    countedSessions.filter {
                        val d = Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalDate()
                        !d.isBefore(start) && !d.isAfter(today)
                    }
                }
                AnalyticsRange.YEAR -> {
                    val currentYm = YearMonth.from(today)
                    val startYm = currentYm.minusMonths(7)
                    countedSessions.filter {
                        val d = Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalDate()
                        val ym = YearMonth.from(d)
                        !ym.isBefore(startYm) && !ym.isAfter(currentYm)
                    }
                }
            }

            // Stat tiles (row 1)
            val totalRangeSeconds = rangeSessions.sumOf { it.actualDurationSeconds.toLong() }
            val totalRangeMinutes = (totalRangeSeconds / 60).toInt()
            val focusTimeFormatted = formatMinutes(totalRangeMinutes)
            val rangeSubtitle = when (sel.range) {
                AnalyticsRange.WEEK -> "7 days"
                AnalyticsRange.MONTH -> "30 days"
                AnalyticsRange.YEAR -> "8 months"
            }
            val sessionsCount = rangeSessions.size
            val averageMinutes = if (sessionsCount > 0) totalRangeMinutes / sessionsCount else 0
            val averageFormatted = formatMinutes(averageMinutes)

            // Stat tiles (row 2)
            // Streak: consecutive calendar days ending today or yesterday with >= 1 counted session
            val activeDates = sessionsByDate.keys
            var streak = 0
            var checkDate: LocalDate? = when {
                activeDates.contains(today) -> today
                activeDates.contains(today.minusDays(1)) -> today.minusDays(1)
                else -> null
            }
            while (checkDate != null && activeDates.contains(checkDate)) {
                streak++
                checkDate = checkDate.minusDays(1)
            }

            // Best day: max daily focus time in range (for Year: across all 365 days)
            val bestDaySeconds = when (sel.range) {
                AnalyticsRange.WEEK -> {
                    (0..6).maxOfOrNull { k ->
                        val d = today.minusDays(6L - k)
                        (sessionsByDate[d] ?: emptyList()).sumOf { it.actualDurationSeconds.toLong() }
                    } ?: 0L
                }
                AnalyticsRange.MONTH -> {
                    (0..29).maxOfOrNull { k ->
                        val d = today.minusDays(29L - k)
                        (sessionsByDate[d] ?: emptyList()).sumOf { it.actualDurationSeconds.toLong() }
                    } ?: 0L
                }
                AnalyticsRange.YEAR -> {
                    (0..364).maxOfOrNull { k ->
                        val d = today.minusDays(364L - k)
                        (sessionsByDate[d] ?: emptyList()).sumOf { it.actualDurationSeconds.toLong() }
                    } ?: 0L
                }
            }
            val bestDayFormatted = formatMinutes((bestDaySeconds / 60).toInt())

            // Finished %
            val completedCount = rangeSessions.count { it.completed }
            val finishedPercentStr = if (sessionsCount > 0) {
                "${(completedCount * 100) / sessionsCount}%"
            } else "-"

            // Bar chart bars
            val bars = when (sel.range) {
                AnalyticsRange.WEEK -> {
                    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
                    (0..6).map { k ->
                        val date = today.minusDays(6L - k)
                        val dayMins = ((sessionsByDate[date] ?: emptyList()).sumOf { it.actualDurationSeconds.toLong() } / 60).toInt()
                        val dowLabel = when (date.dayOfWeek) {
                            DayOfWeek.SUNDAY -> "S"
                            DayOfWeek.MONDAY -> "M"
                            DayOfWeek.TUESDAY -> "T"
                            DayOfWeek.WEDNESDAY -> "W"
                            DayOfWeek.THURSDAY -> "T"
                            DayOfWeek.FRIDAY -> "F"
                            DayOfWeek.SATURDAY -> "S"
                            null -> ""
                        }
                        AnalyticsBarItem(
                            index = k,
                            valueMinutes = dayMins,
                            label = dowLabel,
                            headlineValue = formatMinutes(dayMins),
                            headlineDate = date.format(dateFormatter),
                            isMetGoal = dayMins >= goalMinutes
                        )
                    }
                }
                AnalyticsRange.MONTH -> {
                    val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
                    (0..29).map { k ->
                        val date = today.minusDays(29L - k)
                        val dayMins = ((sessionsByDate[date] ?: emptyList()).sumOf { it.actualDurationSeconds.toLong() } / 60).toInt()
                        val label = if ((29 - k) % 5 == 0) "${date.dayOfMonth}" else ""
                        AnalyticsBarItem(
                            index = k,
                            valueMinutes = dayMins,
                            label = label,
                            headlineValue = formatMinutes(dayMins),
                            headlineDate = date.format(dateFormatter),
                            isMetGoal = dayMins >= goalMinutes
                        )
                    }
                }
                AnalyticsRange.YEAR -> {
                    val currentYm = YearMonth.from(today)
                    (0..7).map { b ->
                        val ym = currentYm.minusMonths(7L - b)
                        val monthSessions = countedSessions.filter {
                            val d = Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalDate()
                            YearMonth.from(d) == ym
                        }
                        val monthMins = (monthSessions.sumOf { it.actualDurationSeconds.toLong() } / 60).toInt()
                        val monthName = ym.month.name.lowercase().replaceFirstChar { it.titlecase(Locale.ENGLISH) }
                        AnalyticsBarItem(
                            index = b,
                            valueMinutes = monthMins,
                            label = monthName.take(3),
                            headlineValue = formatMinutes(monthMins),
                            headlineDate = "$monthName ${ym.year}",
                            isMetGoal = false // Year range does not show goal line
                        )
                    }
                }
            }

            val maxBarMinutes = maxOf(
                bars.maxOfOrNull { it.valueMinutes } ?: 0,
                if (sel.range != AnalyticsRange.YEAR) goalMinutes else 0
            ).coerceAtLeast(60)

            val isBarChartEmpty = bars.all { it.valueMinutes == 0 }

            val effectiveBarIndex = (sel.selectedBarIndex ?: (bars.size - 1)).coerceIn(0, (bars.size - 1).coerceAtLeast(0))
            val selectedBar = bars.getOrNull(effectiveBarIndex) ?: bars.lastOrNull()
            val selectedBarValue = selectedBar?.headlineValue ?: "0m"
            val selectedBarLabel = selectedBar?.headlineDate ?: ""

            // Consistency heatmap (always 365 days ending today)
            val heatmapDateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH)
            val heatmapDays = (0..364).map { i ->
                val date = today.minusDays(364L - i)
                val dayMins = ((sessionsByDate[date] ?: emptyList()).sumOf { it.actualDurationSeconds.toLong() } / 60).toInt()
                val level = when {
                    dayMins == 0 -> 0
                    dayMins < 30 -> 1
                    dayMins < 60 -> 2
                    dayMins < 120 -> 3
                    else -> 4
                }
                HeatmapDay(
                    index = i,
                    date = date,
                    minutes = dayMins,
                    level = level,
                    formattedDuration = formatMinutes(dayMins),
                    formattedDate = date.format(heatmapDateFormatter)
                )
            }

            val firstDayOfHeatmap = today.minusDays(364)
            val firstDayDow = firstDayOfHeatmap.dayOfWeek
            val configuredFirstDay = CalendarPreferenceRepository.preferences.value.firstDayOfWeek
            val heatmapOffset = if (configuredFirstDay == DayOfWeek.MONDAY) {
                (firstDayDow.value - 1) % 7
            } else {
                firstDayDow.value % 7
            }

            val selectedHeatmapDay = sel.selectedHeatmapIndex?.let { idx ->
                heatmapDays.getOrNull(idx)
            }

            // Time of day
            var morningSec = 0L
            var afternoonSec = 0L
            var eveningSec = 0L
            var nightSec = 0L

            rangeSessions.forEach { s ->
                val localTime = Instant.ofEpochMilli(s.startTime).atZone(zoneId).toLocalTime()
                val hour = localTime.hour
                val sec = s.actualDurationSeconds.toLong()
                when (hour) {
                    in 5..11 -> morningSec += sec
                    in 12..16 -> afternoonSec += sec
                    in 17..21 -> eveningSec += sec
                    else -> nightSec += sec
                }
            }

            val totalTodSec = (morningSec + afternoonSec + eveningSec + nightSec).coerceAtLeast(1L)
            val timeOfDayItems = listOf(
                TimeOfDayItem(
                    title = "Morning",
                    timeRange = "5 am to 12 pm",
                    percent = ((morningSec * 100) / totalTodSec).toInt(),
                    totalMinutes = (morningSec / 60).toInt()
                ),
                TimeOfDayItem(
                    title = "Afternoon",
                    timeRange = "12 to 5 pm",
                    percent = ((afternoonSec * 100) / totalTodSec).toInt(),
                    totalMinutes = (afternoonSec / 60).toInt()
                ),
                TimeOfDayItem(
                    title = "Evening",
                    timeRange = "5 to 10 pm",
                    percent = ((eveningSec * 100) / totalTodSec).toInt(),
                    totalMinutes = (eveningSec / 60).toInt()
                ),
                TimeOfDayItem(
                    title = "Night",
                    timeRange = "10 pm to 5 am",
                    percent = ((nightSec * 100) / totalTodSec).toInt(),
                    totalMinutes = (nightSec / 60).toInt()
                )
            )

            // Best 2-hour window tip (only if >= 5 sessions in range)
            val bestWindowTip = if (sessionsCount >= 5) {
                var bestHour = -1
                var bestWindowSec = 0
                for (startHour in 0..23) {
                    val windowSec = rangeSessions.filter { s ->
                        val h = Instant.ofEpochMilli(s.startTime).atZone(zoneId).toLocalTime().hour
                        h == startHour || h == (startHour + 1) % 24
                    }.sumOf { it.actualDurationSeconds }
                    if (windowSec > bestWindowSec) {
                        bestWindowSec = windowSec
                        bestHour = startHour
                    } else if (windowSec == bestWindowSec && windowSec > 0) {
                        val prevStartCount = rangeSessions.count {
                            Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalTime().hour == bestHour
                        }
                        val currStartCount = rangeSessions.count {
                            Instant.ofEpochMilli(it.startTime).atZone(zoneId).toLocalTime().hour == startHour
                        }
                        if (currStartCount > prevStartCount) {
                            bestHour = startHour
                        }
                    }
                }
                if (bestHour >= 0 && bestWindowSec > 0) {
                    "Best window: ${formatBestWindow(bestHour)}"
                } else null
            } else null

            // Session types
            var pomSec = 0L
            var timedSec = 0L
            var openSec = 0L
            rangeSessions.forEach { s ->
                when (s.mode) {
                    FocusMode.CLASSIC_POMODORO -> pomSec += s.actualDurationSeconds
                    FocusMode.FLOW_TIMED -> timedSec += s.actualDurationSeconds
                    FocusMode.FLOW_OPEN -> openSec += s.actualDurationSeconds
                }
            }
            val totalModeSec = (pomSec + timedSec + openSec).coerceAtLeast(1L)
            val sessionTypes = listOf(
                SessionTypeItem(
                    mode = FocusMode.CLASSIC_POMODORO,
                    label = "Pomodoro",
                    percent = ((pomSec * 100) / totalModeSec).toInt(),
                    totalMinutes = (pomSec / 60).toInt(),
                    formattedTime = formatMinutes((pomSec / 60).toInt())
                ),
                SessionTypeItem(
                    mode = FocusMode.FLOW_TIMED,
                    label = "Timed flow",
                    percent = ((timedSec * 100) / totalModeSec).toInt(),
                    totalMinutes = (timedSec / 60).toInt(),
                    formattedTime = formatMinutes((timedSec / 60).toInt())
                ),
                SessionTypeItem(
                    mode = FocusMode.FLOW_OPEN,
                    label = "Open flow",
                    percent = ((openSec * 100) / totalModeSec).toInt(),
                    totalMinutes = (openSec / 60).toInt(),
                    formattedTime = formatMinutes((openSec / 60).toInt())
                )
            )

            // Top tasks (grouped by sessionTitle)
            val taskMinutesMap = mutableMapOf<String, Int>()
            rangeSessions.forEach { s ->
                val title = if (!s.sessionTitle.isNullOrBlank()) s.sessionTitle else "No linked task"
                val current = taskMinutesMap.getOrDefault(title, 0)
                taskMinutesMap[title] = current + (s.actualDurationSeconds / 60)
            }

            // Top 3 tasks + "No linked task"
            val sortedTasks = taskMinutesMap.entries.sortedByDescending { it.value }
            val top3 = sortedTasks.filter { it.key != "No linked task" }.take(3)
            val noLinked = sortedTasks.firstOrNull { it.key == "No linked task" }
            val combinedTaskList = (top3 + listOfNotNull(noLinked)).distinctBy { it.key }
            val maxTaskMins = combinedTaskList.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1

            val topTasks = combinedTaskList.map { (title, mins) ->
                TopTaskItem(
                    title = title,
                    totalMinutes = mins,
                    formattedTime = formatMinutes(mins),
                    percentOfMax = (mins.toFloat() / maxTaskMins.toFloat()).coerceIn(0f, 1f)
                )
            }

            // By tag card
            val showTagCard = data.tags.isNotEmpty()
            val tagStats = if (showTagCard) {
                val tagStatsMap = mutableMapOf<Long?, Int>()
                rangeSessions.forEach { s ->
                    val tid = s.tagId
                    val cur = tagStatsMap.getOrDefault(tid, 0)
                    tagStatsMap[tid] = cur + (s.actualDurationSeconds / 60)
                }
                val maxTagMins = (data.tags.map { tagStatsMap.getOrDefault(it.id, 0) } + listOf(tagStatsMap.getOrDefault(null, 0)))
                    .maxOrNull()?.coerceAtLeast(1) ?: 1

                val items = data.tags.map { tag ->
                    val mins = tagStatsMap.getOrDefault(tag.id, 0)
                    TagStatItem(
                        tagId = tag.id,
                        name = tag.name,
                        totalMinutes = mins,
                        formattedTime = formatMinutes(mins),
                        percentOfMax = (mins.toFloat() / maxTagMins.toFloat()).coerceIn(0f, 1f)
                    )
                }.sortedByDescending { it.totalMinutes }

                val untaggedMins = tagStatsMap.getOrDefault(null, 0)
                val untaggedItem = TagStatItem(
                    tagId = null,
                    name = "Untagged",
                    totalMinutes = untaggedMins,
                    formattedTime = formatMinutes(untaggedMins),
                    percentOfMax = (untaggedMins.toFloat() / maxTagMins.toFloat()).coerceIn(0f, 1f)
                )
                items + listOf(untaggedItem)
            } else {
                emptyList()
            }

            // Distractions blocked card
            val blockedAttempts = data.blockedEvents.size
            val keptMinutes = blockedAttempts * 2
            val keptFormatted = formatMinutes(keptMinutes)
            val mostBlocked = data.blockedEvents.groupBy { it.packageName }
                .maxByOrNull { it.value.size }
            val mostBlockedApp = mostBlocked?.let { resolveAppName(context, it.key) }
            val mostBlockedCount = mostBlocked?.value?.size ?: 0
            val isBlockingEnabled = data.focusSettings?.blockingMode != null && data.focusSettings.blockingMode != BlockingMode.OFF
            val showBlockedCard = isBlockingEnabled || blockedAttempts > 0

            val distractionsBlocked = DistractionsBlockedState(
                attempts = blockedAttempts,
                keptMinutes = keptMinutes,
                keptFormatted = keptFormatted,
                mostBlockedApp = mostBlockedApp,
                mostBlockedCount = mostBlockedCount,
                isVisible = showBlockedCard
            )

            return FocusAnalyticsUiState(
                range = sel.range,
                tags = data.tags,
                selectedTagId = sel.selectedTagId,
                goalMinutes = goalMinutes,
                todayFocusMinutes = todayFocusMinutes,
                todayGoalPercent = todayGoalPercent,
                todayGoalFormatted = todayGoalFormatted,
                focusTimeFormatted = focusTimeFormatted,
                rangeSubtitle = rangeSubtitle,
                sessionsCount = sessionsCount,
                averageFormatted = averageFormatted,
                bars = bars,
                selectedBarIndex = effectiveBarIndex,
                selectedBarValue = selectedBarValue,
                selectedBarLabel = selectedBarLabel,
                maxBarMinutes = maxBarMinutes,
                isBarChartEmpty = isBarChartEmpty,
                streakDays = streak,
                bestDayFormatted = bestDayFormatted,
                finishedPercentStr = finishedPercentStr,
                heatmapDays = heatmapDays,
                heatmapStartDayOfWeekOffset = heatmapOffset,
                selectedHeatmapDay = selectedHeatmapDay,
                timeOfDayItems = timeOfDayItems,
                bestWindowTip = bestWindowTip,
                sessionTypes = sessionTypes,
                topTasks = topTasks,
                tagStats = tagStats,
                showTagCard = showTagCard,
                distractionsBlocked = distractionsBlocked,
                isLoading = false
            )
        }
    }

    class Factory(
        private val application: Application,
        private val repository: ReflexRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusAnalyticsViewModel(application, repository) as T
        }
    }
}
