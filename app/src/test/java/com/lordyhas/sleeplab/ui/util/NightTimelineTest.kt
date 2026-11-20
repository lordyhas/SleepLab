package com.lordyhas.sleeplab.ui.util

import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NightTimelineTest {
    private val minute = 60_000L

    private fun sample(atMs: Long, db: Float, threshold: Float? = null) =
        NoiseSampleEntity(sessionId = 1, timestamp = atMs, decibelLevel = db, thresholdDb = threshold)

    private fun snore(atMs: Long, db: Float) =
        SnoreEventEntity(sessionId = 1, timestamp = atMs, amplitude = db, durationSeconds = 1)

    @Test
    fun `a night without snoring is all quiet`() {
        val samples = (0 until 6).map { sample(it * 10_000L, 8f) }
        val t = NightTimelines.build(0, minute, samples, emptyList(), thresholdDb = 35f)
        assertEquals(6, t.slots.size)
        assertEquals(60L, t.bandSeconds[SnoreBand.QUIET])
        assertEquals(0L, t.snoringSeconds)
        assertTrue(t.slots.all { it.level == 8f })
    }

    @Test
    fun `snore bands depend on dB above the threshold`() {
        assertEquals(SnoreBand.LIGHT, NightTimelines.bandFor(38f, 35f))
        assertEquals(SnoreBand.LOUD, NightTimelines.bandFor(41f, 35f))
        assertEquals(SnoreBand.VERY_LOUD, NightTimelines.bandFor(45f, 35f))
    }

    @Test
    fun `slots inside an episode take the band of their loudest snore`() {
        // Episode from 60 s to ~2 min: snores every 6 s at 38 dB, one at 46 dB.
        val events = (0 until 10).map { snore(60_000L + it * 6_000L, if (it == 4) 46f else 38f) }
        val t = NightTimelines.build(0, 3 * minute, emptyList(), events, thresholdDb = 35f)
        val bands = t.slots.map { it.band }
        assertEquals(SnoreBand.QUIET, bands[0])
        assertEquals(SnoreBand.LIGHT, bands[6])
        assertEquals(SnoreBand.VERY_LOUD, bands[8]) // 84 s falls in the 80–90 s slot
        assertEquals(SnoreBand.QUIET, bands.last())
        assertEquals(46f, t.slots[8].level)
        assertEquals(60L, t.snoringSeconds)
    }

    @Test
    fun `threshold follows the samples when it moved during the night`() {
        val samples = listOf(sample(0, 10f, threshold = 35f), sample(30_000, 10f, threshold = 40f))
        val events = listOf(snore(32_000, 44f), snore(38_000, 44f), snore(44_000, 44f))
        val t = NightTimelines.build(0, minute, samples, events, thresholdDb = 35f)
        // 44 dB is +4 over the 40 dB threshold in force, so LIGHT, not LOUD.
        assertEquals(SnoreBand.LIGHT, t.slots[3].band)
    }

    @Test
    fun `buckets keep the loudest level and band`() {
        val events = listOf(snore(15_000, 46f), snore(21_000, 38f))
        val t = NightTimelines.build(0, 2 * minute, emptyList(), events, thresholdDb = 35f)
        val buckets = t.buckets(3)
        assertEquals(3, buckets.size)
        assertEquals(SnoreBand.VERY_LOUD, buckets[0].band)
        assertEquals(46f, buckets[0].level)
        assertEquals(SnoreBand.QUIET, buckets[2].band)
        assertEquals(0L, buckets[0].startTime)
        assertEquals(2 * minute, buckets.last().endTime)
    }

    @Test
    fun `an empty or inverted night has no slots`() {
        assertTrue(NightTimelines.build(10, 10, emptyList(), emptyList(), 35f).slots.isEmpty())
    }
}
