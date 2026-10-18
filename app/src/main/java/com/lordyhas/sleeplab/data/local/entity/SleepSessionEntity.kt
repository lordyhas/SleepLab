package com.lordyhas.sleeplab.data.local.entity

import androidx.room.ColumnInfo
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
    val snoreIntensityScore: Float = 0f,
    /** Snore detection threshold (dB) actually used for this night. */
    @ColumnInfo(defaultValue = "50.0")
    val thresholdDb: Float = 50f,
    /** Room background noise measured during calibration; null if calibration was off or didn't finish. */
    val noiseFloorDb: Float? = null,
    val note: String? = null,
    /** How the user felt in the morning, 1 (bad) to 5 (great); null if not rated. */
    val morningRating: Int? = null,
    /**
     * Formula behind [snoreIntensityScore]: 1 = legacy `(avgDb + maxDb) / 2`,
     * 2 = time-weighted score from `SnoreScore`. Nights before v4 of the database are 1.
     */
    @ColumnInfo(defaultValue = "1")
    val scoreVersion: Int = 1
)
