package com.reflex.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_tags")
data class FocusTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
