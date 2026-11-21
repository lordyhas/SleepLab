package com.lordyhas.sleeplab.service.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YamnetSnoreTrackerTest {

    private val hop = 500L

    /** Feeds one score per hop starting at 0 and collects every burst, including the open one. */
    private fun run(tracker: YamnetSnoreTracker, scores: List<Float>, db: Float = 20f): List<SnoreBurst> {
        val bursts = mutableListOf<SnoreBurst>()
        scores.forEachIndexed { i, s -> bursts += tracker.onWindow(i * hop, s, db + i) }
        tracker.flush()?.let { bursts += it }
        return bursts
    }

    @Test
    fun `quiet night gives no burst`() {
        assertTrue(run(YamnetSnoreTracker(hop), List(20) { 0.05f }).isEmpty())
    }

    @Test
    fun `a run of snoring windows is one burst covering one hop per window`() {
        val bursts = run(YamnetSnoreTracker(hop), listOf(0f, 0.6f, 0.7f, 0.5f, 0f, 0f))
        assertEquals(1, bursts.size)
        assertEquals(500L, bursts[0].startMs)
        assertEquals(1_500L, bursts[0].durationMs)
        assertEquals(23f, bursts[0].peakDb) // loudest window (index 3)
    }

    @Test
    fun `a weaker window inside a snore does not split it`() {
        val bursts = run(YamnetSnoreTracker(hop), listOf(0.5f, 0.2f, 0.5f, 0f))
        assertEquals(1, bursts.size)
        assertEquals(1_500L, bursts[0].durationMs)
    }

    @Test
    fun `a weak score alone does not start a snore`() {
        assertTrue(run(YamnetSnoreTracker(hop), listOf(0.2f, 0.25f, 0.2f, 0f)).isEmpty())
    }

    @Test
    fun `long runs are cut into bursts of at most the max length`() {
        val bursts = run(YamnetSnoreTracker(hop, maxBurstMs = 2_000L), List(9) { 0.8f })
        assertEquals(listOf(2_000L, 2_000L, 500L), bursts.map { it.durationMs })
        assertEquals(listOf(0L, 2_000L, 4_000L), bursts.map { it.startMs })
    }

    @Test
    fun `flush closes the open burst once`() {
        val tracker = YamnetSnoreTracker(hop)
        tracker.onWindow(0, 0.9f, 10f)
        assertEquals(500L, tracker.flush()?.durationMs)
        assertNull(tracker.flush())
    }
}
