package com.lordyhas.sleeplab.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseFloorCalibratorTest {

    private fun calibrator() = NoiseFloorCalibrator(durationMs = 1_000L, marginDb = 12f)

    @Test
    fun `no measurement gives no result`() {
        val c = calibrator()
        assertNull(c.result())
        assertFalse(c.isComplete)
        assertEquals(0f, c.progress)
    }

    @Test
    fun `completes after the configured duration`() {
        val c = calibrator()
        repeat(9) { c.add(30f, 100) }
        assertFalse(c.isComplete)
        assertEquals(0.9f, c.progress, 0.001f)
        c.add(30f, 100)
        assertTrue(c.isComplete)
        assertEquals(1f, c.progress)
    }

    @Test
    fun `quiet room gives floor plus margin`() {
        val c = calibrator()
        repeat(10) { c.add(36f, 100) }
        val result = c.result()!!
        assertEquals(36f, result.noiseFloorDb)
        assertEquals(48f, result.thresholdDb)
    }

    @Test
    fun `short loud noises do not raise the floor`() {
        val c = calibrator()
        // 8 quiet windows, then a cough and a door slam
        repeat(8) { c.add(32f, 100) }
        c.add(70f, 100)
        c.add(80f, 100)
        assertEquals(32f, c.result()!!.noiseFloorDb)
    }

    @Test
    fun `threshold is clamped to the supported range`() {
        val silent = calibrator().apply { repeat(10) { add(0f, 100) } }
        assertEquals(NoiseFloorCalibrator.MIN_THRESHOLD_DB, silent.result()!!.thresholdDb)

        val noisy = calibrator().apply { repeat(10) { add(70f, 100) } }
        assertEquals(NoiseFloorCalibrator.MAX_THRESHOLD_DB, noisy.result()!!.thresholdDb)
    }

    @Test
    fun `samples after completion are ignored`() {
        val c = calibrator()
        repeat(10) { c.add(30f, 100) }
        repeat(10) { c.add(90f, 100) }
        assertEquals(30f, c.result()!!.noiseFloorDb)
    }
}
