package com.lordyhas.sleeplab.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "snore_events",
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class SnoreEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val sessionId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val amplitude: Float, // Sound level in dB
    val durationSeconds: Int = 1,
    /** Absolute path of the saved audio clip, if clips were enabled and this snore was kept. */
    val clipPath: String? = null
)

/** Event id + clip path, for clip maintenance queries. */
data class EventClip(val id: Int, val clipPath: String)
