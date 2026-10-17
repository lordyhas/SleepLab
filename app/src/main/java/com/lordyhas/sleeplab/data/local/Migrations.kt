package com.lordyhas.sleeplab.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v2 → v3: per-night detection settings, notes and morning rating.
 * Existing nights were all recorded with the fixed 50 dB threshold, hence the default.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN thresholdDb REAL NOT NULL DEFAULT 50.0")
        db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN noiseFloorDb REAL")
        db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN note TEXT")
        db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN morningRating INTEGER")
    }
}

/** Every migration, in order. New migrations must be appended here and covered by MigrationTest. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3)
