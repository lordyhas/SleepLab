package com.lordyhas.sonrelab.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lordyhas.sonrelab.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {
    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    fun getSessionById(id: Int): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getSessionByIdDirect(id: Int): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions ORDER BY startTime DESC LIMIT 1")
    fun getLatestSession(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE treatmentId = :treatmentId ORDER BY startTime DESC")
    fun getSessionsForTreatment(treatmentId: Int): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE startTime >= :fromTime AND startTime <= :toTime ORDER BY startTime ASC")
    fun getSessionsBetween(fromTime: Long, toTime: Long): Flow<List<SleepSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SleepSessionEntity): Long

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Delete
    suspend fun deleteSession(session: SleepSessionEntity)

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)
}
