package com.lordyhas.sonrelab.data.repository

import com.lordyhas.sonrelab.data.local.dao.TreatmentDao
import com.lordyhas.sonrelab.data.local.entity.TreatmentEntity
import kotlinx.coroutines.flow.Flow

class TreatmentRepository(private val treatmentDao: TreatmentDao) {

    fun getAllTreatments(): Flow<List<TreatmentEntity>> = treatmentDao.getAllTreatments()

    fun getActiveTreatments(): Flow<List<TreatmentEntity>> = treatmentDao.getActiveTreatments()

    fun getTreatmentById(id: Int): Flow<TreatmentEntity?> = treatmentDao.getTreatmentById(id)

    suspend fun getTreatmentByIdDirect(id: Int): TreatmentEntity? = treatmentDao.getTreatmentByIdDirect(id)

    suspend fun insertTreatment(name: String, startDate: Long = System.currentTimeMillis(), isActive: Boolean = true): Long {
        return treatmentDao.insertTreatment(
            TreatmentEntity(
                name = name,
                startDate = startDate,
                isActive = isActive
            )
        )
    }

    suspend fun updateTreatment(treatment: TreatmentEntity) {
        treatmentDao.updateTreatment(treatment)
    }

    suspend fun deleteTreatment(treatment: TreatmentEntity) {
        treatmentDao.deleteTreatment(treatment)
    }

    suspend fun deleteTreatmentById(id: Int) {
        treatmentDao.deleteTreatmentById(id)
    }
}
