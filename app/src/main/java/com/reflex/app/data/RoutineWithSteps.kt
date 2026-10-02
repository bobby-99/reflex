package com.reflex.app.data

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineWithSteps(
    @Embedded val routine: Routine,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineId"
    )
    val steps: List<Step>
)
