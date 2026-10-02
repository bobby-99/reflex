package com.reflex.app

import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.Routine
import com.reflex.app.data.Task
import com.reflex.app.util.CalendarCalculations
import com.reflex.app.util.CalendarChipFilter
import com.reflex.app.util.CalendarItemType
import com.reflex.app.util.DeviceCalendarEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class CalendarCalculationsTest {

    private val today = LocalDate.of(2026, 10, 1) // A Thursday
    private val zone = ZoneId.systemDefault()
    private val allDaysString = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY"

    private fun localMillis(date: LocalDate, hour: Int, minute: Int): Long {
        return date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    private fun utcMidnightMillis(date: LocalDate): Long {
        return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    @Test
    fun testAllDayEventsSortedBeforeTimedItems() {
        // Given an all-day event and a 9:00 AM timed event on the same day
        val allDayEvent = DeviceCalendarEvent(
            id = 1L,
            title = "Holiday",
            startMillis = utcMidnightMillis(today),
            endMillis = utcMidnightMillis(today.plusDays(1)),
            allDay = true,
            calendarName = "Holidays"
        )
        val timedEvent = DeviceCalendarEvent(
            id = 2L,
            title = "Team Standup",
            startMillis = localMillis(today, 9, 0),
            endMillis = localMillis(today, 9, 30),
            allDay = false,
            calendarName = "Work"
        )

        val result = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = today,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = false,
            showRoutineReminders = false,
            tasks = emptyList(),
            routines = emptyList(),
            events = listOf(timedEvent, allDayEvent),
            nowMinutes = 480, // 8:00 am
            is24Hour = false
        )

        val todaySection = result.sections.first { it.date == today }
        assertEquals(2, todaySection.items.size)
        // All-day item should be first
        assertEquals("Holiday", todaySection.items[0].title)
        assertTrue(todaySection.items[0].isAllDay)
        assertEquals(-1, todaySection.items[0].startMinutes)

        // Timed item should be second
        assertEquals("Team Standup", todaySection.items[1].title)
        assertEquals(540, todaySection.items[1].startMinutes)
    }

    @Test
    fun testDeterministicTieBreakingForItemsAtSameTime() {
        // Items at same time (10:00 am = 600 min): Event, Task, Routine, and alphabetical within type
        val eventB = DeviceCalendarEvent(
            id = 10L,
            title = "Beta Event",
            startMillis = localMillis(today, 10, 0),
            endMillis = localMillis(today, 11, 0),
            allDay = false
        )
        val eventA = DeviceCalendarEvent(
            id = 11L,
            title = "Alpha Event",
            startMillis = localMillis(today, 10, 0),
            endMillis = localMillis(today, 11, 0),
            allDay = false
        )
        val task = Task(
            id = 20L,
            title = "Task Zebra",
            dueDate = localMillis(today, 0, 0),
            dueTime = localMillis(today, 10, 0)
        )
        val routine = Routine(
            id = 30L,
            name = "Morning Stretch",
            reminderTime = "10:00",
            scheduledDays = allDaysString
        )

        val result = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = today,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = false,
            showRoutineReminders = true,
            tasks = listOf(task),
            routines = listOf(routine),
            events = listOf(eventB, eventA),
            nowMinutes = 480,
            is24Hour = false
        )

        val items = result.sections.first { it.date == today }.items
        assertEquals(4, items.size)
        // Events first (sorted alphabetically: Alpha Event, then Beta Event)
        assertEquals("Alpha Event", items[0].title)
        assertEquals(CalendarItemType.EVENT, items[0].type)
        assertEquals("Beta Event", items[1].title)
        assertEquals(CalendarItemType.EVENT, items[1].type)

        // Then Task
        assertEquals("Task Zebra", items[2].title)
        assertEquals(CalendarItemType.TASK, items[2].type)

        // Then Routine
        assertEquals("Morning Stretch", items[3].title)
        assertEquals(CalendarItemType.ROUTINE, items[3].type)
    }

    @Test
    fun testSelectedDayInjectedIntoAgendaEvenWhenEmpty() {
        // Selected day in past with 0 items
        val pastSelected = today.minusDays(5)

        val result = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = pastSelected,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = false,
            showRoutineReminders = false,
            tasks = emptyList(),
            routines = emptyList(),
            events = emptyList(),
            nowMinutes = 600,
            is24Hour = false
        )

        val pastSection = result.sections.find { it.date == pastSelected }
        assertNotNull(pastSection)
        assertTrue(pastSection!!.items.isEmpty())
        assertTrue(pastSection.isDashedEmpty)
    }

    @Test
    fun testAgendaRangeCutoffItemsBeyondRangeDaysExcluded() {
        val inRangeDate = today.plusDays(5)
        val outOfRangeDate = today.plusDays(10)

        val taskInRange = Task(
            id = 1L,
            title = "In Range Task",
            dueDate = localMillis(inRangeDate, 0, 0),
            dueTime = localMillis(inRangeDate, 12, 0)
        )
        val taskOutOfRange = Task(
            id = 2L,
            title = "Out of Range Task",
            dueDate = localMillis(outOfRangeDate, 0, 0),
            dueTime = localMillis(outOfRangeDate, 12, 0)
        )

        val result = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = today,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = false,
            showRoutineReminders = false,
            tasks = listOf(taskInRange, taskOutOfRange),
            routines = emptyList(),
            events = emptyList(),
            nowMinutes = 600,
            is24Hour = false
        )

        val allItemTitles = result.sections.flatMap { it.items }.map { it.title }
        assertTrue(allItemTitles.contains("In Range Task"))
        assertFalse(allItemTitles.contains("Out of Range Task"))
    }

    @Test
    fun testEmptyTodayRendersWithNowDivider() {
        // Today has no items
        val result = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = today,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = false,
            showRoutineReminders = false,
            tasks = emptyList(),
            routines = emptyList(),
            events = emptyList(),
            nowMinutes = 720, // 12:00 pm
            is24Hour = false
        )

        val todaySection = result.sections.find { it.date == today }
        assertNotNull(todaySection)
        assertEquals(0, todaySection!!.items.size)
        // Now divider should be at index 0
        assertEquals(0, todaySection.nowDividerIndex)
    }

    @Test
    fun testNowDividerPlacementBetweenPastAndFutureItems() {
        // Items on today: 9:00 am (540 min), 1:00 pm (780 min), 5:00 pm (1020 min)
        // Now is 2:00 pm (840 min)
        val task9am = Task(id = 1L, title = "Task 9am", dueDate = localMillis(today, 0, 0), dueTime = localMillis(today, 9, 0))
        val task1pm = Task(id = 2L, title = "Task 1pm", dueDate = localMillis(today, 0, 0), dueTime = localMillis(today, 13, 0))
        val task5pm = Task(id = 3L, title = "Task 5pm", dueDate = localMillis(today, 0, 0), dueTime = localMillis(today, 17, 0))

        val result = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = today,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = false,
            showRoutineReminders = false,
            tasks = listOf(task9am, task1pm, task5pm),
            routines = emptyList(),
            events = emptyList(),
            nowMinutes = 840, // 2:00 pm (between 1pm and 5pm)
            is24Hour = false
        )

        val todaySection = result.sections.first { it.date == today }
        assertEquals(3, todaySection.items.size)
        // Item 0 is 9am, Item 1 is 1pm, Item 2 is 5pm
        // First item with startMinutes > 840 is item 2 (5pm)
        // So nowDividerIndex should be 2!
        assertEquals(2, todaySection.nowDividerIndex)
    }

    @Test
    fun testExcludeCompletedTasksFilter() {
        val completedTask = Task(
            id = 1L,
            title = "Completed Task",
            dueDate = localMillis(today, 0, 0),
            dueTime = localMillis(today, 10, 0),
            isCompleted = true
        )
        val activeTask = Task(
            id = 2L,
            title = "Active Task",
            dueDate = localMillis(today, 0, 0),
            dueTime = localMillis(today, 11, 0),
            isCompleted = false
        )

        // When excludeCompletedTasks = true
        val resultExcluded = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = today,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = true,
            showRoutineReminders = false,
            tasks = listOf(completedTask, activeTask),
            routines = emptyList(),
            events = emptyList(),
            nowMinutes = 480,
            is24Hour = false
        )
        val itemsExcluded = resultExcluded.sections.first { it.date == today }.items
        assertEquals(1, itemsExcluded.size)
        assertEquals("Active Task", itemsExcluded[0].title)

        // When excludeCompletedTasks = false
        val resultIncluded = CalendarCalculations.computeAgendaSections(
            today = today,
            selectedDate = today,
            agendaRangeDays = 7,
            chipFilter = CalendarChipFilter.ALL,
            excludeRepeatingTasks = false,
            excludeCompletedTasks = false,
            showRoutineReminders = false,
            tasks = listOf(completedTask, activeTask),
            routines = emptyList(),
            events = emptyList(),
            nowMinutes = 480,
            is24Hour = false
        )
        val itemsIncluded = resultIncluded.sections.first { it.date == today }.items
        assertEquals(2, itemsIncluded.size)
    }

    @Test
    fun testFilterChipsFiltering() {
        val event = DeviceCalendarEvent(id = 1L, title = "Meeting", startMillis = localMillis(today, 9, 0), endMillis = localMillis(today, 10, 0))
        val task = Task(id = 2L, title = "Homework", dueDate = localMillis(today, 0, 0), dueTime = localMillis(today, 11, 0))
        val routine = Routine(id = 3L, name = "Reading", reminderTime = "14:00", scheduledDays = allDaysString)

        val allItems = listOf(event)
        val allTasks = listOf(task)
        val allRoutines = listOf(routine)

        // 1. ALL
        val resAll = CalendarCalculations.computeAgendaSections(
            today, today, 7, CalendarChipFilter.ALL, false, false, true, allTasks, allRoutines, allItems, 400, false
        )
        assertEquals(3, resAll.sections.first { it.date == today }.items.size)
        assertEquals(listOf(CalendarItemType.EVENT, CalendarItemType.TASK, CalendarItemType.ROUTINE), resAll.dotsByDate[today])

        // 2. EVENTS
        val resEv = CalendarCalculations.computeAgendaSections(
            today, today, 7, CalendarChipFilter.EVENTS, false, false, true, allTasks, allRoutines, allItems, 400, false
        )
        val evItems = resEv.sections.first { it.date == today }.items
        assertEquals(1, evItems.size)
        assertEquals(CalendarItemType.EVENT, evItems[0].type)
        assertEquals(listOf(CalendarItemType.EVENT), resEv.dotsByDate[today])

        // 3. TASKS
        val resTk = CalendarCalculations.computeAgendaSections(
            today, today, 7, CalendarChipFilter.TASKS, false, false, true, allTasks, allRoutines, allItems, 400, false
        )
        val tkItems = resTk.sections.first { it.date == today }.items
        assertEquals(1, tkItems.size)
        assertEquals(CalendarItemType.TASK, tkItems[0].type)
        assertEquals(listOf(CalendarItemType.TASK), resTk.dotsByDate[today])

        // 4. ROUTINES
        val resRt = CalendarCalculations.computeAgendaSections(
            today, today, 7, CalendarChipFilter.ROUTINES, false, false, true, allTasks, allRoutines, allItems, 400, false
        )
        val rtItems = resRt.sections.first { it.date == today }.items
        assertEquals(1, rtItems.size)
        assertEquals(CalendarItemType.ROUTINE, rtItems[0].type)
        assertEquals(listOf(CalendarItemType.ROUTINE), resRt.dotsByDate[today])
    }

    @Test
    fun testFirstDayOfWeekSundayVsMondayGridAlignment() {
        // October 2026 starts on a Thursday (day 1 = Thursday)
        val month = YearMonth.of(2026, 10)
        val selected = LocalDate.of(2026, 10, 1)

        // Monday start: Monday = 0, Tuesday = 1, Wednesday = 2, Thursday = 3.
        // Offset is 3. Grid start date should be Sep 28, 2026 (Monday).
        val (gridMon, selectedRowMon) = CalendarCalculations.computeGridDays(
            currentMonth = month,
            selectedDate = selected,
            today = selected,
            firstDayOfWeek = DayOfWeek.MONDAY,
            dotsMap = emptyMap()
        )
        assertEquals(LocalDate.of(2026, 9, 28), gridMon.first().date)
        assertEquals(DayOfWeek.MONDAY, gridMon.first().date.dayOfWeek)
        assertEquals(0, selectedRowMon)
        // October 1st is index 3 in the first row
        assertEquals(LocalDate.of(2026, 10, 1), gridMon[3].date)
        assertTrue(gridMon[3].isSelected)

        // Sunday start: Sunday = 0, Monday = 1, Tuesday = 2, Wednesday = 3, Thursday = 4.
        // Offset is 4. Grid start date should be Sep 27, 2026 (Sunday).
        val (gridSun, selectedRowSun) = CalendarCalculations.computeGridDays(
            currentMonth = month,
            selectedDate = selected,
            today = selected,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            dotsMap = emptyMap()
        )
        assertEquals(LocalDate.of(2026, 9, 27), gridSun.first().date)
        assertEquals(DayOfWeek.SUNDAY, gridSun.first().date.dayOfWeek)
        assertEquals(0, selectedRowSun)
        // October 1st is index 4 in the first row
        assertEquals(LocalDate.of(2026, 10, 1), gridSun[4].date)
        assertTrue(gridSun[4].isSelected)
    }
}
