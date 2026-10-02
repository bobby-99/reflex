package com.reflex.app.util

import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.Routine
import com.reflex.app.data.Task
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

enum class CalendarItemType {
    EVENT,
    TASK,
    ROUTINE
}

enum class CalendarChipFilter(val label: String) {
    ALL("All"),
    EVENTS("Events"),
    TASKS("Tasks"),
    ROUTINES("Routines")
}

data class AgendaItem(
    val id: String,
    val type: CalendarItemType,
    val title: String,
    val subtitle: String,
    val timeLabel: String,
    val startMinutes: Int, // -1 for all day
    val endMinutes: Int? = null,
    val isCompleted: Boolean = false,
    val isAllDay: Boolean = false,
    val taskRef: Task? = null,
    val eventRef: DeviceCalendarEvent? = null,
    val routineRef: Routine? = null
)

data class AgendaDaySection(
    val date: LocalDate,
    val items: List<AgendaItem>,
    val nowDividerIndex: Int? = null,
    val isDashedEmpty: Boolean = false
)

data class GridDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val dots: List<CalendarItemType>,
    val talkBackDescription: String
)

data class AgendaCalculationResult(
    val sections: List<AgendaDaySection>,
    val filteredItemsByDate: Map<LocalDate, List<AgendaItem>>,
    val dotsByDate: Map<LocalDate, List<CalendarItemType>>
)

object CalendarCalculations {

    /**
     * Formats minutes from midnight into 12-hour or 24-hour localized format.
     * Enforces universal sentence case (e.g. "4:00 pm").
     */
    fun formatMinutes(minutes: Int, is24Hour: Boolean): String {
        if (minutes < 0) return "All day"
        val h = minutes / 60
        val m = minutes % 60
        val mm = if (m < 10) "0$m" else "$m"
        return if (is24Hour) {
            val hh = if (h < 10) "0$h" else "$h"
            "$hh:$mm"
        } else {
            val displayH = if (h == 0) 12 else if (h > 12) h - 12 else h
            val amPm = if (h < 12) "am" else "pm"
            "$displayH:$mm $amPm"
        }
    }

