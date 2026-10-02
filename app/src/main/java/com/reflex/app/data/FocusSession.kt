package com.reflex.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FocusMode {
    CLASSIC_POMODORO,
    FLOW_TIMED,
    FLOW_OPEN
}

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mode: FocusMode,
    val startTime: Long,
    val endTime: Long,
    val plannedDurationSeconds: Int?, // null for FLOW_OPEN
    val actualDurationSeconds: Int,
    val completedCycles: Int, // count of full work phases completed
    val completed: Boolean, // true only if not abandoned early
    val blockedAttemptCount: Int = 0,
    val sessionTitle: String? = null,
    val checklistJson: String? = null,
    val tagId: Long? = null
)
