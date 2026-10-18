package com.lordyhas.sleeplab.service.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoreDetectorTest {

    private val frameMs = 100L
    private val snore = FrameFeatures(db = 60f, lowBandRatio = 0.8f)
    private val speech = FrameFeatures(db = 62f, lowBandRatio = 0.25f)
    private val quiet = FrameFeatures(db = 30f, lowBandRatio = 0.5f)

    private class Run(val detector: SnoreDetector) {
        var now = 0L
        val reported = mutableListOf<SnoreBurst>()
        fun feed(features: FrameFeatures, durationMs: Long) {
            var t = 0L
            while (t < durationMs) {
                reported += detector.onFrame(now, 100L, features)
                now += 100L
                t += 100L
            }
        }
    }

    private fun run() = Run(SnoreDetector(thresholdDb = 45f))

    /** One breath: [burstMs] of sound, then quiet until [periodMs]. */
    private fun Run.breath(features: FrameFeatures = snore, burstMs: Long = 1_000, periodMs: Long = 4_000) {
        feed(features, burstMs)
        feed(quiet, periodMs - burstMs)
    }

    @Test
    fun `regular snoring is confirmed after three breaths`() {
        val r = run()
        r.breath(); r.breath()
        assertEquals(0, r.reported.size)
        r.breath()
        assertEquals(3, r.reported.size)
        assertTrue(r.detector.isRhythmLocked)
        assertEquals(listOf(0L, 4_000L, 8_000L), r.reported.map { it.startMs })
        assertEquals(1, r.reported.first().durationSeconds)
    }

    @Test
    fun `once locked each new breath is reported immediately`() {
        val r = run()
        repeat(3) { r.breath() }
        r.breath()
        assertEquals(4, r.reported.size)
        r.breath()
        assertEquals(5, r.reported.size)
    }

    @Test
    fun `a single loud low noise is never reported`() {
        val r = run()
        r.feed(snore, 1_000)
        r.feed(quiet, 30_000)
        assertEquals(0, r.reported.size)
    }

    @Test
    fun `speech at snoring cadence is ignored because it is not low-pitched`() {
        val r = run()
        repeat(6) { r.breath(features = speech) }
        assertEquals(0, r.reported.size)
    }

    @Test
    fun `continuous noise longer than a breath is ignored`() {
        val r = run()
        repeat(3) {
            r.feed(snore, 6_000)
            r.feed(quiet, 2_000)
        }
        assertEquals(0, r.reported.size)
    }

    @Test
    fun `irregular bursts are not taken as a rhythm`() {
        val r = run()
        r.breath(periodMs = 2_000)
        r.breath(periodMs = 8_500)
        r.breath()
        assertEquals(0, r.reported.size)
    }

    @Test
    fun `lock is dropped after a long silence and must be re-established`() {
        val r = run()
        repeat(3) { r.breath() }
        assertEquals(3, r.reported.size)
        r.feed(quiet, 30_000)
        assertFalse(r.detector.isRhythmLocked)
        r.breath(); r.breath()
        assertEquals(3, r.reported.size)
        r.breath()
        assertEquals(6, r.reported.size)
    }

    @Test
    fun `classification separates quiet, noise and snore-like frames`() {
        val d = SnoreDetector(thresholdDb = 45f)
        assertEquals(SoundClass.QUIET, d.classify(quiet))
        assertEquals(SoundClass.NOISE, d.classify(speech))
        assertEquals(SoundClass.SNORE_LIKE, d.classify(snore))
    }
}
