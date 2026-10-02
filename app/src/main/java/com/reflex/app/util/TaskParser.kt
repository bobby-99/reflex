package com.reflex.app.util

import com.reflex.app.data.MonthlyMode
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.RecurrenceUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ParsedTaskInput(
    val title: String,
    val dueDate: Long? = null,
    val dueTime: Long? = null,
    val priority: Priority = Priority.NONE,
    val isPastDate: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAY,
    val recurrenceDaysOfWeek: String? = null,
    val recurrenceMonthlyMode: MonthlyMode = MonthlyMode.SAME_DATE
)

object TaskParser {

    fun parse(input: String): ParsedTaskInput {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return ParsedTaskInput(title = "")
        }

        var workingText = trimmed
        var detectedPriority = Priority.NONE
        var detectedDate: LocalDate? = null
        var detectedTime: LocalTime? = null
        var isPast = false

        var detectedRecurrenceFreq = RecurrenceFrequency.NONE
        var detectedRecurrenceInterval = 1
        var detectedRecurrenceUnit = RecurrenceUnit.DAY
        var detectedRecurrenceDaysOfWeek: String? = null
        var detectedRecurrenceMonthlyMode = MonthlyMode.SAME_DATE

        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        val weekdaysLookup = mapOf(
            "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY, "thur" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY
        )

        // ── 0. RECURRENCE NATURAL PARSING (Trigger: 'repeat' or 'every') ──
        // A. Custom interval: "repeat every 2 days", "every 3 weeks repeat", "repeat every 2 months"
        val customRepeatRegex = Regex("(?i)\\b(?:repeat\\s+)?every\\s+(\\d+)\\s+(day|days|week|weeks|month|months|year|years)(?:\\s+repeat)?\\b")
        val customRepeatMatch = customRepeatRegex.find(workingText)
        if (customRepeatMatch != null) {
            val interval = customRepeatMatch.groupValues[1].toIntOrNull() ?: 1
            val unitStr = customRepeatMatch.groupValues[2].lowercase(Locale.ROOT)
            val unit = when {
                unitStr.startsWith("day") -> RecurrenceUnit.DAY
                unitStr.startsWith("week") -> RecurrenceUnit.WEEK
                unitStr.startsWith("month") -> RecurrenceUnit.MONTH
                unitStr.startsWith("year") -> RecurrenceUnit.YEAR
                else -> RecurrenceUnit.DAY
            }
            detectedRecurrenceFreq = RecurrenceFrequency.CUSTOM
            detectedRecurrenceInterval = interval
            detectedRecurrenceUnit = unit
            workingText = workingText.replace(customRepeatMatch.value, " ")
        }

        // B. Weekday list / specific day repeat: "repeat every tuesday", "sunday repeat", "every mon, wed, fri repeat"
        val dayRepeatRegex = Regex("(?i)\\b(?:repeat\\s+)?every\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)(?:\\s+repeat)?\\b")
        val dayRepeatMatch = dayRepeatRegex.find(workingText)
        if (dayRepeatMatch != null && detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
            val dayName = dayRepeatMatch.groupValues[1].lowercase(Locale.ROOT)
            val dow = weekdaysLookup[dayName]
            if (dow != null) {
                detectedRecurrenceFreq = RecurrenceFrequency.WEEKLY
                detectedRecurrenceDaysOfWeek = dow.name
                detectedDate = getNextOrSameDayOfWeek(today, dow)
                workingText = workingText.replace(dayRepeatMatch.value, " ")
            }
        }

        // C. "[weekday] repeat" / "repeat [weekday]" e.g. "sunday repeat trimming", "repeat sunday"
        val singleDayRepeatRegex = Regex("(?i)\\b(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\\s+repeat\\b|\\brepeat\\s+(?:on\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|tues|wed|thu|thur|thurs|fri|sat|sun)\\b")
        val singleDayRepeatMatch = singleDayRepeatRegex.find(workingText)
        if (singleDayRepeatMatch != null && detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
            val dayName = (singleDayRepeatMatch.groupValues[1].ifEmpty { singleDayRepeatMatch.groupValues[2] }).lowercase(Locale.ROOT)
            val dow = weekdaysLookup[dayName]
            if (dow != null) {
                detectedRecurrenceFreq = RecurrenceFrequency.WEEKLY
                detectedRecurrenceDaysOfWeek = dow.name
                detectedDate = getNextOrSameDayOfWeek(today, dow)
                workingText = workingText.replace(singleDayRepeatMatch.value, " ")
            }
        }

