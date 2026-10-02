package com.reflex.app

import com.reflex.app.data.Priority
import com.reflex.app.util.TaskParser
import com.reflex.app.util.TimeDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class TaskParserTest {

    @Test
    fun testSubmitReportTomorrow5pmHighPriority() {
        val result = TaskParser.parse("Submit report tomorrow 5pm high priority")
        assertEquals("Submit report", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)
        assertEquals(Priority.HIGH, result.priority)
    }

    @Test
    fun testCallMomFridayMediumPriority() {
        val result = TaskParser.parse("Call mom friday !!")
        assertEquals("Call mom", result.title)
        assertNotNull(result.dueDate)
        assertNull(result.dueTime)
        assertEquals(Priority.MEDIUM, result.priority)
    }

    @Test
    fun testBuyGroceries() {
        val result = TaskParser.parse("Buy groceries")
        assertEquals("Buy groceries", result.title)
        assertNull(result.dueDate)
        assertNull(result.dueTime)
        assertEquals(Priority.NONE, result.priority)
    }

    @Test
    fun testRelativeDayTerms() {
        val tmrwResult = TaskParser.parse("Water plants tmrw")
        assertEquals("Water plants", tmrwResult.title)
        assertNotNull(tmrwResult.dueDate)

        val tmrResult = TaskParser.parse("Feed cat tmr")
        assertEquals("Feed cat", tmrResult.title)
        assertNotNull(tmrResult.dueDate)

        val todayResult = TaskParser.parse("Gym 2day")
        assertEquals("Gym", todayResult.title)
        assertNotNull(todayResult.dueDate)

        val yesterdayResult = TaskParser.parse("Read book yesterday")
        assertEquals("Read book", yesterdayResult.title)
        assertTrue(yesterdayResult.isPastDate)

        val nextWeekResult = TaskParser.parse("Project kickoff next week")
        assertEquals("Project kickoff", nextWeekResult.title)
        assertNotNull(nextWeekResult.dueDate)
    }

    @Test
    fun testTimeOfDayTerms() {
        val mrngResult = TaskParser.parse("Yoga mrng")
        assertEquals("Yoga", mrngResult.title)
        assertNotNull(mrngResult.dueTime)

        val noonResult = TaskParser.parse("Lunch noon")
        assertEquals("Lunch", noonResult.title)
        assertNotNull(noonResult.dueTime)

        val afternoonResult = TaskParser.parse("Coffee afternoon")
        assertEquals("Coffee", afternoonResult.title)
        assertNotNull(afternoonResult.dueTime)

        val niteResult = TaskParser.parse("Stretching nite")
        assertEquals("Stretching", niteResult.title)
        assertNotNull(niteResult.dueTime)

        val midnightResult = TaskParser.parse("Backup database midnight")
        assertEquals("Backup database", midnightResult.title)
        assertNotNull(midnightResult.dueTime)
    }

    @Test
    fun testTeamSyncTmrwMrng() {
        val result = TaskParser.parse("team sync tmrw mrng !!")
        assertEquals("team sync", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)
        assertEquals(Priority.MEDIUM, result.priority)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(TimeDefaults.MORNING, localTime)
    }

    @Test
    fun testFinishDeckEodHighPriority() {
        val result = TaskParser.parse("finish deck eod high priority")
        assertEquals("finish deck", result.title)
        assertNotNull(result.dueTime)
        assertEquals(Priority.HIGH, result.priority)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(TimeDefaults.EOD, localTime)
    }

    @Test
    fun testDurationIn5Min() {
        val result = TaskParser.parse("call mom in 5min")
        assertEquals("call mom", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)
    }

    @Test
    fun testDurationIn1Hr4Min() {
        val result = TaskParser.parse("finish slides in 1hr4min")
        assertEquals("finish slides", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)
    }

    @Test
    fun testTeamLunchTodayEvening() {
        val result = TaskParser.parse("team lunch today evening")
        assertEquals("team lunch", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(TimeDefaults.EVENING, localTime)
    }

    @Test
    fun testCallMom1046Am() {
        val result = TaskParser.parse("call mom 10:46am")
        assertEquals("call mom", result.title)
        assertNotNull(result.dueTime)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(LocalTime.of(10, 46), localTime)
    }

    @Test
    fun testPackBag8Pm() {
        val result = TaskParser.parse("pack bag 8pm")
        assertEquals("pack bag", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(LocalTime.of(20, 0), localTime)
    }

    @Test
    fun testTimeOnlyAssumesToday() {
        val result3pm = TaskParser.parse("call mom 3pm")
        assertEquals("call mom", result3pm.title)
        assertNotNull(result3pm.dueDate)
        assertNotNull(result3pm.dueTime)

        val resultEvng = TaskParser.parse("pack bag evng")
        assertEquals("pack bag", resultEvng.title)
        assertNotNull(resultEvng.dueDate)
        assertNotNull(resultEvng.dueTime)

        val resultMrng = TaskParser.parse("mrng schedule diet")
        assertEquals("schedule diet", resultMrng.title)
        assertNotNull(resultMrng.dueDate)
        assertNotNull(resultMrng.dueTime)
    }

    @Test
    fun testSpokenCallMom2pmToday() {
        val result = TaskParser.parse("call mom 2 p.m. today")
        assertEquals("call mom", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(LocalTime.of(14, 0), localTime)
    }

    @Test
    fun testSpokenDottedAmPmAndUrgent() {
        val result = TaskParser.parse("pay bill at 5:30 p. m. tomorrow urgent")
        assertEquals("pay bill", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)
        assertEquals(Priority.HIGH, result.priority)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(LocalTime.of(17, 30), localTime)
    }

    @Test
    fun testSpokenOClock() {
        val result = TaskParser.parse("doctor appointment at 10 o clock today")
        assertEquals("doctor appointment", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)

        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(LocalTime.of(10, 0), localTime)
    }

    @Test
    fun testExplicitCalendarDates() {
        val augResult = TaskParser.parse("Submit report by 12 Aug")
        assertEquals("Submit report", augResult.title)
        assertNotNull(augResult.dueDate)

        val aug12thResult = TaskParser.parse("Buy milk 12th aug")
        assertEquals("Buy milk", aug12thResult.title)
        assertNotNull(aug12thResult.dueDate)

        val septResult = TaskParser.parse("Call doctor 17th Sept")
        assertEquals("Call doctor", septResult.title)
        assertNotNull(septResult.dueDate)

        val jan2027Result = TaskParser.parse("Flight to Paris 20 jan 2027 at 5pm !!!")
        assertEquals("Flight to Paris", jan2027Result.title)
        assertNotNull(jan2027Result.dueDate)
        assertNotNull(jan2027Result.dueTime)
        assertEquals(Priority.HIGH, jan2027Result.priority)

        val localDate = Instant.ofEpochMilli(jan2027Result.dueDate!!).atZone(ZoneId.systemDefault()).toLocalDate()
        assertEquals(2027, localDate.year)
        assertEquals(1, localDate.monthValue)
        assertEquals(20, localDate.dayOfMonth)
    }

    @Test
    fun testSpaceSeparatedTime() {
        val result = TaskParser.parse("team meeting 10 30 pm")
        assertEquals("team meeting", result.title)
        assertNotNull(result.dueTime)
        val localTime = Instant.ofEpochMilli(result.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(LocalTime.of(22, 30), localTime)

        val resultMorning = TaskParser.parse("gym 8 45 am")
        assertEquals("gym", resultMorning.title)
        assertNotNull(resultMorning.dueTime)
        val morningTime = Instant.ofEpochMilli(resultMorning.dueTime!!).atZone(ZoneId.systemDefault()).toLocalTime()
        assertEquals(LocalTime.of(8, 45), morningTime)
    }

    @Test
    fun testInXDaysAndWeeks() {
        val resultDays = TaskParser.parse("Dentist appointment in 5 days")
        assertEquals("Dentist appointment", resultDays.title)
        assertNotNull(resultDays.dueDate)

        val resultWeeks = TaskParser.parse("Renew passport in 2 weeks")
        assertEquals("Renew passport", resultWeeks.title)
        assertNotNull(resultWeeks.dueDate)
    }

    @Test
    fun testThisWeekday() {
        val result = TaskParser.parse("Submit tax this friday")
        assertEquals("Submit tax", result.title)
        assertNotNull(result.dueDate)
    }

    @Test
    fun testRelativeDurationsWithoutIn() {
        val result1 = TaskParser.parse("45min call mom")
        assertEquals("call mom", result1.title)
        assertNotNull(result1.dueDate)
        assertNotNull(result1.dueTime)

        val result2 = TaskParser.parse("1h10min switch off oven")
        assertEquals("switch off oven", result2.title)
        assertNotNull(result2.dueDate)
        assertNotNull(result2.dueTime)
    }

    @Test
    fun testRecurrenceNaturalParsing() {
        // Example 1: repeat every tuesday shave 9pm
        val result1 = TaskParser.parse("repeat every tuesday shave 9pm")
        assertEquals("shave", result1.title)
        assertNotNull(result1.dueDate)
        assertNotNull(result1.dueTime)
        assertEquals(com.reflex.app.data.RecurrenceFrequency.WEEKLY, result1.recurrenceFrequency)
        assertEquals("TUESDAY", result1.recurrenceDaysOfWeek)

        // Example 2: sunday repeat trimming
        val result2 = TaskParser.parse("sunday repeat trimming")
        assertEquals("trimming", result2.title)
        assertNotNull(result2.dueDate)
        assertEquals(com.reflex.app.data.RecurrenceFrequency.WEEKLY, result2.recurrenceFrequency)
        assertEquals("SUNDAY", result2.recurrenceDaysOfWeek)

        // Example 3: every month 10th day Pay bill repeat
        val result3 = TaskParser.parse("every month 10th day Pay bill repeat")
        assertEquals("Pay bill", result3.title)
        assertNotNull(result3.dueDate)
        assertEquals(com.reflex.app.data.RecurrenceFrequency.MONTHLY, result3.recurrenceFrequency)

        // Example 4: every day 9pm Brush + clean sink repeat
        val result4 = TaskParser.parse("every day 9pm Brush + clean sink repeat")
        assertEquals("Brush + clean sink", result4.title)
        assertNotNull(result4.dueTime)
        assertEquals(com.reflex.app.data.RecurrenceFrequency.DAILY, result4.recurrenceFrequency)
    }
}
