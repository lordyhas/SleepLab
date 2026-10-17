package com.lordyhas.sleeplab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity

@Dao
interface NoiseSampleDao {
    @Query("SELECT * FROM noise_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getSamplesForSession(sessionId: Int): List<NoiseSampleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: NoiseSampleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSamples(samples: List<NoiseSampleEntity>)

    @Query("DELETE FROM noise_samples WHERE sessionId = :sessionId")
    suspend fun deleteSamplesForSession(sessionId: Int)

    @Query("SELECT AVG(decibelLevel) FROM noise_samples WHERE sessionId = :sessionId")
    suspend fun getAverageDbForSession(sessionId: Int): Float?

    @Query("SELECT MAX(decibelLevel) FROM noise_samples WHERE sessionId = :sessionId")
    suspend fun getMaxDbForSession(sessionId: Int): Float?

    @Query("SELECT MIN(decibelLevel) FROM noise_samples WHERE sessionId = :sessionId")
    suspend fun getMinDbForSession(sessionId: Int): Float?
}
