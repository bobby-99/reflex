package com.reflex.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.Routine
import com.reflex.app.data.Task
import com.reflex.app.util.AgendaCalculationResult
import com.reflex.app.util.AgendaDaySection
import com.reflex.app.util.AgendaItem
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.CalendarCalculations
import com.reflex.app.util.CalendarChipFilter
import com.reflex.app.util.CalendarItemType
import com.reflex.app.util.CalendarPreferenceRepository
import com.reflex.app.util.CalendarPreferences
import com.reflex.app.util.CalendarProviderHelper
import com.reflex.app.util.DeviceCalendarEvent
import com.reflex.app.util.GridDay
import com.reflex.app.util.TaskParser
import com.reflex.app.util.TimeDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

sealed class CalendarItem {
    abstract val sortTimeMinutes: Int

    data class TaskItem(val task: Task) : CalendarItem() {
        override val sortTimeMinutes: Int
            get() {
                val due = task.dueTime ?: return -1
                val localTime = Instant.ofEpochMilli(due).atZone(ZoneId.systemDefault()).toLocalTime()
                return localTime.hour * 60 + localTime.minute
            }
    }

    data class RoutineScheduleItem(val routine: Routine, val date: LocalDate) : CalendarItem() {
        override val sortTimeMinutes: Int
            get() {
                val timeStr = routine.reminderTime ?: return 540 // 9:00 AM default
                val parts = timeStr.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                return h * 60 + m
            }
    }

    data class DeviceEventItem(val event: DeviceCalendarEvent) : CalendarItem() {
        override val sortTimeMinutes: Int
            get() {
                if (event.allDay) return -1
                val localTime = Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                return localTime.hour * 60 + localTime.minute
            }
    }
}

data class CalendarState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedWeekStart: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val isMonthGridExpanded: Boolean = false,
    val tasksByDate: Map<LocalDate, List<Task>> = emptyMap(),
    val routinesByDate: Map<LocalDate, List<Routine>> = emptyMap(),
    val nativeEventsByDate: Map<LocalDate, List<DeviceCalendarEvent>> = emptyMap(),
    val itemsByDate: Map<LocalDate, List<CalendarItem>> = emptyMap(),
    val datesInAgenda: List<LocalDate> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val preferences: CalendarPreferences = CalendarPreferences(),
    // Restyled Calendar tokens & calculated data structures
    val chipFilter: CalendarChipFilter = CalendarChipFilter.ALL,
    val agendaSections: List<AgendaDaySection> = emptyList(),
    val filteredItemsByDate: Map<LocalDate, List<AgendaItem>> = emptyMap(),
    val dotsByDate: Map<LocalDate, List<CalendarItemType>> = emptyMap(),
    val gridDays: List<GridDay> = emptyList(),
    val selectedRowIndex: Int = 0,
    val nowMinutes: Int = 0,
    val is24Hour: Boolean = false
)

class CalendarViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _selectedWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(CalendarPreferenceRepository.preferences.value.firstDayOfWeek))
    )
    private val _isMonthGridExpanded = MutableStateFlow(
        CalendarPreferenceRepository.preferences.value.defaultLandingView == "MONTH_GRID"
    )
    private val _hasPermission = MutableStateFlow(false)
    private val _nativeEvents = MutableStateFlow<List<DeviceCalendarEvent>>(emptyList())
    private val _chipFilter = MutableStateFlow(CalendarChipFilter.ALL)
    private val _is24Hour = MutableStateFlow(false)
    private val _nowMinutes = MutableStateFlow(LocalTime.now().let { it.hour * 60 + it.minute })

    private var _appContext: Context? = null
    private var loadedRangeStart: Long = 0L
    private var loadedRangeEnd: Long = 0L
    private var loadEventsJob: Job? = null

    init {
        // Ticker to keep nowMinutes fresh every minute
        viewModelScope.launch {
            while (isActive) {
                val now = LocalTime.now()
                _nowMinutes.value = now.hour * 60 + now.minute
                delay(30_000)
            }
        }
    }

    private val _uiControlState = combine(
        _currentMonth,
        _selectedDate,
        _selectedWeekStart,
        _isMonthGridExpanded,
        _hasPermission,
        _chipFilter,
        _is24Hour,
        _nowMinutes
    ) { args: Array<Any> ->
        @Suppress("UNCHECKED_CAST")
        ControlState(
            month = args[0] as YearMonth,
            selected = args[1] as LocalDate,
            weekStart = args[2] as LocalDate,
            isMonthGridExpanded = args[3] as Boolean,
            hasPermission = args[4] as Boolean,
            chipFilter = args[5] as CalendarChipFilter,
            is24Hour = args[6] as Boolean,
            nowMinutes = args[7] as Int
        )
    }

    val state: StateFlow<CalendarState> = combine(
        _uiControlState,
        _nativeEvents,
        repository.getAllTasks(),
        repository.getActiveRoutines(),
        CalendarPreferenceRepository.preferences
    ) { ctrl, events, allTasks, activeRoutines, prefs ->

        val today = LocalDate.now()

        // 1. Run pure calculation engine for agenda sections and dots
        val calculationResult: AgendaCalculationResult = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = ctrl.selected,
            agendaRangeDays = prefs.agendaRangeDays,
            chipFilter = ctrl.chipFilter,
            excludeRepeatingTasks = prefs.excludeRepeatingTasks,
            excludeCompletedTasks = prefs.excludeCompletedTasks,
            showRoutineReminders = prefs.showRoutineReminders,
            tasks = allTasks,
            routines = activeRoutines,
            events = events,
            nowMinutes = ctrl.nowMinutes,
            is24Hour = ctrl.is24Hour,
            calendarNameOverrides = prefs.calendarDisplayNameOverrides,
            currentMonth = ctrl.month
        )

        // 2. Compute morphing grid days and selected row
        val (gridDays, selectedRow) = CalendarCalculations.computeGridDays(
            currentMonth = ctrl.month,
            selectedDate = ctrl.selected,
            today = today,
            firstDayOfWeek = prefs.firstDayOfWeek,
            dotsMap = calculationResult.dotsByDate
        )

        // 3. For backward compatibility, populate tasksByDate, routinesByDate, nativeEventsByDate, itemsByDate, datesInAgenda
        val tasksMap = mutableMapOf<LocalDate, MutableList<Task>>()
        allTasks.forEach { task ->
            if (prefs.excludeCompletedTasks && task.isCompleted) return@forEach
            if (prefs.excludeRepeatingTasks && task.recurrenceFrequency != RecurrenceFrequency.NONE) return@forEach
            if (task.dueDate != null) {
                val date = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
                tasksMap.getOrPut(date) { mutableListOf() }.add(task)
            }
        }

        val routinesMap = mutableMapOf<LocalDate, MutableList<Routine>>()
        if (prefs.showRoutineReminders) {
            calculationResult.sections.forEach { section ->
                val date = section.date
                val dow = date.dayOfWeek
                activeRoutines.forEach { routine ->
                    if (routine.scheduledDaysSet.contains(dow)) {
                        routinesMap.getOrPut(date) { mutableListOf() }.add(routine)
                    }
                }
            }
        }

        val eventsMap = mutableMapOf<LocalDate, MutableList<DeviceCalendarEvent>>()
        events.forEach { event ->
            val date = Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            eventsMap.getOrPut(date) { mutableListOf() }.add(event)
        }

        val legacyItemsMap = mutableMapOf<LocalDate, List<CalendarItem>>()
        val datesWithItems = (tasksMap.keys + routinesMap.keys + eventsMap.keys + ctrl.selected).sorted()
        datesWithItems.forEach { date ->
            val items = mutableListOf<CalendarItem>()
            tasksMap[date]?.forEach { items.add(CalendarItem.TaskItem(it)) }
            routinesMap[date]?.forEach { items.add(CalendarItem.RoutineScheduleItem(it, date)) }
            eventsMap[date]?.forEach { items.add(CalendarItem.DeviceEventItem(it)) }
            items.sortWith(compareBy<CalendarItem> { if (it.sortTimeMinutes < 0) 0 else 1 }.thenBy { it.sortTimeMinutes })
            legacyItemsMap[date] = items
        }

        CalendarState(
            currentMonth = ctrl.month,
            selectedDate = ctrl.selected,
            selectedWeekStart = ctrl.weekStart,
            isMonthGridExpanded = ctrl.isMonthGridExpanded,
            tasksByDate = tasksMap,
            routinesByDate = routinesMap,
            nativeEventsByDate = eventsMap,
            itemsByDate = legacyItemsMap,
            datesInAgenda = calculationResult.sections.map { it.date },
            hasCalendarPermission = ctrl.hasPermission,
            preferences = prefs,
            chipFilter = ctrl.chipFilter,
            agendaSections = calculationResult.sections,
            filteredItemsByDate = calculationResult.filteredItemsByDate,
            dotsByDate = calculationResult.dotsByDate,
            gridDays = gridDays,
            selectedRowIndex = selectedRow,
            nowMinutes = ctrl.nowMinutes,
            is24Hour = ctrl.is24Hour
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = run {
            val initialToday = LocalDate.now()
            val initialMonth = YearMonth.from(initialToday)
            val firstDay = CalendarPreferenceRepository.preferences.value.firstDayOfWeek
            val (initialGridDays, initialSelectedRow) = CalendarCalculations.computeGridDays(
                currentMonth = initialMonth,
                selectedDate = initialToday,
                today = initialToday,
                firstDayOfWeek = firstDay,
                dotsMap = emptyMap()
            )
            CalendarState(
                currentMonth = initialMonth,
                selectedDate = initialToday,
                selectedWeekStart = initialToday.with(TemporalAdjusters.previousOrSame(firstDay)),
                gridDays = initialGridDays,
                selectedRowIndex = initialSelectedRow
            )
        }
    )

    fun checkAndLoadCalendarPermission(context: Context) {
        _appContext = context.applicationContext
        val is24 = android.text.format.DateFormat.is24HourFormat(context)
        _is24Hour.value = is24

        val hasPerm = CalendarProviderHelper.hasReadPermission(context)
        _hasPermission.value = hasPerm
        if (hasPerm) {
            loadDeviceCalendarEvents(context)
        }
    }

    fun loadDeviceCalendarEvents(context: Context? = null, force: Boolean = false) {
        val ctx = context?.applicationContext ?: _appContext ?: return
        _appContext = ctx
        if (!CalendarProviderHelper.hasReadPermission(ctx)) return

        val prefs = CalendarPreferenceRepository.preferences.value
        val today = LocalDate.now()
        val todayMonth = YearMonth.from(today)
        val month = _currentMonth.value

        // Query a generous window: from at least 12 months in the past to at least 24 months in the future
        val minYearMonth = if (month.isBefore(todayMonth)) month.minusMonths(12) else todayMonth.minusMonths(12)
        val maxYearMonth = if (month.isAfter(todayMonth.plusMonths(18))) month.plusMonths(24) else todayMonth.plusMonths(24)

        val startOfRange = minYearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfRange = maxYearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (!force && loadedRangeStart > 0 && loadedRangeEnd > 0 && startOfRange >= loadedRangeStart && endOfRange <= loadedRangeEnd && _nativeEvents.value.isNotEmpty()) {
            return
        }

        loadEventsJob?.cancel()
        loadEventsJob = viewModelScope.launch(Dispatchers.IO) {
            val events = CalendarProviderHelper.getEventsInRange(ctx, startOfRange, endOfRange, prefs.enabledCalendarIds)
            _nativeEvents.value = events
            loadedRangeStart = startOfRange
            loadedRangeEnd = endOfRange
        }
    }

    fun setChipFilter(filter: CalendarChipFilter) {
        _chipFilter.value = filter
    }

    fun setSelectedDate(date: LocalDate, context: Context? = null) {
        _selectedDate.value = date
        val firstDay = CalendarPreferenceRepository.preferences.value.firstDayOfWeek
        _selectedWeekStart.value = date.with(TemporalAdjusters.previousOrSame(firstDay))
        if (date.year != _currentMonth.value.year || date.month != _currentMonth.value.month) {
            _currentMonth.value = YearMonth.from(date)
            loadDeviceCalendarEvents(context)
        }
    }

    fun selectToday(): LocalDate {
        val today = LocalDate.now()
        setSelectedDate(today)
        return today
    }

    fun previousWeek(): LocalDate {
        val newDate = _selectedDate.value.minusWeeks(1)
        setSelectedDate(newDate)
        return newDate
    }

    fun nextWeek(): LocalDate {
        val newDate = _selectedDate.value.plusWeeks(1)
        setSelectedDate(newDate)
        return newDate
    }

    fun previousMonth(context: Context? = null): LocalDate {
        val newMonth = _currentMonth.value.minusMonths(1)
        _currentMonth.value = newMonth
        val today = LocalDate.now()
        val newSelected = if (newMonth == YearMonth.from(today)) {
            today
        } else {
            newMonth.atDay(1)
        }
        _selectedDate.value = newSelected
        val firstDay = CalendarPreferenceRepository.preferences.value.firstDayOfWeek
        _selectedWeekStart.value = newSelected.with(TemporalAdjusters.previousOrSame(firstDay))
        loadDeviceCalendarEvents(context)
        return newSelected
    }

    fun nextMonth(context: Context? = null): LocalDate {
        val newMonth = _currentMonth.value.plusMonths(1)
        _currentMonth.value = newMonth
        val today = LocalDate.now()
        val newSelected = if (newMonth == YearMonth.from(today)) {
            today
        } else {
            newMonth.atDay(1)
        }
        _selectedDate.value = newSelected
        val firstDay = CalendarPreferenceRepository.preferences.value.firstDayOfWeek
        _selectedWeekStart.value = newSelected.with(TemporalAdjusters.previousOrSame(firstDay))
        loadDeviceCalendarEvents(context)
        return newSelected
    }

    fun toggleMonthGridExpanded() {
        _isMonthGridExpanded.value = !_isMonthGridExpanded.value
    }

    fun setMonthGridExpanded(expanded: Boolean) {
        _isMonthGridExpanded.value = expanded
    }

    fun toggleTaskComplete(task: Task, context: Context? = null) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task, context)
        }
    }

    fun updateTask(task: Task, context: Context) {
        viewModelScope.launch {
            repository.updateTask(task)
            if (task.dueTime != null && !task.isCompleted) {
                AlarmScheduler.scheduleTaskReminder(context, task)
            } else {
                AlarmScheduler.cancelTaskReminder(context, task.id)
            }
        }
    }

    fun addTaskForSelectedDate(task: Task, context: Context) {
        viewModelScope.launch {
            val selectedLocalDate = _selectedDate.value
            val dueDateMillis = selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val finalDueTime = task.dueTime ?: selectedLocalDate.atTime(TimeDefaults.DEFAULT_CALENDAR_QUICK_ADD_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()

            val finalTask = task.copy(
                dueDate = dueDateMillis,
                dueTime = finalDueTime,
                reminderTime = task.reminderTime ?: finalDueTime
            )

            val insertedId = repository.insertTask(finalTask)
            val insertedTask = finalTask.copy(id = insertedId)

            AlarmScheduler.scheduleTaskReminder(context, insertedTask)
        }
    }

    fun addTaskFromNaturalLanguage(input: String, context: Context? = null, fallbackDate: LocalDate? = null) {
        if (input.isBlank()) return
        val parsed = TaskParser.parse(input)
        val finalDueDate = parsed.dueDate ?: fallbackDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val finalDueTime = parsed.dueTime ?: (if (parsed.dueDate == null && fallbackDate != null) {
            fallbackDate.atTime(TimeDefaults.DEFAULT_CALENDAR_QUICK_ADD_TIME)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null)
        val reminderToUse = finalDueTime ?: (if (finalDueDate != null) finalDueDate + (9 * 3600000L) else null)

        val newTask = Task(
            title = parsed.title,
            dueDate = finalDueDate,
            dueTime = finalDueTime,
            priority = parsed.priority,
            reminderTime = reminderToUse,
            recurrenceFrequency = parsed.recurrenceFrequency,
            recurrenceInterval = parsed.recurrenceInterval,
            recurrenceUnit = parsed.recurrenceUnit,
            recurrenceDaysOfWeek = parsed.recurrenceDaysOfWeek,
            recurrenceMonthlyMode = parsed.recurrenceMonthlyMode
        )
        viewModelScope.launch {
            val insertedId = repository.insertTask(newTask)
            if (context != null && reminderToUse != null && reminderToUse > System.currentTimeMillis()) {
                AlarmScheduler.scheduleTaskReminder(context, newTask.copy(id = insertedId))
            }
        }
    }

    fun deleteTask(task: Task, context: Context) {
        viewModelScope.launch {
            repository.deleteTask(task, context)
            AlarmScheduler.cancelTaskReminder(context, task.id)
        }
    }

    class Factory(private val repository: ReflexRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CalendarViewModel(repository) as T
        }
    }
}

private data class ControlState(
    val month: YearMonth,
    val selected: LocalDate,
    val weekStart: LocalDate,
    val isMonthGridExpanded: Boolean,
    val hasPermission: Boolean,
    val chipFilter: CalendarChipFilter,
    val is24Hour: Boolean,
    val nowMinutes: Int
)
