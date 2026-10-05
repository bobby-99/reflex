package com.reflex.app

import com.reflex.app.data.Priority
import com.reflex.app.util.TaskParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class TaskParserComprehensiveTest {

    private val fixedZone = ZoneId.of("UTC")
    // Base clock: 2026-10-05 23:30:00 UTC (30 minutes before midnight)
    private val clockNearMidnight = Clock.fixed(
        Instant.parse("2026-10-05T23:30:00Z"),
        fixedZone
    )
    // Standard clock: 2026-10-05 10:00:00 UTC
    private val standardClock = Clock.fixed(
        Instant.parse("2026-10-05T10:00:00Z"),
        fixedZone
    )

    @Test
    fun testEmptyAndWhitespaceInput() {
        val empty = TaskParser.parse("", standardClock)
        assertEquals("", empty.title)
        assertNull(empty.dueDate)
        assertNull(empty.dueTime)
        assertEquals(Priority.NONE, empty.priority)

        val spaces = TaskParser.parse("     \n\t   ", standardClock)
        assertEquals("", spaces.title)
        assertNull(spaces.dueDate)
    }

    @Test
    fun test10000CharacterInput() {
        val longString = "A".repeat(10_000)
        val startTime = System.currentTimeMillis()
        val result = TaskParser.parse(longString, standardClock)
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(longString, result.title)
        assertEquals(Priority.NONE, result.priority)
        assertTrue("Parsing 10,000 chars took ${elapsed}ms, must be < 500ms", elapsed < 500)
    }

    @Test
    fun testExclamationInTheMiddleOfAWordNotTreatedAsPriority() {
        // "foo!!!bar" has exclamation inside letters -> NOT priority
        val result1 = TaskParser.parse("Fix bug in foo!!!bar urgently", standardClock)
        // Note: "urgently" is not in priority words, but "urgent" is. Here only checking foo!!!bar
        val result2 = TaskParser.parse("Check foo!!!bar module", standardClock)
        assertEquals(Priority.NONE, result2.priority)
        assertTrue(result2.title.contains("foo!!!bar"))

        // Single exclamation inside word: "file!name" or "test!case"
        val result3 = TaskParser.parse("Open file!name.txt", standardClock)
        assertEquals(Priority.NONE, result3.priority)
        assertTrue(result3.title.contains("file!name.txt"))

        // Boundary exclamation marks SHOULD be treated as priority
        val resultHigh = TaskParser.parse("Buy groceries !!!", standardClock)
        assertEquals(Priority.HIGH, resultHigh.priority)
        assertEquals("Buy groceries", resultHigh.title)

        val resultMed = TaskParser.parse("Call mom !!", standardClock)
        assertEquals(Priority.MEDIUM, resultMed.priority)
        assertEquals("Call mom", resultMed.title)

        val resultLow = TaskParser.parse("Read book !", standardClock)
        assertEquals(Priority.LOW, resultLow.priority)
        assertEquals("Read book", resultLow.title)
    }

    @Test
    fun testMultiplePrioritiesInOneStringResolvesToHighest() {
        // "urgent" (HIGH) and "low priority" (LOW) -> HIGH should win
        val result1 = TaskParser.parse("urgent submit low priority paperwork", standardClock)
        assertEquals(Priority.HIGH, result1.priority)

        // "p3" (LOW) and "p1" (HIGH) -> HIGH wins
        val result2 = TaskParser.parse("fix engine p3 p1", standardClock)
        assertEquals(Priority.HIGH, result2.priority)

        // "med priority" and "!!!" (HIGH) -> HIGH wins
        val result3 = TaskParser.parse("med priority update server !!!", standardClock)
        assertEquals(Priority.HIGH, result3.priority)

        // "p2" (MED) and "p3" (LOW) -> MED wins
        val result4 = TaskParser.parse("review code p3 p2", standardClock)
        assertEquals(Priority.MEDIUM, result4.priority)
    }

    @Test
    fun testIn90MinsAcrossMidnightWithFakeClock() {
        // clockNearMidnight is 2026-10-05 at 23:30:00
        // "in 90 mins" -> target time is 2026-10-06 at 01:00:00 (next day!)
        val result = TaskParser.parse("Take medicine in 90 mins", clockNearMidnight)
        assertEquals("Take medicine", result.title)
        assertNotNull(result.dueDate)
        assertNotNull(result.dueTime)

        val targetLocalDate = Instant.ofEpochMilli(result.dueDate!!).atZone(fixedZone).toLocalDate()
        val targetLocalTime = Instant.ofEpochMilli(result.dueTime!!).atZone(fixedZone).toLocalTime()

        assertEquals(LocalDate.of(2026, 10, 6), targetLocalDate)
        assertEquals(LocalTime.of(1, 0), targetLocalTime)
    }

    @Test
    fun test24HourVs12HourTimeParsing() {
        // 24-hour times
        val result24a = TaskParser.parse("Standup at 14:30 today", standardClock)
        assertEquals("Standup", result24a.title)
        assertNotNull(result24a.dueTime)
        assertEquals(LocalTime.of(14, 30), Instant.ofEpochMilli(result24a.dueTime!!).atZone(fixedZone).toLocalTime())

        val result24b = TaskParser.parse("Midnight shift 00:00", standardClock)
        assertNotNull(result24b.dueTime)
        assertEquals(LocalTime.of(0, 0), Instant.ofEpochMilli(result24b.dueTime!!).atZone(fixedZone).toLocalTime())

        val result24c = TaskParser.parse("Deploy system 23:59", standardClock)
        assertNotNull(result24c.dueTime)
        assertEquals(LocalTime.of(23, 59), Instant.ofEpochMilli(result24c.dueTime!!).atZone(fixedZone).toLocalTime())

        // 12-hour times with AM/PM
        val result12a = TaskParser.parse("Dentist appointment 8:15am", standardClock)
        assertEquals(LocalTime.of(8, 15), Instant.ofEpochMilli(result12a.dueTime!!).atZone(fixedZone).toLocalTime())

        val result12b = TaskParser.parse("Dinner party 7:45pm", standardClock)
        assertEquals(LocalTime.of(19, 45), Instant.ofEpochMilli(result12b.dueTime!!).atZone(fixedZone).toLocalTime())
    }

    @Test
    fun testMultipleDatesInOneStringPreservesExplicitDate() {
        // "Flight on 15 Aug booked today" should keep 15 Aug, not overwrite with today
        val result = TaskParser.parse("Flight on 15 Aug booked today", standardClock)
        assertNotNull(result.dueDate)
        val parsedDate = Instant.ofEpochMilli(result.dueDate!!).atZone(fixedZone).toLocalDate()
        assertEquals(8, parsedDate.monthValue)
        assertEquals(15, parsedDate.dayOfMonth)
    }

    @Test
    fun testMessyInputAndTypos() {
        // Repeated whitespace, punctuation, spoken forms
        val messy = TaskParser.parse("   clean    garage   at    5:00   p.  m.   tmrw   !!!   ", standardClock)
        assertEquals("clean garage", messy.title)
        assertEquals(Priority.HIGH, messy.priority)
        assertNotNull(messy.dueDate)
        assertNotNull(messy.dueTime)

        val tomorrow = LocalDate.of(2026, 10, 6)
        val parsedDate = Instant.ofEpochMilli(messy.dueDate!!).atZone(fixedZone).toLocalDate()
        assertEquals(tomorrow, parsedDate)

        val parsedTime = Instant.ofEpochMilli(messy.dueTime!!).atZone(fixedZone).toLocalTime()
        assertEquals(LocalTime.of(17, 0), parsedTime)
    }
}
