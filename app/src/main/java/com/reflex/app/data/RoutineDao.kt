package com.reflex.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routines WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines ORDER BY createdAt DESC")
    fun getAllRoutines(): Flow<List<Routine>>

    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun getRoutineById(routineId: Long): Flow<Routine?>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :routineId")
    fun getRoutineWithSteps(routineId: Long): Flow<RoutineWithSteps?>

    @Transaction
    @Query("SELECT * FROM routines WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveRoutinesWithSteps(): Flow<List<RoutineWithSteps>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: Routine): Long

    @Update
    suspend fun updateRoutine(routine: Routine)

    @Delete
    suspend fun deleteRoutine(routine: Routine)

    @Query("UPDATE routines SET isArchived = :isArchived WHERE id = :routineId")
    suspend fun setArchived(routineId: Long, isArchived: Boolean)

    @Query("DELETE FROM routines")
    suspend fun deleteAllRoutines()
}
