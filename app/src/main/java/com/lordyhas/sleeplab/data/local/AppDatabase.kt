package com.lordyhas.sleeplab.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lordyhas.sleeplab.data.local.dao.SleepSessionDao
import com.lordyhas.sleeplab.data.local.dao.SnoreEventDao
import com.lordyhas.sleeplab.data.local.dao.NoiseSampleDao
import com.lordyhas.sleeplab.data.local.dao.TreatmentDao
import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity

@Database(
    entities = [
        TreatmentEntity::class,
        SleepSessionEntity::class,
        SnoreEventEntity::class,
        NoiseSampleEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun treatmentDao(): TreatmentDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun snoreEventDao(): SnoreEventDao
    abstract fun noiseSampleDao(): NoiseSampleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "snore_tracker_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
