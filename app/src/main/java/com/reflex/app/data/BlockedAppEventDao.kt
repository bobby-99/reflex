package com.reflex.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedAppEventDao {
    @Insert
    suspend fun insertEvent(event: BlockedAppEvent): Long

    @Query("SELECT * FROM blocked_app_events WHERE timestamp >= :sinceMs ORDER BY timestamp DESC")
    fun getEventsSince(sinceMs: Long): Flow<List<BlockedAppEvent>>

    @Query("SELECT * FROM blocked_app_events WHERE timestamp BETWEEN :startMs AND :endMs ORDER BY timestamp DESC")
    fun getEventsInRange(startMs: Long, endMs: Long): Flow<List<BlockedAppEvent>>

    @Query("SELECT COUNT(*) FROM blocked_app_events")
    suspend fun getEventCount(): Int

    @Query("SELECT * FROM blocked_app_events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<BlockedAppEvent>>

    @Query("DELETE FROM blocked_app_events")
    suspend fun deleteAllEvents()
}
