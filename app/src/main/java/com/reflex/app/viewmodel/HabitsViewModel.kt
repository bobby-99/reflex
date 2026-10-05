package com.reflex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.Habit
import com.reflex.app.data.HabitKind
import com.reflex.app.data.ReflexRepository
import com.reflex.app.util.HabitAnalysisItem
import com.reflex.app.util.HabitCalculations
import com.reflex.app.util.HabitDayProgress
import com.reflex.app.util.HabitToWorkOnItem
import com.reflex.app.util.HeatmapData
import com.reflex.app.util.MonthSnapshotStats
import com.reflex.app.util.StreakStats
import com.reflex.app.util.TopHabitItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class HabitsViewMode {
    MAIN,
    ANALYTICS
}

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val logs: Map<Pair<Long, Long>, Double> = emptyMap(),
    val today: LocalDate = LocalDate.now(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val viewMode: HabitsViewMode = HabitsViewMode.MAIN,
    val streakStats: StreakStats = StreakStats(0, 0),
    val todayProgress: HabitDayProgress = HabitDayProgress(0, 0, 0f, 0),
    val consistencyStats: Triple<Int, Int, String> = Triple(0, 0, "None"),
    val topHabits: List<TopHabitItem> = emptyList(),
    val habitsToWorkOn: List<HabitToWorkOnItem> = emptyList(),
    val monthSnapshot: MonthSnapshotStats = MonthSnapshotStats(0, 0, 0, 0),
    val heatmapData: HeatmapData = HeatmapData(emptyList(), emptyList()),
    val analysisItems: List<HabitAnalysisItem> = emptyList()
)

class HabitsViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    private val _viewMode = MutableStateFlow(HabitsViewMode.MAIN)
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    private val _today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HabitsUiState> = combine(
        repository.getAllHabits(),
        repository.getAllHabitLogs(),
        _viewMode,
        _selectedMonth,
        _today
    ) { habitsList, logsList, viewMode, selectedMonth, todayDate ->
        val logsMap = logsList.associate { Pair(it.habitId, it.epochDay) to it.value }

        val streakStats = HabitCalculations.computeStreakStats(habitsList, logsMap, todayDate.toEpochDay())
        val todayProgress = HabitCalculations.computeDailyProgress(habitsList, logsMap, todayDate.toEpochDay())
        val consistencyStats = HabitCalculations.computeConsistencyStats(habitsList, logsMap, todayDate)
        val topHabits = HabitCalculations.computeTopHabits(habitsList, logsMap, todayDate)
        val habitsToWorkOn = HabitCalculations.computeHabitsToWorkOn(habitsList, logsMap, todayDate)
        val monthSnapshot = HabitCalculations.computeMonthSnapshot(habitsList, logsMap, todayDate)
        val heatmapData = HabitCalculations.computeHeatmapData(habitsList, logsMap, todayDate)
        val analysisItems = HabitCalculations.computeAnalysis(habitsList, logsMap, todayDate)

        HabitsUiState(
            habits = habitsList,
            logs = logsMap,
            today = todayDate,
            selectedMonth = selectedMonth,
            viewMode = viewMode,
            streakStats = streakStats,
            todayProgress = todayProgress,
            consistencyStats = consistencyStats,
            topHabits = topHabits,
            habitsToWorkOn = habitsToWorkOn,
            monthSnapshot = monthSnapshot,
            heatmapData = heatmapData,
            analysisItems = analysisItems
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun checkDateRefresh() {
        val current = LocalDate.now()
        if (_today.value != current) {
            _today.value = current
        }
    }

    fun setViewMode(mode: HabitsViewMode) {
        _viewMode.value = mode
    }

    fun setSelectedMonth(month: YearMonth) {
        _selectedMonth.value = month
    }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun prevMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun resetToCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    fun toggleCheckOff(habit: Habit, epochDay: Long) {
        viewModelScope.launch {
            val currentVal = uiState.value.logs[Pair(habit.id, epochDay)]
            val newVal = if ((currentVal ?: 0.0) >= 1.0) 0.0 else 1.0
            repository.setHabitLogValue(habit.id, epochDay, newVal)
        }
    }

    fun stepHabit(habit: Habit, epochDay: Long, direction: Int) {
        viewModelScope.launch {
            val currentVal = uiState.value.logs[Pair(habit.id, epochDay)]
            val nextVal = habit.nextStepValue(currentVal, direction)
            repository.setHabitLogValue(habit.id, epochDay, nextVal)
        }
    }

    fun setHabitValue(habitId: Long, epochDay: Long, value: Double) {
        viewModelScope.launch {
            repository.setHabitLogValue(habitId, epochDay, value)
        }
    }

    fun saveHabit(
        name: String,
        kind: HabitKind,
        target: Double = 1.0,
        unit: String = "",
        step: Double = 1.0,
        frequencyType: String = "DAILY",
        frequencyDays: String = "",
        frequencyTargetPerWeek: Int = 0,
        reminderEnabled: Boolean = false,
        reminderTimes: String = "",
        startEpochDay: Long? = null,
        endEpochDay: Long? = null,
        colorHex: String? = null,
        iconKey: String? = null,
        notes: String? = null,
        existingId: Long = 0L,
        context: android.content.Context? = null
    ) {
        viewModelScope.launch {
            val habits = uiState.value.habits
            val sortOrder = if (existingId == 0L) {
                (habits.maxOfOrNull { it.sortOrder } ?: 0) + 1
            } else {
                habits.find { it.id == existingId }?.sortOrder ?: 0
            }

            val finalStartEpochDay = startEpochDay ?: if (existingId == 0L) {
                LocalDate.now().toEpochDay()
            } else {
                habits.find { it.id == existingId }?.startEpochDay ?: LocalDate.now().toEpochDay()
            }

            val habit = Habit(
                id = existingId,
                name = name.trim(),
                type = kind.name,
                target = if (kind == HabitKind.CHECK_OFF) 1.0 else target,
                unit = if (kind == HabitKind.CHECK_OFF) "" else unit.trim(),
                step = if (kind == HabitKind.CHECK_OFF) 1.0 else (if (step <= 0.0) 1.0 else step),
                startEpochDay = finalStartEpochDay,
                sortOrder = sortOrder,
                frequencyType = frequencyType,
                frequencyDays = frequencyDays,
                frequencyTargetPerWeek = frequencyTargetPerWeek,
                reminderEnabled = reminderEnabled,
                reminderTimes = reminderTimes,
                endEpochDay = endEpochDay,
                colorHex = colorHex,
                iconKey = iconKey,
                notes = if (notes.isNullOrBlank()) null else notes.trim()
            )
            repository.saveHabit(habit, context)
        }
    }

    fun reorderHabits(reorderedList: List<Habit>) {
        viewModelScope.launch {
            val updated = reorderedList.mapIndexed { index, habit ->
                habit.copy(sortOrder = index)
            }
            repository.updateHabits(updated)
        }
    }

    fun deleteHabit(habit: Habit, context: android.content.Context? = null) {
        viewModelScope.launch {
            repository.deleteHabit(habit, context)
        }
    }

    class Factory(private val repository: ReflexRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HabitsViewModel::class.java)) {
                return HabitsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
