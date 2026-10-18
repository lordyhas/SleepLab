package com.lordyhas.sleeplab.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Something that may influence snoring on a given night (alcohol, a cold, sleeping on the back…).
 * Built-in factors have a [key] and are named by the UI in the user's language; custom ones
 * have a [name] and no key.
 */
@Entity(tableName = "factors", indices = [Index(value = ["key"], unique = true)])
data class FactorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val key: String? = null,
    val name: String? = null,
    /** Hidden from pickers but kept so past nights still show it. */
    @ColumnInfo(defaultValue = "0")
    val isArchived: Boolean = false
)

/** Which factors applied to which night. */
@Entity(
    tableName = "session_factors",
    primaryKeys = ["sessionId", "factorId"],
    foreignKeys = [
        ForeignKey(
            entity = SleepSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FactorEntity::class,
            parentColumns = ["id"],
            childColumns = ["factorId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("factorId")]
)
data class SessionFactorCrossRef(
    val sessionId: Int,
    val factorId: Int
)

/** Keys of the factors every install starts with (order = display order). */
object BuiltInFactors {
    const val ALCOHOL = "alcohol"
    const val LATE_MEAL = "late_meal"
    const val CONGESTION = "congestion"
    const val BACK_SLEEPING = "back_sleeping"
    const val EXHAUSTED = "exhausted"
    const val SEDATIVES = "sedatives"

    val ALL = listOf(ALCOHOL, LATE_MEAL, CONGESTION, BACK_SLEEPING, EXHAUSTED, SEDATIVES)
}
