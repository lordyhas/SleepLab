package com.lordyhas.sleeplab.service

import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import com.lordyhas.sleeplab.service.detection.SnoreScore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NightRecoveryTest {

    private val start = 1_000_000_000_000L
    private val session = SleepSessionEntity(id = 7, startTime = start, endTime = 0L, thresholdDb = 50f)

    private fun sample(offsetSec: Int, thresholdDb: Float? = 50f) = NoiseSampleEntity(
        sessionId = 7, timestamp = start + offsetSec * 1000L, decibelLevel = 30f, thresholdDb = thresholdDb
    )

    private fun event(offsetSec: Int, durationSec: Int, amplitude: Float) = SnoreEventEntity(
        sessionId = 7, timestamp = start + offsetSec * 1000L, amplitude = amplitude, durationSeconds = durationSec
    )

    @Test
    fun `night with nothing recorded is dropped`() {
        assertNull(NightRecovery.recover(session, emptyList(), emptyList()))
    }

    @Test
    fun `night cut within the first minute is dropped`() {
        assertNull(NightRecovery.recover(session, emptyList(), listOf(sample(10), sample(20), sample(30))))
    }

    @Test
    fun `night ends at the last sample`() {
        val samples = (1..360).map { sample(it * 10) } // one hour
        val recovered = NightRecovery.recover(session, emptyList(), samples)
        assertNotNull(recovered)
        assertEquals(start + 3_600_000L, recovered!!.endTime)
        assertEquals(0, recovered.totalSnoreDurationSeconds)
        assertEquals(0f, recovered.snoreIntensityScore)
    }

    @Test
    fun `a snore after the last sample extends the night`() {
        val recovered = NightRecovery.recover(session, listOf(event(605, 3, 60f)), (1..60).map { sample(it * 10) })
        assertEquals(start + 608_000L, recovered!!.endTime)
    }

    @Test
    fun `snore time and score are rebuilt from the events`() {
        val samples = (1..360).map { sample(it * 10) }
        val events = listOf(event(600, 2, 60f), event(1200, 3, 70f))
        val recovered = NightRecovery.recover(session, events, samples)!!
        assertEquals(5, recovered.totalSnoreDurationSeconds)
        val expected = SnoreScore.compute(
            listOf(SnoreScore.Episode(2, 10f), SnoreScore.Episode(3, 20f)), 3600
        )
        assertEquals(expected, recovered.snoreIntensityScore, 0.001f)
    }

    @Test
    fun `each snore is scored against the threshold in force at that time`() {
        // Threshold rises from 50 to 58 half-way through (adaptive floor).
        val samples = (1..360).map { sample(it * 10, thresholdDb = if (it <= 180) 50f else 58f) }
        val events = listOf(event(905, 2, 60f), event(2705, 2, 60f))
        val recovered = NightRecovery.recover(session, events, samples)!!
        val expected = SnoreScore.compute(
            listOf(SnoreScore.Episode(2, 10f), SnoreScore.Episode(2, 2f)), 3600
        )
        assertEquals(expected, recovered.snoreIntensityScore, 0.001f)
    }

    @Test
    fun `samples without a threshold fall back to the session threshold`() {
        val samples = (1..60).map { sample(it * 10, thresholdDb = null) }
        val recovered = NightRecovery.recover(session, listOf(event(300, 2, 60f)), samples)!!
        val expected = SnoreScore.compute(listOf(SnoreScore.Episode(2, 10f)), 600)
        assertEquals(expected, recovered.snoreIntensityScore, 0.001f)
    }
}