        // D. Monthly day repeat: "every month 10th day Pay bill repeat", "repeat every month 15th", "every month 10th repeat"
        val monthlyDayRepeatRegex = Regex("(?i)\\b(?:repeat\\s+)?every\\s+month\\s+(?:on\\s+(?:the\\s+)?)?(\\d{1,2})(?:st|nd|rd|th)?(?:\\s+day)?(?:\\s+repeat)?\\b")
        val monthlyDayMatch = monthlyDayRepeatRegex.find(workingText)
        if (monthlyDayMatch != null && detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
            val day = monthlyDayMatch.groupValues[1].toIntOrNull()
            if (day != null && day in 1..31) {
                val targetDay = day.coerceIn(1, 28)
                detectedDate = if (today.dayOfMonth <= targetDay) {
                    today.withDayOfMonth(targetDay)
                } else {
                    today.plusMonths(1).withDayOfMonth(targetDay)
                }
                detectedRecurrenceFreq = RecurrenceFrequency.MONTHLY
                detectedRecurrenceInterval = 1
                detectedRecurrenceUnit = RecurrenceUnit.MONTH
                detectedRecurrenceMonthlyMode = MonthlyMode.SAME_DATE
                workingText = workingText.replace(monthlyDayMatch.value, " ")
            }
        }

        // E. General Monthly: "every month repeat", "repeat every month", "monthly repeat", "repeat monthly"
        val generalMonthlyRegex = Regex("(?i)\\b(?:repeat\\s+)?every\\s+month(?:\\s+repeat)?\\b|\\b(?:monthly\\s+repeat|repeat\\s+monthly)\\b")
        val generalMonthlyMatch = generalMonthlyRegex.find(workingText)
        if (generalMonthlyMatch != null && detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
            detectedRecurrenceFreq = RecurrenceFrequency.MONTHLY
            detectedRecurrenceInterval = 1
            detectedRecurrenceUnit = RecurrenceUnit.MONTH
            detectedRecurrenceMonthlyMode = MonthlyMode.SAME_DATE
            if (detectedDate == null) detectedDate = today
            workingText = workingText.replace(generalMonthlyMatch.value, " ")
        }

        // F. Weekdays: "every weekday repeat", "repeat every weekday", "weekdays repeat"
        val weekdaysRepeatRegex = Regex("(?i)\\b(?:repeat\\s+)?every\\s+weekday(?:s)?(?:\\s+repeat)?\\b|\\b(?:weekdays\\s+repeat|repeat\\s+weekdays)\\b")
        val weekdaysRepeatMatch = weekdaysRepeatRegex.find(workingText)
        if (weekdaysRepeatMatch != null && detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
            detectedRecurrenceFreq = RecurrenceFrequency.WEEKLY
            detectedRecurrenceDaysOfWeek = "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
            if (detectedDate == null) detectedDate = today
            workingText = workingText.replace(weekdaysRepeatMatch.value, " ")
        }

        // G. Daily: "every day 9pm ... repeat", "repeat every day", "everyday repeat", "daily repeat", "repeat daily"
        val dailyRepeatRegex = Regex("(?i)\\b(?:repeat\\s+)?every\\s*day(?:\\s+repeat)?\\b|\\b(?:daily\\s+repeat|repeat\\s+daily)\\b")
        val dailyRepeatMatch = dailyRepeatRegex.find(workingText)
        if (dailyRepeatMatch != null && detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
            detectedRecurrenceFreq = RecurrenceFrequency.DAILY
            detectedRecurrenceInterval = 1
            detectedRecurrenceUnit = RecurrenceUnit.DAY
            if (detectedDate == null) detectedDate = today
            workingText = workingText.replace(dailyRepeatMatch.value, " ")
        }

