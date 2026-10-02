package com.reflex.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val notes: String? = null,
    val dueDate: Long? = null,       // epoch millis, date only (midnight)
    val dueTime: Long? = null,       // epoch millis, specific time
    val priority: Priority = Priority.NONE,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null,   // epoch millis

    // Recurrence Fields
    val recurrenceFrequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val recurrenceInterval: Int = 1,
    val recurrenceUnit: RecurrenceUnit = RecurrenceUnit.DAY,
    val recurrenceDaysOfWeek: String? = null, // e.g. "MON,WED,FRI"
    val recurrenceMonthlyMode: MonthlyMode = MonthlyMode.SAME_DATE,
    val recurrenceEndType: RecurrenceEndType = RecurrenceEndType.NEVER,
    val recurrenceEndDate: Long? = null,
    val recurrenceEndOccurrences: Int = 0,
    val recurrenceBasis: RecurrenceBasis = RecurrenceBasis.FROM_DUE_DATE,
    val recurrenceOccurrenceCount: Int = 0
)
