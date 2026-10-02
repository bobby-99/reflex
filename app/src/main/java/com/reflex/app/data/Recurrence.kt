package com.reflex.app.data

enum class RecurrenceFrequency {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

enum class RecurrenceUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

enum class RecurrenceEndType {
    NEVER,
    ON_DATE,
    AFTER_OCCURRENCES
}

enum class RecurrenceBasis {
    FROM_DUE_DATE,
    FROM_COMPLETION_DATE
}

enum class MonthlyMode {
    SAME_DATE,
    SAME_WEEKDAY_POS
}