        // H. General Weekly: "every week repeat", "repeat every week", "weekly repeat"
        val weeklyRepeatRegex = Regex("(?i)\\b(?:repeat\\s+)?every\\s+week(?:\\s+repeat)?\\b|\\b(?:weekly\\s+repeat|repeat\\s+weekly)\\b")
        val weeklyRepeatMatch = weeklyRepeatRegex.find(workingText)
        if (weeklyRepeatMatch != null && detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
            detectedRecurrenceFreq = RecurrenceFrequency.WEEKLY
            detectedRecurrenceInterval = 1
            detectedRecurrenceUnit = RecurrenceUnit.WEEK
            if (detectedDate == null) detectedDate = today
            workingText = workingText.replace(weeklyRepeatMatch.value, " ")
        }

        // I. Isolated "repeat" or "repeating" or "repeats" word (e.g. "shave 9pm repeat", "Sunday repeat trimming")
        val isolatedRepeatRegex = Regex("(?i)\\b(?:repeat|repeating|repeats)\\b")
        if (isolatedRepeatRegex.containsMatchIn(workingText)) {
            if (detectedRecurrenceFreq == RecurrenceFrequency.NONE) {
                detectedRecurrenceFreq = RecurrenceFrequency.DAILY
                detectedRecurrenceInterval = 1
                if (detectedDate == null) detectedDate = today
            }
            workingText = workingText.replace(isolatedRepeatRegex, " ")
        }

        // 0.5 Speech Recognition & Spoken Language Pre-Normalization
        // Normalize abbreviations first
        workingText = workingText
            .replace(Regex("\\bevng?\\b", RegexOption.IGNORE_CASE), "evening")
            .replace(Regex("\\beve\\b", RegexOption.IGNORE_CASE), "evening")
            .replace(Regex("\\bevn\\b", RegexOption.IGNORE_CASE), "evening")
            .replace(Regex("\\bmrng?\\b", RegexOption.IGNORE_CASE), "morning")
            .replace(Regex("\\bmrn\\b", RegexOption.IGNORE_CASE), "morning")
            .replace(Regex("\\baftn?o?o?n?\\b", RegexOption.IGNORE_CASE), "afternoon")
            .replace(Regex("\\bmidday\\b", RegexOption.IGNORE_CASE), "noon")
            .replace(Regex("\\bngt\\b", RegexOption.IGNORE_CASE), "night")
            .replace(Regex("\\btmrw\\b", RegexOption.IGNORE_CASE), "tomorrow")
            .replace(Regex("\\btmr\\b", RegexOption.IGNORE_CASE), "tomorrow")
            .replace(Regex("\\b2day\\b", RegexOption.IGNORE_CASE), "today")

