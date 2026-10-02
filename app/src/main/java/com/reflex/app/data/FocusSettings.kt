package com.reflex.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BlockingMode {
    OFF,
    BLOCK_LIST,
    ALLOW_LIST
}

@Entity(tableName = "focus_settings")
data class FocusSettings(
    @PrimaryKey
    val id: Int = 1,
    val workDurationMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val autoStartNextPhase: Boolean = false,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val blockingMode: BlockingMode = BlockingMode.OFF,
    val selectedPackages: String = ""
) {
    val selectedPackagesSet: Set<String>
        get() = if (selectedPackages.isBlank()) emptySet() else selectedPackages.split(",").map { it.trim() }.toSet()
}
