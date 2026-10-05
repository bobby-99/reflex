package com.reflex.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSettingsDao {
    @Query("SELECT * FROM focus_settings WHERE id = 1 LIMIT 1")
    fun getFocusSettings(): Flow<FocusSettings?>

    @Query("SELECT * FROM focus_settings WHERE id = 1 LIMIT 1")
    suspend fun getFocusSettingsSync(): FocusSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: FocusSettings)
}

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSession): Long

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE startTime >= :startDate AND startTime <= :endDate ORDER BY startTime DESC")
    fun getSessionsInDateRange(startDate: Long, endDate: Long): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE tagId = :tagId ORDER BY startTime DESC")
    fun getSessionsByTag(tagId: Long): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE startTime >= :startDate AND startTime <= :endDate AND (:tagId IS NULL OR tagId = :tagId) ORDER BY startTime DESC")
    fun getSessionsInDateRangeWithTag(startDate: Long, endDate: Long, tagId: Long?): Flow<List<FocusSession>>

    @Query("SELECT tagId, SUM(actualDurationSeconds) AS totalSeconds, COUNT(*) AS sessionCount FROM focus_sessions WHERE startTime >= :startDate AND startTime <= :endDate GROUP BY tagId")
    fun getFocusTimeByTagInRange(startDate: Long, endDate: Long): Flow<List<TagFocusStat>>

    @Query("SELECT tagId, SUM(actualDurationSeconds) AS totalSeconds, COUNT(*) AS sessionCount FROM focus_sessions GROUP BY tagId")
    fun getTotalFocusTimeByTag(): Flow<List<TagFocusStat>>

    @Query("UPDATE focus_sessions SET tagId = NULL WHERE tagId = :tagId")
    suspend fun clearTagFromSessions(tagId: Long): Int

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): FocusSession?

    @Query("DELETE FROM focus_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long): Int

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}

data class TagFocusStat(
    val tagId: Long?,
    val totalSeconds: Long,
    val sessionCount: Int
)

@Dao
interface FocusTagDao {
    @Query("SELECT * FROM focus_tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<FocusTag>>

    @Query("SELECT * FROM focus_tags ORDER BY name ASC")
    suspend fun getAllTagsSync(): List<FocusTag>

    @Query("SELECT * FROM focus_tags WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: Long): FocusTag?

    @Query("SELECT * FROM focus_tags WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTagByName(name: String): FocusTag?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: FocusTag): Long

    @androidx.room.Update
    suspend fun updateTag(tag: FocusTag)

    @Query("DELETE FROM focus_tags WHERE id = :id")
    suspend fun deleteTagById(id: Long)

    @Query("DELETE FROM focus_tags")
    suspend fun deleteAllTags()
}

