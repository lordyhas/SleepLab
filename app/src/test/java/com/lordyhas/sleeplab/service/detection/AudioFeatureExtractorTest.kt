package com.lordyhas.sleeplab.service.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class AudioFeatureExtractorTest {

    private val sampleRate = 16_000
    private val frame = 1_600 // 100 ms

    private fun tone(hz: Double, amplitude: Double, frames: Int = 5): ShortArray =
        ShortArray(frame * frames) { i -> (amplitude * sin(2 * PI * hz * i / sampleRate)).toInt().toShort() }

    /** Runs all frames through one extractor and returns the last (filters settled). */
    private fun lastFrame(signal: ShortArray): FrameFeatures {
        val extractor = AudioFeatureExtractor(sampleRate)
        var result = FrameFeatures(0f, 0f)
        var offset = 0
        while (offset + frame <= signal.size) {
            result = extractor.extract(signal, offset, frame)
            offset += frame
        }
        return result
    }

    @Test
    fun `low tone in the snore band has a high low-band ratio`() {
        val f = lastFrame(tone(150.0, 3_000.0))
        assertTrue("ratio was ${f.lowBandRatio}", f.lowBandRatio > 0.8f)
    }

    @Test
    fun `speech-range tone has a low low-band ratio`() {
        val f = lastFrame(tone(1_500.0, 3_000.0))
        assertTrue("ratio was ${f.lowBandRatio}", f.lowBandRatio < 0.2f)
    }

    @Test
    fun `white noise is mostly outside the snore band`() {
        val random = Random(1)
        val noise = ShortArray(frame * 5) { (random.nextGaussian() * 2_000).toInt().toShort() }
        val f = lastFrame(noise)
        assertTrue("ratio was ${f.lowBandRatio}", f.lowBandRatio < 0.2f)
    }

    @Test
    fun `level matches the app's relative dB scale`() {
        // A full-scale-ish sine of amplitude A has rms A/√2 → 20·log10(3000/√2) ≈ 66.5 dB
        val f = lastFrame(tone(150.0, 3_000.0))
        assertEquals(66.5f, f.db, 0.5f)
    }

    @Test
    fun `silence is 0 dB with no ratio`() {
        val f = lastFrame(ShortArray(frame * 3))
        assertEquals(0f, f.db)
        assertEquals(0f, f.lowBandRatio)
    }

    private fun Random.nextGaussian(): Double {
        // Box-Muller
        val u1 = nextDouble().coerceAtLeast(1e-12)
        val u2 = nextDouble()
        return kotlin.math.sqrt(-2 * kotlin.math.ln(u1)) * kotlin.math.cos(2 * PI * u2)
    }
}
