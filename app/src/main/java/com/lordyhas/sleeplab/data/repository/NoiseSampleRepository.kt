package com.lordyhas.sleeplab.data.repository

import com.lordyhas.sleeplab.data.local.dao.NoiseSampleDao
import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity

class NoiseSampleRepository(private val noiseSampleDao: NoiseSampleDao) {

    suspend fun getSamplesForSession(sessionId: Int): List<NoiseSampleEntity> =
        noiseSampleDao.getSamplesForSession(sessionId)

    suspend fun recordSample(sessionId: Int, decibelLevel: Float): Long {
        val sample = NoiseSampleEntity(
            sessionId = sessionId,
            timestamp = System.currentTimeMillis(),
            decibelLevel = decibelLevel
        )
        return noiseSampleDao.insertSample(sample)
    }

    suspend fun recordSamples(samples: List<NoiseSampleEntity>) {
        noiseSampleDao.insertSamples(samples)
    }

    suspend fun getAverageDbForSession(sessionId: Int): Float? =
        noiseSampleDao.getAverageDbForSession(sessionId)

    suspend fun getMaxDbForSession(sessionId: Int): Float? =
        noiseSampleDao.getMaxDbForSession(sessionId)

    suspend fun getMinDbForSession(sessionId: Int): Float? =
        noiseSampleDao.getMinDbForSession(sessionId)
}
