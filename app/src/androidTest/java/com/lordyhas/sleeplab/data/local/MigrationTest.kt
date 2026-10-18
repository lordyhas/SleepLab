package com.lordyhas.sleeplab.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val dbName = "migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate2To3_keepsNightsAndDefaultsNewColumns() {
        helper.createDatabase(dbName, 2).apply {
            execSQL("INSERT INTO treatments (id, name, startDate, isActive) VALUES (1, 'Orthosis', 1000, 1)")
            execSQL(
                "INSERT INTO sleep_sessions (id, startTime, endTime, treatmentId, totalSnoreDurationSeconds, snoreIntensityScore) " +
                    "VALUES (7, 1000, 9000, 1, 120, 55.5)"
            )
            execSQL("INSERT INTO snore_events (id, sessionId, timestamp, amplitude, durationSeconds) VALUES (1, 7, 2000, 61.0, 3)")
            execSQL("INSERT INTO noise_samples (id, sessionId, timestamp, decibelLevel) VALUES (1, 7, 2000, 40.0)")
            close()
        }

        // Validates the migrated schema against 3.json.
        val db = helper.runMigrationsAndValidate(dbName, 3, true, *ALL_MIGRATIONS)

        db.query(
            "SELECT treatmentId, totalSnoreDurationSeconds, snoreIntensityScore, thresholdDb, noiseFloorDb, note, morningRating " +
                "FROM sleep_sessions WHERE id = 7"
        ).use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
            assertEquals(120, c.getInt(1))
            assertEquals(55.5f, c.getFloat(2))
            assertEquals(50f, c.getFloat(3))
            assertEquals(true, c.isNull(4))
            assertEquals(true, c.isNull(5))
            assertEquals(true, c.isNull(6))
        }
        db.query("SELECT COUNT(*) FROM snore_events WHERE sessionId = 7").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate3To4_marksExistingScoresAsLegacy() {
        helper.createDatabase(dbName, 3).apply {
            execSQL(
                "INSERT INTO sleep_sessions (id, startTime, endTime, totalSnoreDurationSeconds, snoreIntensityScore, thresholdDb, noiseFloorDb) " +
                    "VALUES (3, 1000, 9000, 60, 52.0, 47.0, 35.0)"
            )
            execSQL("INSERT INTO noise_samples (id, sessionId, timestamp, decibelLevel) VALUES (1, 3, 2000, 38.0)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, MIGRATION_3_4)

        db.query("SELECT snoreIntensityScore, scoreVersion, thresholdDb, noiseFloorDb FROM sleep_sessions WHERE id = 3").use { c ->
            c.moveToFirst()
            assertEquals(52f, c.getFloat(0))
            assertEquals(1, c.getInt(1))
            assertEquals(47f, c.getFloat(2))
            assertEquals(35f, c.getFloat(3))
        }
        db.query("SELECT decibelLevel, thresholdDb FROM noise_samples WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(38f, c.getFloat(0))
            assertEquals(true, c.isNull(1))
        }
        db.close()
    }

    @Test
    fun migrate4To5_addsEmptyClipPath() {
        helper.createDatabase(dbName, 4).apply {
            execSQL("INSERT INTO sleep_sessions (id, startTime, endTime, totalSnoreDurationSeconds, snoreIntensityScore) VALUES (1, 1000, 9000, 0, 0.0)")
            execSQL("INSERT INTO snore_events (id, sessionId, timestamp, amplitude, durationSeconds) VALUES (5, 1, 2000, 58.0, 2)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, MIGRATION_4_5)

        db.query("SELECT amplitude, durationSeconds, clipPath FROM snore_events WHERE id = 5").use { c ->
            c.moveToFirst()
            assertEquals(58f, c.getFloat(0))
            assertEquals(2, c.getInt(1))
            assertEquals(true, c.isNull(2))
        }
        db.close()
    }

    @Test
    fun migrate2To4_fullUpgradePathFromRelease() {
        helper.createDatabase(dbName, 2).apply {
            execSQL(
                "INSERT INTO sleep_sessions (id, startTime, endTime, totalSnoreDurationSeconds, snoreIntensityScore) " +
                    "VALUES (1, 1000, 9000, 30, 48.0)"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, *ALL_MIGRATIONS)

        db.query("SELECT thresholdDb, scoreVersion FROM sleep_sessions WHERE id = 1").use { c ->
            c.moveToFirst()
            assertEquals(50f, c.getFloat(0))
            assertEquals(1, c.getInt(1))
        }
        db.close()
    }

    @Test
    fun deleteNight_removesSessionWithItsEventsAndSamples() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val sessionDao = db.sleepSessionDao()
            val keep = sessionDao.insertSession(
                SleepSessionEntity(startTime = 1, endTime = 2)
            ).toInt()
            val remove = sessionDao.insertSession(
                SleepSessionEntity(startTime = 3, endTime = 4)
            ).toInt()
            listOf(keep, remove).forEach { id ->
                db.snoreEventDao().insertEvent(
                    SnoreEventEntity(sessionId = id, amplitude = 60f)
                )
                db.noiseSampleDao().insertSample(
                    NoiseSampleEntity(sessionId = id, decibelLevel = 40f)
                )
            }

            sessionDao.deleteNight(remove)

            assertNull(sessionDao.getSessionByIdDirect(remove))
            assertEquals(0, db.snoreEventDao().getEventCountForSession(remove))
            assertEquals(emptyList<Any>(), db.noiseSampleDao().getSamplesForSession(remove))
            assertEquals(1, db.snoreEventDao().getEventCountForSession(keep))
            assertEquals(1, db.noiseSampleDao().getSamplesForSession(keep).size)
        } finally {
            db.close()
        }
    }
}
