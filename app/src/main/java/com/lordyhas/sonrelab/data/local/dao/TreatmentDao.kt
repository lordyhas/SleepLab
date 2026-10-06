package com.lordyhas.sonrelab.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lordyhas.sonrelab.data.local.entity.TreatmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TreatmentDao {
    @Query("SELECT * FROM treatments ORDER BY startDate DESC")
    fun getAllTreatments(): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatments WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveTreatments(): Flow<List<TreatmentEntity>>

    @Query("SELECT * FROM treatments WHERE id = :id")
    fun getTreatmentById(id: Int): Flow<TreatmentEntity?>

    @Query("SELECT * FROM treatments WHERE id = :id")
    suspend fun getTreatmentByIdDirect(id: Int): TreatmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatment(treatment: TreatmentEntity): Long

    @Update
    suspend fun updateTreatment(treatment: TreatmentEntity)

    @Delete
    suspend fun deleteTreatment(treatment: TreatmentEntity)

    @Query("DELETE FROM treatments WHERE id = :id")
    suspend fun deleteTreatmentById(id: Int)
}
