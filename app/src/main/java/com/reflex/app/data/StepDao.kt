package com.reflex.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StepDao {

    @Query("SELECT * FROM steps WHERE routineId = :routineId ORDER BY orderIndex ASC")
    fun getStepsForRoutine(routineId: Long): Flow<List<Step>>

    @Query("SELECT * FROM steps WHERE id = :stepId")
    suspend fun getStepById(stepId: Long): Step?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: Step): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<Step>)

    @Update
    suspend fun updateStep(step: Step)

    @Delete
    suspend fun deleteStep(step: Step)

    @Query("DELETE FROM steps WHERE routineId = :routineId")
    suspend fun deleteStepsForRoutine(routineId: Long)

    @Query("DELETE FROM steps")
    suspend fun deleteAllSteps()
}
