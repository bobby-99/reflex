package com.reflex.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

enum class HabitKind {
    CHECK_OFF,
    MEASURABLE,
    LIMIT
}

sealed class HabitDefinition {
    abstract val target: Double
    abstract val unit: String
    abstract val step: Double

    data object CheckOff : HabitDefinition() {
        override val target: Double = 1.0
        override val unit: String = ""
        override val step: Double = 1.0
    }

    data class Measurable(
        override val target: Double,
        override val unit: String,
        override val step: Double
    ) : HabitDefinition()

    data class Limit(
        override val target: Double,
        override val unit: String,
        override val step: Double
    ) : HabitDefinition()
}

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // "CHECK_OFF", "MEASURABLE", "LIMIT"
    val target: Double,
    val unit: String = "",
    val step: Double = 1.0,
    val startEpochDay: Long,
    val sortOrder: Int = 0
) {
    val habitKind: HabitKind
        get() = try {
            HabitKind.valueOf(type)
        } catch (_: Exception) {
            HabitKind.CHECK_OFF
        }

    val definition: HabitDefinition
        get() = when (habitKind) {
            HabitKind.CHECK_OFF -> HabitDefinition.CheckOff
            HabitKind.MEASURABLE -> HabitDefinition.Measurable(target, unit, step)
            HabitKind.LIMIT -> HabitDefinition.Limit(target, unit, step)
        }

    fun isCompleted(value: Double?): Boolean {
        if (value == null) return false
        return when (habitKind) {
            HabitKind.CHECK_OFF -> value >= 1.0
            HabitKind.MEASURABLE -> value >= target
            HabitKind.LIMIT -> value <= target
        }
    }

    fun progressFraction(value: Double?): Float {
        if (value == null) return 0f
        return when (habitKind) {
            HabitKind.CHECK_OFF -> if (value >= 1.0) 1f else 0f
            HabitKind.MEASURABLE -> if (target <= 0.0) 0f else min(1f, (value / target).toFloat())
            HabitKind.LIMIT -> if (value <= target) 1f else 0f
        }
    }

    fun nextStepValue(currentValue: Double?, direction: Int): Double {
        val curr = currentValue ?: 0.0
        val s = if (step <= 0.0) 1.0 else step
        val raw = max(0.0, round((curr + direction * s) * 10.0) / 10.0)
        val maxCap = when (habitKind) {
            HabitKind.CHECK_OFF -> 1.0
            HabitKind.MEASURABLE -> target * 3.0
            HabitKind.LIMIT -> target * 4.0
        }
        return min(raw, maxCap)
    }
}

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["habitId", "epochDay"], unique = true)
    ]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val habitId: Long,
    val epochDay: Long,
    val value: Double
)
