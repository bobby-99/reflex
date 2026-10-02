package com.reflex.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_app_events")
data class BlockedAppEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val packageName: String,
    val timestamp: Long
)
