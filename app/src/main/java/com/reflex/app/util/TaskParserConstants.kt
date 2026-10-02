package com.reflex.app.util

import com.reflex.app.data.Priority
import java.time.LocalTime

object TimeDefaults {
    val MORNING: LocalTime = LocalTime.of(8, 0)
    val NOON: LocalTime = LocalTime.of(12, 30)
    val AFTERNOON: LocalTime = LocalTime.of(15, 0)
    val EVENING: LocalTime = LocalTime.of(16, 0)
    val EOD: LocalTime = LocalTime.of(18, 0)
    val NIGHT: LocalTime = LocalTime.of(20, 0)
    val MIDNIGHT: LocalTime = LocalTime.of(0, 0)

    val DEFAULT_CALENDAR_QUICK_ADD_TIME: LocalTime = LocalTime.of(9, 0)
}

object TaskParserLookup {
    val TIME_WORDS: Map<String, LocalTime> = mapOf(
        "morning" to TimeDefaults.MORNING,
        "mrng" to TimeDefaults.MORNING,
        "mrn" to TimeDefaults.MORNING,

        "noon" to TimeDefaults.NOON,
        "midday" to TimeDefaults.NOON,

        "afternoon" to TimeDefaults.AFTERNOON,

        "evening" to TimeDefaults.EVENING,
        "evng" to TimeDefaults.EVENING,
        "eve" to TimeDefaults.EVENING,
        "evn" to TimeDefaults.EVENING,

        "eod" to TimeDefaults.EOD,
        "end of day" to TimeDefaults.EOD,

        "night" to TimeDefaults.NIGHT,
        "nite" to TimeDefaults.NIGHT,
        "tonight" to TimeDefaults.NIGHT,

        "midnight" to TimeDefaults.MIDNIGHT
    )
}

data class QuickAddExample(
    val exampleText: String,
    val expectedTitle: String,
    val expectedPriority: Priority = Priority.NONE,
    val hasDate: Boolean = false,
    val hasTime: Boolean = false,
    val isRepeating: Boolean = false
)

object QuickAddHelperData {
    const val PLACEHOLDER_TEXT = "Call mom tomorrow 5pm p1"

    val EXAMPLES: List<QuickAddExample> = listOf(
        QuickAddExample(
            exampleText = "Call mom tomorrow 5pm p1",
            expectedTitle = "Call mom",
            expectedPriority = Priority.HIGH,
            hasDate = true,
            hasTime = true
        ),
        QuickAddExample(
            exampleText = "Buy groceries today 6pm !!!",
            expectedTitle = "Buy groceries",
            expectedPriority = Priority.HIGH,
            hasDate = true,
            hasTime = true
        ),
        QuickAddExample(
            exampleText = "Dentist appointment Monday 10am urgent",
            expectedTitle = "Dentist appointment",
            expectedPriority = Priority.HIGH,
            hasDate = true,
            hasTime = true
        ),
        QuickAddExample(
            exampleText = "Review DSA notes Friday 8pm !!",
            expectedTitle = "Review DSA notes",
            expectedPriority = Priority.MEDIUM,
            hasDate = true,
            hasTime = true
        ),
        QuickAddExample(
            exampleText = "Team sync tomorrow 2pm p2",
            expectedTitle = "Team sync",
            expectedPriority = Priority.MEDIUM,
            hasDate = true,
            hasTime = true
        ),
        QuickAddExample(
            exampleText = "Water plants Saturday 9am repeat every week",
            expectedTitle = "Water plants",
            expectedPriority = Priority.NONE,
            hasDate = true,
            hasTime = true,
            isRepeating = true
        )
    )
}
