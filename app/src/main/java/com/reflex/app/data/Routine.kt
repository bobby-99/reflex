package com.reflex.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek

@Entity(tableName = "routines")
data class Routine(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String = "BOLT",
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val restBetweenStepsEnabled: Boolean = false,
    val restDurationSeconds: Int = 15,
    val scheduledDays: String = "",       // Comma-separated DayOfWeek names e.g. "MONDAY,WEDNESDAY,FRIDAY"
    val reminderTime: String? = null,     // Formatted time string e.g. "08:30"
    val reminderEnabled: Boolean = false
) {
    val scheduledDaysSet: Set<DayOfWeek>
        get() = if (scheduledDays.isBlank()) emptySet() else {
            scheduledDays.split(",")
                .mapNotNull { name ->
                    try { DayOfWeek.valueOf(name.trim()) } catch (e: Exception) { null }
                }.toSet()
        }
}
