package com.reflex.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(
    tableName = "steps",
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
data class Step(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val routineId: Long,
    val name: String,
    val orderIndex: Int,
    val stepType: StepType = StepType.TIMED,
    val durationSeconds: Int? = null,
    val targetCount: Int? = null,
    val notes: String? = null,
    val emoji: String? = null,
    val restDurationSeconds: Int? = null
) : Serializable
