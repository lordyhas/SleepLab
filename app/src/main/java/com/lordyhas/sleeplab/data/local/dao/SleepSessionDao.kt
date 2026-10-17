package com.lordyhas.sleeplab.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
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

    @Query("UPDATE sleep_sessions SET noiseFloorDb = :noiseFloorDb, thresholdDb = :thresholdDb WHERE id = :id")
    suspend fun updateCalibration(id: Int, noiseFloorDb: Float, thresholdDb: Float)

    @Query("UPDATE sleep_sessions SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Int, note: String?)

    @Query("UPDATE sleep_sessions SET morningRating = :rating WHERE id = :id")
    suspend fun updateMorningRating(id: Int, rating: Int?)

    @Query("DELETE FROM snore_events WHERE sessionId = :sessionId")
    suspend fun deleteEventsForSession(sessionId: Int)

    @Query("DELETE FROM noise_samples WHERE sessionId = :sessionId")
    suspend fun deleteSamplesForSession(sessionId: Int)

    /** Deletes a night and everything recorded during it, atomically. */
    @Transaction
    suspend fun deleteNight(id: Int) {
        deleteEventsForSession(id)
        deleteSamplesForSession(id)
        deleteSessionById(id)
    }
}
