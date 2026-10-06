package com.lordyhas.sonrelab.data.repository

import com.lordyhas.sonrelab.data.local.dao.SleepSessionDao
import com.lordyhas.sonrelab.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

class SleepSessionRepository(private val sleepSessionDao: SleepSessionDao) {

    fun getAllSessions(): Flow<List<SleepSessionEntity>> = sleepSessionDao.getAllSessions()

    fun getSessionById(id: Int): Flow<SleepSessionEntity?> = sleepSessionDao.getSessionById(id)

    suspend fun getSessionByIdDirect(id: Int): SleepSessionEntity? = sleepSessionDao.getSessionByIdDirect(id)

    fun getLatestSession(): Flow<SleepSessionEntity?> = sleepSessionDao.getLatestSession()

    fun getSessionsForTreatment(treatmentId: Int): Flow<List<SleepSessionEntity>> =
        sleepSessionDao.getSessionsForTreatment(treatmentId)

    fun getSessionsBetween(fromTime: Long, toTime: Long): Flow<List<SleepSessionEntity>> =
        sleepSessionDao.getSessionsBetween(fromTime, toTime)

    suspend fun startNewSession(treatmentId: Int?): Long {
        val session = SleepSessionEntity(
            startTime = System.currentTimeMillis(),
            endTime = 0L,
            treatmentId = treatmentId,
            totalSnoreDurationSeconds = 0,
            snoreIntensityScore = 0f
        )
        return sleepSessionDao.insertSession(session)
    }

    suspend fun completeSession(
        sessionId: Int,
        endTime: Long = System.currentTimeMillis(),
        totalSnoreDurationSeconds: Int,
        snoreIntensityScore: Float
    ) {
        val existing = sleepSessionDao.getSessionByIdDirect(sessionId) ?: return
        val updated = existing.copy(
            endTime = endTime,
            totalSnoreDurationSeconds = totalSnoreDurationSeconds,
            snoreIntensityScore = snoreIntensityScore
        )
        sleepSessionDao.updateSession(updated)
    }

    suspend fun updateSession(session: SleepSessionEntity) {
        sleepSessionDao.updateSession(session)
    }

    suspend fun deleteSession(session: SleepSessionEntity) {
        sleepSessionDao.deleteSession(session)
    }

    suspend fun deleteSessionById(id: Int) {
        sleepSessionDao.deleteSessionById(id)
    }
}
