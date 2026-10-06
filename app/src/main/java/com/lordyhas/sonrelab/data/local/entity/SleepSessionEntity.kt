package com.lordyhas.sonrelab.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sleep_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TreatmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["treatmentId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("treatmentId")]
)
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long = 0L,
    val treatmentId: Int? = null,
    val totalSnoreDurationSeconds: Int = 0,
    val snoreIntensityScore: Float = 0f
)