    /**
     * Extracts date and start minute for an event.
     * All-day events are strictly interpreted in UTC to avoid western timezone date shift.
     */
    fun getEventDateAndMinutes(event: DeviceCalendarEvent): Pair<LocalDate, Int> {
        return if (event.allDay) {
            val date = Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.of("UTC")).toLocalDate()
            Pair(date, -1)
        } else {
            val zdt = Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault())
            Pair(zdt.toLocalDate(), zdt.hour * 60 + zdt.minute)
        }
    }

    /**
     * Extracts end date and end minute for an event.
     */
    fun getEventEndDateAndMinutes(event: DeviceCalendarEvent): Pair<LocalDate, Int> {
        return if (event.allDay) {
            val date = Instant.ofEpochMilli(event.endMillis).atZone(ZoneId.of("UTC")).toLocalDate()
            Pair(date, -1)
        } else {
            val zdt = Instant.ofEpochMilli(event.endMillis).atZone(ZoneId.systemDefault())
            Pair(zdt.toLocalDate(), zdt.hour * 60 + zdt.minute)
        }
    }

    /**
     * Computes the time label for multi-day and single-day events.
     */
    fun formatEventTimeLabel(
        event: DeviceCalendarEvent,
        day: LocalDate,
        is24Hour: Boolean
    ): String {
        if (event.allDay) return "All day"

        val (startDate, startMin) = getEventDateAndMinutes(event)
        val (endDate, endMin) = getEventEndDateAndMinutes(event)

        return if (startDate == endDate || endDate.isBefore(startDate)) {
            val startStr = formatMinutes(startMin, is24Hour)
            if (endMin > startMin) {
                val endStr = formatMinutes(endMin, is24Hour)
                "$startStr – $endStr"
            } else {
                startStr
            }
        } else {
            // Multi-day timed event
            when {
                day == startDate -> formatMinutes(startMin, is24Hour)
                day == endDate -> "Until ${formatMinutes(endMin, is24Hour)}"
                else -> "All day"
            }
        }
    }

    /**
     * Computes the "Now" divider index for a day section on today.
     * Placed before the first timed item starting after nowMinutes,
     * or after the last item if none remain.
     * If items is empty, returns 0.
     */
    fun computeNowDividerIndex(items: List<AgendaItem>, nowMinutes: Int): Int {
        if (items.isEmpty()) return 0

        val firstAfterIndex = items.indexOfFirst { it.startMinutes >= 0 && it.startMinutes > nowMinutes }
        return if (firstAfterIndex >= 0) {
            firstAfterIndex
        } else {
            items.size
        }
    }

    /**
     * Builds dots list for a date (up to 3 items, in fixed order: EVENT, TASK, ROUTINE).
     */
    fun computeDotsForDate(
        items: List<AgendaItem>,
        filter: CalendarChipFilter
    ): List<CalendarItemType> {
        val typesPresent = items.map { it.type }.toSet()
        val dots = mutableListOf<CalendarItemType>()

        if (typesPresent.contains(CalendarItemType.EVENT) && (filter == CalendarChipFilter.ALL || filter == CalendarChipFilter.EVENTS)) {
            dots.add(CalendarItemType.EVENT)
        }
        if (typesPresent.contains(CalendarItemType.TASK) && (filter == CalendarChipFilter.ALL || filter == CalendarChipFilter.TASKS)) {
            dots.add(CalendarItemType.TASK)
        }
        if (typesPresent.contains(CalendarItemType.ROUTINE) && (filter == CalendarChipFilter.ALL || filter == CalendarChipFilter.ROUTINES)) {
            dots.add(CalendarItemType.ROUTINE)
        }
        return dots
    }

    /**
     * Builds the complete morphing calendar grid days (4 to 6 rows).
     * Returns Pair(gridDays, selectedRowIndex).
     */
    fun computeGridDays(
        currentMonth: YearMonth,
        selectedDate: LocalDate,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        dotsMap: Map<LocalDate, List<CalendarItemType>>,
        locale: Locale = Locale.getDefault()
    ): Pair<List<GridDay>, Int> {
        val firstOfMonth = currentMonth.atDay(1)
        val dim = currentMonth.lengthOfMonth()

        // Calculate offset based on firstDayOfWeek setting (Monday or Sunday)
        val dayOfWeekVal = firstOfMonth.dayOfWeek.value // 1 = Monday, 7 = Sunday
        val firstDayVal = firstDayOfWeek.value
        val offset = (dayOfWeekVal - firstDayVal + 7) % 7

        val totalDays = offset + dim
        val rows = (totalDays + 6) / 7
        val gridStartDate = firstOfMonth.minusDays(offset.toLong())

        val gridDays = mutableListOf<GridDay>()
        var selectedRow = 0

        for (i in 0 until (rows * 7)) {
            val date = gridStartDate.plusDays(i.toLong())
            val isCurrentMonth = (date.year == currentMonth.year && date.month == currentMonth.month)
            val isToday = (date == today)
            val isSelected = (date == selectedDate)
            val dots = dotsMap[date] ?: emptyList()

            val row = i / 7
            if (isSelected) {
                selectedRow = row
            }

            val weekdayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            val monthName = date.month.getDisplayName(TextStyle.FULL, locale)
            val dayNum = date.dayOfMonth
            val countText = if (dots.isEmpty()) "0 items" else "${dots.size} item${if (dots.size == 1) "" else "s"}"
            val selText = if (isSelected) ", selected" else ", not selected"
            val talkBack = "$weekdayName, $monthName $dayNum, $countText$selText"

            gridDays.add(
                GridDay(
                    date = date,
                    isCurrentMonth = isCurrentMonth,
                    isToday = isToday,
                    isSelected = isSelected,
                    dots = dots,
                    talkBackDescription = talkBack
                )
            )
        }

        // Clamp selectedRow in bounds [0, rows - 1]
        selectedRow = selectedRow.coerceIn(0, rows - 1)
        return Pair(gridDays, selectedRow)
    }

    /**
     * Builds complete agenda day sections and maps items for calendar dots.
     */
    fun computeAgendaSections(
        today: LocalDate,
        selectedDate: LocalDate,
        agendaRangeDays: Int,
        chipFilter: CalendarChipFilter,
        excludeRepeatingTasks: Boolean,
        excludeCompletedTasks: Boolean,
        showRoutineReminders: Boolean,
        tasks: List<Task>,
        routines: List<Routine>,
        events: List<DeviceCalendarEvent>,
        nowMinutes: Int,
        is24Hour: Boolean,
        calendarNameOverrides: Map<String, String> = emptyMap(),
        currentMonth: YearMonth = YearMonth.from(selectedDate)
    ): AgendaCalculationResult {

        val rawItemsByDate = mutableMapOf<LocalDate, MutableList<AgendaItem>>()

        // 1. Process Tasks
        tasks.forEach { task ->
            if (excludeCompletedTasks && task.isCompleted) return@forEach
            if (excludeRepeatingTasks && task.recurrenceFrequency != RecurrenceFrequency.NONE) return@forEach
            if (task.dueDate == null) return@forEach

            val date = Instant.ofEpochMilli(task.dueDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val startMin = task.dueTime?.let { millis ->
                val zdt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
                zdt.hour * 60 + zdt.minute
            } ?: -1

            val timeLabel = if (startMin < 0) "All day" else formatMinutes(startMin, is24Hour)
            val subtitle = when (task.priority) {
                Priority.HIGH -> "Task · High priority"
                Priority.MEDIUM -> "Task · Medium priority"
                Priority.LOW -> "Task · Low priority"
                Priority.NONE -> "Task"
            }

            rawItemsByDate.getOrPut(date) { mutableListOf() }.add(
                AgendaItem(
                    id = "task_${task.id}",
                    type = CalendarItemType.TASK,
                    title = task.title,
                    subtitle = subtitle,
                    timeLabel = timeLabel,
                    startMinutes = startMin,
                    isCompleted = task.isCompleted,
                    isAllDay = (startMin < 0),
                    taskRef = task
                )
            )
        }

        // 2. Process Routine Reminders
        if (showRoutineReminders) {
            // Generate routine instances for an extended range around today, selectedDate, and currentMonth
            val minDate = minOf(
                today.minusDays(30),
                selectedDate.minusDays(7),
                currentMonth.atDay(1).minusDays(14)
            )
            val maxDate = maxOf(
                today.plusDays(agendaRangeDays.toLong() + 30),
                selectedDate.plusDays(7),
                currentMonth.atEndOfMonth().plusDays(14)
            )
            var cur = minDate
            while (!cur.isAfter(maxDate)) {
                val dow = cur.dayOfWeek
                routines.forEach { routine ->
                    if (routine.scheduledDaysSet.contains(dow)) {
                        val timeStr = routine.reminderTime ?: "09:00"
                        val parts = timeStr.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        val startMin = h * 60 + m
                        val timeLabel = formatMinutes(startMin, is24Hour)

                        rawItemsByDate.getOrPut(cur) { mutableListOf() }.add(
                            AgendaItem(
                                id = "routine_${routine.id}_$cur",
                                type = CalendarItemType.ROUTINE,
                                title = routine.name,
                                subtitle = "Routine reminder",
                                timeLabel = timeLabel,
                                startMinutes = startMin,
                                isCompleted = false,
                                isAllDay = false,
                                routineRef = routine
                            )
                        )
                    }
                }
                cur = cur.plusDays(1)
            }
        }

        // 3. Process Device Calendar Events
        events.forEach { event ->
            val (startDate, _) = getEventDateAndMinutes(event)
            val (endDate, _) = getEventEndDateAndMinutes(event)

            val effectiveEnd = if (endDate.isBefore(startDate)) startDate else endDate

            // For all-day events, CalendarContract ends at midnight next day, so clamp to start if same day
            val lastDay = if (event.allDay && effectiveEnd.isAfter(startDate)) {
                effectiveEnd.minusDays(1)
            } else {
                effectiveEnd
            }

            var day = startDate
            while (!day.isAfter(lastDay)) {
                val (evStartDay, startMin) = getEventDateAndMinutes(event)
                val itemStartMin = if (event.allDay || day != evStartDay) -1 else startMin
                val timeLabel = formatEventTimeLabel(event, day, is24Hour)

                val calName = event.calendarName ?: "Calendar"
                val resolvedName = if (calName.contains("://") || calName.length > 50) {
                    "Subscribed calendar"
                } else {
                    calendarNameOverrides[calName] ?: calName
                }

                rawItemsByDate.getOrPut(day) { mutableListOf() }.add(
                    AgendaItem(
                        id = "event_${event.id}_$day",
                        type = CalendarItemType.EVENT,
                        title = event.title,
                        subtitle = resolvedName,
                        timeLabel = timeLabel,
                        startMinutes = itemStartMin,
                        isCompleted = false,
                        isAllDay = (itemStartMin < 0),
                        eventRef = event
                    )
                )
                day = day.plusDays(1)
            }
        }

        // 4. Sort items within each day: All-day first (-1), then ascending by startMinutes,
        // then by type order (EVENT = 0, TASK = 1, ROUTINE = 2), then by title
        val sortedAllItemsByDate = mutableMapOf<LocalDate, List<AgendaItem>>()
        rawItemsByDate.forEach { (date, items) ->
            val sorted = items.sortedWith(
                compareBy<AgendaItem> { if (it.startMinutes < 0) 0 else 1 }
                    .thenBy { if (it.startMinutes < 0) 0 else it.startMinutes }
                    .thenBy { it.type.ordinal }
                    .thenBy { it.title.lowercase(Locale.ROOT) }
            )
            sortedAllItemsByDate[date] = sorted
        }

        // 5. Apply Session Chip Filter to items
        val filteredItemsByDate = mutableMapOf<LocalDate, List<AgendaItem>>()
        sortedAllItemsByDate.forEach { (date, items) ->
            val filtered = items.filter { item ->
                when (chipFilter) {
                    CalendarChipFilter.ALL -> true
                    CalendarChipFilter.EVENTS -> item.type == CalendarItemType.EVENT
                    CalendarChipFilter.TASKS -> item.type == CalendarItemType.TASK
                    CalendarChipFilter.ROUTINES -> item.type == CalendarItemType.ROUTINE
                }
            }
            if (filtered.isNotEmpty()) {
                filteredItemsByDate[date] = filtered
            }
        }

        // 6. Compute dots for every date based on items passing the active chip filter
        val dotsMap = mutableMapOf<LocalDate, List<CalendarItemType>>()
        sortedAllItemsByDate.forEach { (date, items) ->
            val dots = computeDotsForDate(items, chipFilter)
            if (dots.isNotEmpty()) {
                dotsMap[date] = dots
            }
        }

        // 7. Build Agenda Sections
        val agendaDates = mutableSetOf<LocalDate>()

        // (a) Upcoming range from today: [today .. today + agendaRangeDays]
        for (offset in 0..agendaRangeDays) {
            val d = today.plusDays(offset.toLong())
            if (filteredItemsByDate[d]?.isNotEmpty() == true) {
                agendaDates.add(d)
            }
        }

        // (b) If viewing or selected in a different month (navigated ahead/behind),
        // include dates in that month that have items (holidays, events, tasks)
        val todayMonth = YearMonth.from(today)
        val targetMonths = mutableSetOf<YearMonth>()
        if (currentMonth != todayMonth) {
            targetMonths.add(currentMonth)
        }
        val selectedMonth = YearMonth.from(selectedDate)
        if (selectedMonth != todayMonth) {
            targetMonths.add(selectedMonth)
        }

        targetMonths.forEach { m ->
            val mStart = m.atDay(1)
            val mEnd = m.atEndOfMonth()
            var d = mStart
            while (!d.isAfter(mEnd)) {
                if (filteredItemsByDate[d]?.isNotEmpty() == true) {
                    agendaDates.add(d)
                }
                d = d.plusDays(1)
            }
        }

        // (c) Always inject today
        agendaDates.add(today)

        // (d) Always inject selectedDate even if empty or in the past
        agendaDates.add(selectedDate)

        val sections = agendaDates.sorted().map { date ->
            val items = filteredItemsByDate[date] ?: emptyList()
            val isSelectedEmpty = (date == selectedDate && items.isEmpty())
            val nowDivider = if (date == today) {
                computeNowDividerIndex(items, nowMinutes)
            } else {
                null
            }

            AgendaDaySection(
                date = date,
                items = items,
                nowDividerIndex = nowDivider,
                isDashedEmpty = isSelectedEmpty
            )
        }

        return AgendaCalculationResult(
            sections = sections,
            filteredItemsByDate = filteredItemsByDate,
            dotsByDate = dotsMap
        )
    }
}
