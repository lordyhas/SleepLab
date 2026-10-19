package com.lordyhas.sleeplab.data.repository

import com.lordyhas.sleeplab.data.local.dao.FactorDao
import com.lordyhas.sleeplab.data.local.entity.FactorEntity
import com.lordyhas.sleeplab.data.local.entity.SessionFactorCrossRef
import kotlinx.coroutines.flow.Flow

class FactorRepository(private val factorDao: FactorDao) {

    fun getAllFactors(): Flow<List<FactorEntity>> = factorDao.getAllFactors()

    fun getFactorIdsForSession(sessionId: Int): Flow<List<Int>> = factorDao.getFactorIdsForSession(sessionId)

    fun getAllSessionFactors(): Flow<List<SessionFactorCrossRef>> = factorDao.getAllSessionFactors()

    suspend fun addCustomFactor(name: String): Int =
        factorDao.insertFactor(FactorEntity(name = name.trim())).toInt()

    suspend fun setArchived(factorId: Int, archived: Boolean) = factorDao.setArchived(factorId, archived)

    suspend fun setSessionFactors(sessionId: Int, factorIds: Collection<Int>) =
        factorDao.setSessionFactors(sessionId, factorIds)
}
