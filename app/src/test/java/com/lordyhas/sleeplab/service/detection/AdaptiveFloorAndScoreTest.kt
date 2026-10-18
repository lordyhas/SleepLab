package com.lordyhas.sleeplab.service.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveFloorTest {

    private fun AdaptiveFloor.feed(db: Float, seconds: Int): Boolean {
        var changed = false
        repeat(seconds * 10) { changed = onFrame(db, 100L) || changed }
        return changed
    }

    @Test
    fun `stable room keeps the threshold`() {
        val floor = AdaptiveFloor(initialFloorDb = 35f, marginDb = 12f)
        assertEquals(47f, floor.thresholdDb)
        assertFalse(floor.feed(35f, 300))
        assertEquals(47f, floor.thresholdDb)
    }

    @Test
    fun `louder room raises the threshold gradually`() {
        val floor = AdaptiveFloor(initialFloorDb = 35f, marginDb = 12f, maxStepDb = 2f)
        floor.feed(45f, 60) // fan turns on: room now 10 dB louder
        assertEquals(49f, floor.thresholdDb) // one capped step
        floor.feed(45f, 240)
        assertEquals(57f, floor.thresholdDb) // reached floor + margin
        assertEquals(45f, floor.floorDb)
    }

    @Test
    fun `snoring does not drag the floor up`() {
        val floor = AdaptiveFloor(initialFloorDb = 35f, marginDb = 12f)
        // 1 s of snoring every 4 s for 5 minutes: quiet seconds dominate the low percentile
        repeat(75) {
            floor.feed(62f, 1)
            floor.feed(35f, 3)
        }
        assertEquals(47f, floor.thresholdDb)
    }

    @Test
    fun `no update before a minute of data`() {
        val floor = AdaptiveFloor(initialFloorDb = 35f)
        assertFalse(floor.feed(60f, 59))
        assertTrue(floor.feed(60f, 2))
    }
}

class SnoreScoreTest {

    private val hour = 3_600L

    @Test
    fun `no snoring scores zero`() {
        assertEquals(0f, SnoreScore.compute(emptyList(), 8 * hour))
    }

    @Test
    fun `quarter of the night snoring loudly scores 100`() {
        val episodes = List(2 * 3_600) { SnoreScore.Episode(1, dbAboveThreshold = 25f) }
        assertEquals(100f, SnoreScore.compute(episodes, 8 * hour), 0.01f)
    }

    @Test
    fun `loudness weighting halves faint snoring`() {
        val loud = List(1_000) { SnoreScore.Episode(1, 20f) }
        val faint = List(1_000) { SnoreScore.Episode(1, 0f) }
        val loudScore = SnoreScore.compute(loud, 8 * hour)
        val faintScore = SnoreScore.compute(faint, 8 * hour)
        // fraction halves → score scales by √½
        assertEquals(loudScore * 0.7071f, faintScore, 0.1f)
    }

    @Test
    fun `curve keeps small amounts visible`() {
        // 1 % of an 8 h night at full loudness ≈ 20
        val episodes = List(288) { SnoreScore.Episode(1, 20f) }
        assertEquals(20f, SnoreScore.compute(episodes, 8 * hour), 0.5f)
    }
}
