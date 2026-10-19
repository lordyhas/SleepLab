package com.lordyhas.sleeplab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lordyhas.sleeplab.data.local.entity.EventClip
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnoreEventDao {
    @Query("SELECT * FROM snore_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEventsForSession(sessionId: Int): Flow<List<SnoreEventEntity>>

    @Query("SELECT * FROM snore_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSessionDirect(sessionId: Int): List<SnoreEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SnoreEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<SnoreEventEntity>)

    @Query("UPDATE snore_events SET clipPath = :path WHERE id = :id")
    suspend fun updateClipPath(id: Int, path: String?)

    @Query("SELECT id, clipPath FROM snore_events WHERE clipPath IS NOT NULL AND timestamp < :cutoff")
    suspend fun getClipsOlderThan(cutoff: Long): List<EventClip>

    @Query("UPDATE snore_events SET clipPath = NULL WHERE id IN (:ids)")
    suspend fun clearClipPaths(ids: List<Int>)

    @Query("UPDATE snore_events SET clipPath = NULL WHERE clipPath IS NOT NULL")
    suspend fun clearAllClipPaths()

    @Query("DELETE FROM snore_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: Int)

    @Query("SELECT COUNT(*) FROM snore_events WHERE sessionId = :sessionId")
    suspend fun getEventCountForSession(sessionId: Int): Int

    @Query("SELECT AVG(amplitude) FROM snore_events WHERE sessionId = :sessionId")
    suspend fun getAverageAmplitudeForSession(sessionId: Int): Float?

    @Query("SELECT MAX(amplitude) FROM snore_events WHERE sessionId = :sessionId")
    suspend fun getMaxAmplitudeForSession(sessionId: Int): Float?
}
