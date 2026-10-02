package com.reflex.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionLogDao {

    @Query("SELECT * FROM completion_logs WHERE routineId = :routineId ORDER BY dateCompleted DESC")
    fun getLogsForRoutine(routineId: Long): Flow<List<CompletionLog>>

    @Query("SELECT completion_logs.* FROM completion_logs INNER JOIN routines ON completion_logs.routineId = routines.id WHERE dateCompleted BETWEEN :startDate AND :endDate ORDER BY dateCompleted DESC")
    fun getLogsInDateRange(startDate: Long, endDate: Long): Flow<List<CompletionLog>>

    @Query("SELECT * FROM completion_logs WHERE routineId = :routineId AND dateCompleted BETWEEN :startDate AND :endDate ORDER BY dateCompleted DESC")
    fun getLogsForRoutineInDateRange(routineId: Long, startDate: Long, endDate: Long): Flow<List<CompletionLog>>

    @Query("SELECT completion_logs.* FROM completion_logs INNER JOIN routines ON completion_logs.routineId = routines.id ORDER BY dateCompleted DESC")
    fun getAllLogs(): Flow<List<CompletionLog>>

    @Query("SELECT COUNT(*) FROM completion_logs WHERE routineId = :routineId AND isCompleted = 1")
    fun getCompletionCountForRoutine(routineId: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CompletionLog): Long

    @Delete
    suspend fun deleteLog(log: CompletionLog)

    @Query("DELETE FROM completion_logs WHERE routineId = :routineId")
    suspend fun deleteLogsForRoutine(routineId: Long)

    @Query("DELETE FROM completion_logs")
    suspend fun deleteAllLogs()
}
