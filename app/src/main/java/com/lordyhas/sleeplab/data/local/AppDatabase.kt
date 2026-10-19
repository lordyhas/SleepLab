package com.lordyhas.sleeplab.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lordyhas.sleeplab.data.local.dao.SleepSessionDao
import com.lordyhas.sleeplab.data.local.dao.SnoreEventDao
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lordyhas.sleeplab.data.local.dao.FactorDao
import com.lordyhas.sleeplab.data.local.dao.NoiseSampleDao
import com.lordyhas.sleeplab.data.local.entity.FactorEntity
import com.lordyhas.sleeplab.data.local.entity.SessionFactorCrossRef
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
        NoiseSampleEntity::class,
        FactorEntity::class,
        SessionFactorCrossRef::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun treatmentDao(): TreatmentDao
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun snoreEventDao(): SnoreEventDao
    abstract fun noiseSampleDao(): NoiseSampleDao
    abstract fun factorDao(): FactorDao

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
                    .addMigrations(*ALL_MIGRATIONS)
                    // Only pre-release v1 databases (never shipped with real data) may be wiped.
                    // From v2 on, every schema change needs a Migration — see Migrations.kt.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1)
                    // Fresh installs get the built-in factors too (upgrades get them from MIGRATION_5_6).
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) = seedBuiltInFactors(db)
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
