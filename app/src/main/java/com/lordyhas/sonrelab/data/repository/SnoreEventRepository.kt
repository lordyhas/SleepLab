package com.lordyhas.sonrelab.data.repository

import com.lordyhas.sonrelab.data.local.dao.SnoreEventDao
import com.lordyhas.sonrelab.data.local.entity.SnoreEventEntity
import kotlinx.coroutines.flow.Flow

class SnoreEventRepository(private val snoreEventDao: SnoreEventDao) {

    fun getEventsForSession(sessionId: Int): Flow<List<SnoreEventEntity>> =
        snoreEventDao.getEventsForSession(sessionId)

    suspend fun getEventsForSessionDirect(sessionId: Int): List<SnoreEventEntity> =
        snoreEventDao.getEventsForSessionDirect(sessionId)

    suspend fun recordSnoreEvent(sessionId: Int, amplitude: Float, durationSeconds: Int = 1): Long {
        val event = SnoreEventEntity(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            amplitude = amplitude,
            durationSeconds = durationSeconds
        )
        return snoreEventDao.insertEvent(event)
    }

    suspend fun recordSnoreEvents(events: List<SnoreEventEntity>) {
        snoreEventDao.insertEvents(events)
    }

    suspend fun getEventCountForSession(sessionId: Int): Int =
        snoreEventDao.getEventCountForSession(sessionId)

    suspend fun getAverageAmplitudeForSession(sessionId: Int): Float? =
        snoreEventDao.getAverageAmplitudeForSession(sessionId)

    suspend fun getMaxAmplitudeForSession(sessionId: Int): Float? =
        snoreEventDao.getMaxAmplitudeForSession(sessionId)
}
