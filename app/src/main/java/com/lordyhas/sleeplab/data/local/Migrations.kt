package com.lordyhas.sleeplab.data.local

import androidx.room.migration.Migration
import com.lordyhas.sleeplab.data.local.entity.BuiltInFactors
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

/**
 * v3 → v4: versioned snore score (existing nights keep the legacy formula) and the threshold
 * in force for each noise sample, so the night chart can show it moving with the adaptive floor.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN scoreVersion INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE noise_samples ADD COLUMN thresholdDb REAL")
    }
}

/** v4 → v5: optional audio clip per snore event. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE snore_events ADD COLUMN clipPath TEXT")
    }
}

/** v5 → v6: per-night factors (alcohol, congestion…), with the built-in ones seeded. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `factors` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`key` TEXT, `name` TEXT, `isArchived` INTEGER NOT NULL DEFAULT 0)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_factors_key` ON `factors` (`key`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_factors` (`sessionId` INTEGER NOT NULL, `factorId` INTEGER NOT NULL, " +
                "PRIMARY KEY(`sessionId`, `factorId`), " +
                "FOREIGN KEY(`sessionId`) REFERENCES `sleep_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`factorId`) REFERENCES `factors`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_factors_factorId` ON `session_factors` (`factorId`)")
        seedBuiltInFactors(db)
    }
}

/** Inserts the built-in factors (idempotent thanks to the unique key). */
fun seedBuiltInFactors(db: SupportSQLiteDatabase) {
    BuiltInFactors.ALL.forEach { key ->
        db.execSQL("INSERT OR IGNORE INTO factors (`key`, `name`, `isArchived`) VALUES (?, NULL, 0)", arrayOf(key))
    }
}

/** Every migration, in order. New migrations must be appended here and covered by MigrationTest. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
