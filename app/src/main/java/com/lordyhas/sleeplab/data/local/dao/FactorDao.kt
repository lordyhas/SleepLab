package com.lordyhas.sleeplab.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.lordyhas.sleeplab.data.local.entity.FactorEntity
import com.lordyhas.sleeplab.data.local.entity.SessionFactorCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface FactorDao {
    @Query("SELECT * FROM factors ORDER BY id")
    fun getAllFactors(): Flow<List<FactorEntity>>

    @Insert
    suspend fun insertFactor(factor: FactorEntity): Long

    @Query("UPDATE factors SET isArchived = :archived WHERE id = :id")
    suspend fun setArchived(id: Int, archived: Boolean)

    @Query("SELECT factorId FROM session_factors WHERE sessionId = :sessionId")
    fun getFactorIdsForSession(sessionId: Int): Flow<List<Int>>

    @Query("SELECT * FROM session_factors")
    fun getAllSessionFactors(): Flow<List<SessionFactorCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSessionFactors(refs: List<SessionFactorCrossRef>)

    @Query("DELETE FROM session_factors WHERE sessionId = :sessionId")
    suspend fun clearSessionFactors(sessionId: Int)

    /** Replaces the factors of a night. */
    @Transaction
    suspend fun setSessionFactors(sessionId: Int, factorIds: Collection<Int>) {
        clearSessionFactors(sessionId)
        insertSessionFactors(factorIds.map { SessionFactorCrossRef(sessionId, it) })
    }
}
