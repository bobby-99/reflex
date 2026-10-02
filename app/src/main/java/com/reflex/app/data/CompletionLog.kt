package com.reflex.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completion_logs",
    foreignKeys = [
        ForeignKey(
            entity = Routine::class,
            parentColumns = ["id"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["routineId"])]
)
data class CompletionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val dateCompleted: Long,
    val totalTimeTakenSeconds: Int,
    val stepsCompletedCount: Int,
    val isCompleted: Boolean = true,
    val isSkipped: Boolean = false,
    val totalStepsCount: Int = 0,
    val stepLogsJson: String? = null
)
