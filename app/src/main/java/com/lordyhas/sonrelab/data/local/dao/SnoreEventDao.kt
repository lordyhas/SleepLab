package com.lordyhas.sonrelab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lordyhas.sonrelab.data.local.entity.SnoreEventEntity
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

    @Query("DELETE FROM snore_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: Int)

    @Query("SELECT COUNT(*) FROM snore_events WHERE sessionId = :sessionId")
    suspend fun getEventCountForSession(sessionId: Int): Int

    @Query("SELECT AVG(amplitude) FROM snore_events WHERE sessionId = :sessionId")
    suspend fun getAverageAmplitudeForSession(sessionId: Int): Float?

    @Query("SELECT MAX(amplitude) FROM snore_events WHERE sessionId = :sessionId")
    suspend fun getMaxAmplitudeForSession(sessionId: Int): Float?
}