        // Normalize dotted/spaced AM/PM forms ("2 p.m.", "2 p. m.", "2 p m", "10 a.m.", "10 a. m.") into "2pm" / "10am"
        workingText = workingText.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s*(?:p\\.?\\s*m\\.?|p\\s+m)(?=\\s|$|\\b)")) { match ->
            "${match.groupValues[1]}pm"
        }
        workingText = workingText.replace(Regex("(?i)\\b(\\d{1,2}(?::\\d{2})?)\\s*(?:a\\.?\\s*m\\.?|a\\s+m)(?=\\s|$|\\b)")) { match ->
            "${match.groupValues[1]}am"
        }

        // Normalize "o'clock", "o clock", "oclock"
        workingText = workingText.replace(Regex("(?i)\\b(\\d{1,2})\\s*(?:o['’]?\\s*clock)(?=\\s|$|\\b)")) { match ->
            "${match.groupValues[1]}:00"
        }

        // Strip spoken prepositions attached to dates/times ("at 2pm" -> "2pm", "by today" -> "today", "for tomorrow" -> "tomorrow")
        workingText = workingText.replace(Regex("(?i)\\b(?:at|by|for|on)\\s+(\\d{1,2}(?::\\d{2})?(?:am|pm)?|today|2day|tomorrow|tmrw|tmr|tonight|yesterday|monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)(?=\\s|$|\\b)")) { match ->
            match.groupValues[1]
        }

        // 1. Priority Parsing
        when {
            workingText.contains("!!!") -> {
                detectedPriority = Priority.HIGH
                workingText = workingText.replace("!!!", " ")
            }
            workingText.contains("!!") -> {
                detectedPriority = Priority.MEDIUM
                workingText = workingText.replace("!!", " ")
            }
            workingText.contains("!") -> {
                detectedPriority = Priority.LOW
                workingText = workingText.replace("!", " ")
            }
        }

        var lowerText = workingText.lowercase(Locale.ROOT)
        if (lowerText.contains("urgent") || lowerText.contains("asap") || lowerText.contains("important") || lowerText.contains("top priority")) {
            detectedPriority = Priority.HIGH
            workingText = replaceIgnoreCase(workingText, "urgent", " ")
            workingText = replaceIgnoreCase(workingText, "asap", " ")
            workingText = replaceIgnoreCase(workingText, "important", " ")
            workingText = replaceIgnoreCase(workingText, "top priority", " ")
            lowerText = workingText.lowercase(Locale.ROOT)
        }

        if (lowerText.contains("high priority") || lowerText.contains("priority high") || lowerText.contains("p1")) {
            detectedPriority = Priority.HIGH
            workingText = replaceIgnoreCase(workingText, "high priority", " ")
            workingText = replaceIgnoreCase(workingText, "priority high", " ")
            workingText = replaceIgnoreCase(workingText, "p1", " ")
        } else if (lowerText.contains("medium priority") || lowerText.contains("med priority") || lowerText.contains("priority medium") || lowerText.contains("p2")) {
            detectedPriority = Priority.MEDIUM
            workingText = replaceIgnoreCase(workingText, "medium priority", " ")
            workingText = replaceIgnoreCase(workingText, "med priority", " ")
            workingText = replaceIgnoreCase(workingText, "priority medium", " ")
            workingText = replaceIgnoreCase(workingText, "p2", " ")
        } else if (lowerText.contains("low priority") || lowerText.contains("priority low") || lowerText.contains("p3")) {
            detectedPriority = Priority.LOW
            workingText = replaceIgnoreCase(workingText, "low priority", " ")
            workingText = replaceIgnoreCase(workingText, "priority low", " ")
            workingText = replaceIgnoreCase(workingText, "p3", " ")
        }

        // 2. Relative Duration Parsing ("in 5min", "45min", "1h10min", "1hr4min", "2 hours", "in 45 mins", "10m")
        val durationRegexCombined = Regex("(?i)\\b(?:in\\s+)?(?:(\\d+)\\s*(?:h|hr|hrs|hour|hours))?\\s*(?:(\\d+)\\s*(?:m|min|mins|minute|minutes))\\b")
        val durationRegexHoursOnly = Regex("(?i)\\b(?:in\\s+)?(\\d+)\\s*(?:h|hr|hrs|hour|hours)\\b")

        val durationMatch = durationRegexCombined.find(workingText) ?: durationRegexHoursOnly.find(workingText)
        if (durationMatch != null) {
            val hours = if (durationMatch.groupValues.size > 1) durationMatch.groupValues[1].toIntOrNull() ?: 0 else 0
            val minutes = if (durationMatch.groupValues.size > 2) durationMatch.groupValues[2].toIntOrNull() ?: 0 else 0
            val totalHours = if (durationMatch.groupValues.size == 2 && (durationMatch.value.contains("h", ignoreCase = true) || durationMatch.value.contains("hour", ignoreCase = true))) {
                durationMatch.groupValues[1].toIntOrNull() ?: 0
            } else hours

            if (totalHours > 0 || minutes > 0) {
                val targetDateTime = now.plusHours(totalHours.toLong()).plusMinutes(minutes.toLong())
                detectedDate = targetDateTime.toLocalDate()
                detectedTime = targetDateTime.toLocalTime().withSecond(0).withNano(0)
                workingText = workingText.replace(durationMatch.value, " ")
            }
        }

        val inDaysRegex = Regex("(?i)\\bin\\s+(\\d+)\\s+day(?:s)?\\b")
        val matchInDays = inDaysRegex.find(workingText)
        if (matchInDays != null) {
            val days = matchInDays.groupValues[1].toLongOrNull() ?: 0
            detectedDate = today.plusDays(days)
            workingText = workingText.replace(matchInDays.value, " ")
        }

        val inWeeksRegex = Regex("(?i)\\bin\\s+(\\d+)\\s+week(?:s)?\\b")
        val matchInWeeks = inWeeksRegex.find(workingText)
        if (matchInWeeks != null) {
            val weeks = matchInWeeks.groupValues[1].toLongOrNull() ?: 0
            detectedDate = today.plusWeeks(weeks)
            workingText = workingText.replace(matchInWeeks.value, " ")
        }

        // 2.5 Explicit Calendar Date Parsing (e.g. "12 Aug", "17th Sept", "20 jan 2027", "Aug 12", "Sept 17th", "20th of Jan")
        val monthsRegexPart = "(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|sept|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"

        // Pattern A: Day Month [Year] e.g. "12 Aug", "17th Sept", "20 jan 2027", "20th of Jan"
        val dayMonthYearRegex = Regex("(?i)\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(?:of\\s+)?($monthsRegexPart)(?:\\s+(\\d{4}))?\\b")
        val matchDMY = dayMonthYearRegex.find(workingText)

        if (matchDMY != null) {
            val day = matchDMY.groupValues[1].toIntOrNull()
            val monthStr = matchDMY.groupValues[2]
            val yearStr = matchDMY.groupValues[3]
            val month = parseMonthString(monthStr)

            if (day != null && month != null && day in 1..31) {
                val year = yearStr.toIntOrNull() ?: run {
                    val candidate = LocalDate.of(today.year, month, day.coerceAtMost(maxDaysInMonth(today.year, month)))
                    if (candidate.isBefore(today)) today.year + 1 else today.year
                }
                val maxDays = maxDaysInMonth(year, month)
                detectedDate = LocalDate.of(year, month, day.coerceAtMost(maxDays))
                workingText = workingText.replace(matchDMY.value, " ")
            }
        } else {
            // Pattern B: Month Day [Year] e.g. "Aug 12", "Sept 17th", "Jan 20 2027"
            val monthDayYearRegex = Regex("(?i)\\b($monthsRegexPart)\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s+(\\d{4}))?\\b")
            val matchMDY = monthDayYearRegex.find(workingText)
            if (matchMDY != null) {
                val monthStr = matchMDY.groupValues[1]
                val day = matchMDY.groupValues[2].toIntOrNull()
                val yearStr = matchMDY.groupValues[3]
                val month = parseMonthString(monthStr)

                if (day != null && month != null && day in 1..31) {
                    val year = yearStr.toIntOrNull() ?: run {
                        val candidate = LocalDate.of(today.year, month, day.coerceAtMost(maxDaysInMonth(today.year, month)))
                        if (candidate.isBefore(today)) today.year + 1 else today.year
                    }
                    val maxDays = maxDaysInMonth(year, month)
                    detectedDate = LocalDate.of(year, month, day.coerceAtMost(maxDays))
                    workingText = workingText.replace(matchMDY.value, " ")
                }
            }
        }

        // 3. Multi-word phrase matching & explicit time regexes
        var currentLower = workingText.lowercase(Locale.ROOT)

        if (currentLower.contains("end of day") || currentLower.contains("eod")) {
            detectedTime = LocalTime.of(18, 0) // 6:00 PM
            if (currentLower.contains("eod")) {
                detectedDate = today
                workingText = replaceIgnoreCase(workingText, "eod", " ")
            } else {
                detectedDate = today
                workingText = replaceIgnoreCase(workingText, "end of day", " ")
            }
            currentLower = workingText.lowercase(Locale.ROOT)
        }
        if (currentLower.contains("eow")) {
            detectedTime = LocalTime.of(18, 0) // 6:00 PM
            detectedDate = getNextOrSameDayOfWeek(today, DayOfWeek.FRIDAY)
            workingText = replaceIgnoreCase(workingText, "eow", " ")
            currentLower = workingText.lowercase(Locale.ROOT)
        }
        if (currentLower.contains("next week")) {
            detectedDate = today.plusWeeks(1)
            workingText = replaceIgnoreCase(workingText, "next week", " ")
            currentLower = workingText.lowercase(Locale.ROOT)
        }

        // Check "next [weekday]" phrases
        val weekdaysMap = mapOf(
            "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY, "thur" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY
        )

        for ((name, dayOfWeek) in weekdaysMap) {
            val nextPhrase = "next $name"
            if (currentLower.contains(nextPhrase)) {
                detectedDate = today.with(TemporalAdjusters.next(dayOfWeek)).plusWeeks(1)
                workingText = replaceIgnoreCase(workingText, nextPhrase, " ")
                currentLower = workingText.lowercase(Locale.ROOT)
                break
            }
            
            val thisPhrase = "this $name"
            if (currentLower.contains(thisPhrase)) {
                detectedDate = today.with(TemporalAdjusters.nextOrSame(dayOfWeek))
                workingText = replaceIgnoreCase(workingText, thisPhrase, " ")
                currentLower = workingText.lowercase(Locale.ROOT)
                break
            }
        }

        // Space-separated time: "10 30 pm", "8 45am", "10 30 PM"
        val spacedTimeRegex = Regex("""\b(\d{1,2})\s+(\d{2})\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
        val spacedTimeMatch = spacedTimeRegex.find(workingText)
        if (spacedTimeMatch != null && detectedTime == null) {
            val hour = spacedTimeMatch.groupValues[1].toIntOrNull() ?: 0
            val minute = spacedTimeMatch.groupValues[2].toIntOrNull() ?: 0
            val ampm = spacedTimeMatch.groupValues[3].lowercase(Locale.ROOT)
            var adjustedHour = hour
            if (ampm == "pm" && hour < 12) adjustedHour += 12
            if (ampm == "am" && hour == 12) adjustedHour = 0
            if (adjustedHour in 0..23 && minute in 0..59) {
                detectedTime = LocalTime.of(adjustedHour, minute)
                workingText = workingText.replace(spacedTimeMatch.value, " ")
            }
        }

        // Mid-sentence explicit 12-hr time regex (e.g. "10:46am", "8pm", "3:15 pm")
        val explicit12HrRegex = Regex("(?i)\\b(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b")
        val match12Hr = explicit12HrRegex.find(workingText)
        if (match12Hr != null) {
            val hour = match12Hr.groupValues[1].toIntOrNull() ?: 0
            val minute = match12Hr.groupValues[2].toIntOrNull() ?: 0
            val ampm = match12Hr.groupValues[3].lowercase(Locale.ROOT)
            var adjustedHour = hour
            if (ampm == "pm" && hour < 12) adjustedHour += 12
            if (ampm == "am" && hour == 12) adjustedHour = 0
            if (adjustedHour in 0..23 && minute in 0..59) {
                detectedTime = LocalTime.of(adjustedHour, minute)
                workingText = workingText.replace(match12Hr.value, " ")
            }
        }

        // 4. Token-by-token parsing for remaining date/time terms
        val tokens = workingText.split("\\s+".toRegex())
        val remainingTokens = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val cleanToken = token.lowercase(Locale.ROOT)

            when (cleanToken) {
                "today", "2day" -> {
                    detectedDate = today
                }
                "tomorrow", "tmrw", "tmr" -> {
                    detectedDate = today.plusDays(1)
                }
                "tonight" -> {
                    detectedDate = today
                    if (detectedTime == null) {
                        detectedTime = TimeDefaults.NIGHT
                    }
                }
                "yesterday" -> {
                    detectedDate = today.minusDays(1)
                    isPast = true
                }
                in weekdaysMap.keys -> {
                    val dayOfWeek = weekdaysMap[cleanToken]!!
                    detectedDate = getNextOrSameDayOfWeek(today, dayOfWeek)
                }
                in TaskParserLookup.TIME_WORDS.keys -> {
                    if (detectedTime == null) {
                        detectedTime = TaskParserLookup.TIME_WORDS[cleanToken]
                    }
                }
                else -> {
                    val parsedTime = parseTimeToken(token, if (i + 1 < tokens.size) tokens[i + 1] else null)
                    if (parsedTime != null) {
                        if (detectedTime == null) {
                            detectedTime = parsedTime.time
                        }
                        if (parsedTime.consumedTwoTokens) {
                            i++
                        }
                    } else {
                        remainingTokens.add(token)
                    }
                }
            }
            i++
        }

        val cleanedTitle = remainingTokens.joinToString(" ").replace("\\s+".toRegex(), " ").trim()
        val finalTitle = cleanedTitle.ifEmpty { trimmed }

        val effectiveDate = detectedDate ?: if (detectedTime != null) today else null
        val dueDateMillis = effectiveDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        val dueTimeMillis = if (detectedTime != null) {
            val dateToUse = effectiveDate ?: today
            LocalDateTime.of(dateToUse, detectedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else null

        return ParsedTaskInput(
            title = finalTitle,
            dueDate = dueDateMillis,
            dueTime = dueTimeMillis,
            priority = detectedPriority,
            isPastDate = isPast,
            recurrenceFrequency = detectedRecurrenceFreq,
            recurrenceInterval = detectedRecurrenceInterval,
            recurrenceUnit = detectedRecurrenceUnit,
            recurrenceDaysOfWeek = detectedRecurrenceDaysOfWeek,
            recurrenceMonthlyMode = detectedRecurrenceMonthlyMode
        )
    }

    private fun getNextOrSameDayOfWeek(today: LocalDate, targetDay: DayOfWeek): LocalDate {
        return if (today.dayOfWeek == targetDay) {
            today.plusWeeks(1)
        } else {
            today.with(TemporalAdjusters.next(targetDay))
        }
    }

    private data class TimeParseResult(val time: LocalTime, val consumedTwoTokens: Boolean)

    private fun parseTimeToken(token1: String, token2: String?): TimeParseResult? {
        val t1 = token1.lowercase(Locale.ROOT)
        val t2 = token2?.lowercase(Locale.ROOT)

        val singleTimePattern = Regex("^(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)$")
        val matchSingle = singleTimePattern.find(t1)
        if (matchSingle != null) {
            val hour = matchSingle.groupValues[1].toIntOrNull() ?: return null
            val minute = matchSingle.groupValues[2].toIntOrNull() ?: 0
            val ampm = matchSingle.groupValues[3]
            var adjustedHour = hour
            if (ampm == "pm" && hour < 12) adjustedHour += 12
            if (ampm == "am" && hour == 12) adjustedHour = 0
            if (adjustedHour in 0..23 && minute in 0..59) {
                return TimeParseResult(LocalTime.of(adjustedHour, minute), consumedTwoTokens = false)
            }
        }

        val time24Pattern = Regex("^([01]?\\d|2[0-3]):([0-5]\\d)$")
        val match24 = time24Pattern.find(t1)
        if (match24 != null) {
            val hour = match24.groupValues[1].toInt()
            val minute = match24.groupValues[2].toInt()
            return TimeParseResult(LocalTime.of(hour, minute), consumedTwoTokens = false)
        }

        if (t2 == "am" || t2 == "pm") {
            val hourMinPattern = Regex("^(\\d{1,2})(?::(\\d{2}))?$")
            val matchHM = hourMinPattern.find(t1)
            if (matchHM != null) {
                val hour = matchHM.groupValues[1].toIntOrNull() ?: return null
                val minute = matchHM.groupValues[2].toIntOrNull() ?: 0
                var adjustedHour = hour
                if (t2 == "pm" && hour < 12) adjustedHour += 12
                if (t2 == "am" && hour == 12) adjustedHour = 0
                if (adjustedHour in 0..23 && minute in 0..59) {
                    return TimeParseResult(LocalTime.of(adjustedHour, minute), consumedTwoTokens = true)
                }
            }
        }

        return null
    }

    private fun parseMonthString(monthStr: String): Int? {
        val m = monthStr.lowercase(Locale.ROOT)
        return when {
            m.startsWith("jan") -> 1
            m.startsWith("feb") -> 2
            m.startsWith("mar") -> 3
            m.startsWith("apr") -> 4
            m.startsWith("may") -> 5
            m.startsWith("jun") -> 6
            m.startsWith("jul") -> 7
            m.startsWith("aug") -> 8
            m.startsWith("sep") -> 9
            m.startsWith("oct") -> 10
            m.startsWith("nov") -> 11
            m.startsWith("dec") -> 12
            else -> null
        }
    }

    private fun maxDaysInMonth(year: Int, month: Int): Int {
        return java.time.YearMonth.of(year, month).lengthOfMonth()
    }

    private fun replaceIgnoreCase(text: String, target: String, replacement: String): String {
        return text.replace(Regex("(?i)" + Regex.escape(target)), replacement)
    }
}
