package com.lordyhas.sonrelab.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lordyhas.sonrelab.data.local.dao.SleepSessionDao
import com.lordyhas.sonrelab.data.local.dao.SnoreEventDao
import com.lordyhas.sonrelab.data.local.dao.TreatmentDao
import com.lordyhas.sonrelab.data.local.entity.SleepSessionEntity
import com.lordyhas.sonrelab.data.local.entity.SnoreEventEntity
import com.lordyhas.sonrelab.data.local.entity.TreatmentEntity

@Database(
    entities = [
        TreatmentEntity::class,
        SleepSessionEntity::class,
        SnoreEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun treatmentDao(): TreatmentDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun snoreEventDao(): SnoreEventDao

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
