package com.lordyhas.sleeplab.service.detection

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/** Per-frame measurements the detector works from. */
data class FrameFeatures(
    /** Broadband level, relative dB (`20·log10(rms)`), same scale as the rest of the app. */
    val db: Float,
    /** Share of the frame's energy in the snoring band (0..1). */
    val lowBandRatio: Float
)

/**
 * Second-order IIR section (RBJ "Audio EQ Cookbook"), transposed direct form II.
 * Keeps state between calls so consecutive frames filter seamlessly.
 */
internal class Biquad private constructor(
    private val b0: Double, private val b1: Double, private val b2: Double,
    private val a1: Double, private val a2: Double
) {
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    companion object {
        private const val Q = 0.7071 // Butterworth

        fun highPass(sampleRate: Int, cutoffHz: Double): Biquad {
            val w0 = 2 * PI * cutoffHz / sampleRate
            val alpha = sin(w0) / (2 * Q)
            val c = cos(w0)
            val a0 = 1 + alpha
            return Biquad(
                b0 = (1 + c) / 2 / a0, b1 = -(1 + c) / a0, b2 = (1 + c) / 2 / a0,
                a1 = -2 * c / a0, a2 = (1 - alpha) / a0
            )
        }

        fun lowPass(sampleRate: Int, cutoffHz: Double): Biquad {
            val w0 = 2 * PI * cutoffHz / sampleRate
            val alpha = sin(w0) / (2 * Q)
            val c = cos(w0)
            val a0 = 1 + alpha
            return Biquad(
                b0 = (1 - c) / 2 / a0, b1 = (1 - c) / a0, b2 = (1 - c) / 2 / a0,
                a1 = -2 * c / a0, a2 = (1 - alpha) / a0
            )
        }
    }
}

/**
 * Turns raw PCM-16 frames into [FrameFeatures].
 *
 * Snoring concentrates its energy at low frequencies (the vibrating soft palate, roughly
 * 60–500 Hz), while speech, TV and traffic spread theirs much higher. The ratio of band energy to
 * total energy separates the two far better than loudness alone. Stateful: use one instance per
 * recording so filter state carries across frames.
 */
class AudioFeatureExtractor(
    sampleRate: Int,
    lowCutHz: Double = SNORE_BAND_LOW_HZ,
    highCutHz: Double = SNORE_BAND_HIGH_HZ
) {
    private val highPass = Biquad.highPass(sampleRate, lowCutHz)
    private val lowPass = Biquad.lowPass(sampleRate, highCutHz)

    fun extract(samples: ShortArray, offset: Int, count: Int): FrameFeatures {
        if (count <= 0) return FrameFeatures(0f, 0f)

        var mean = 0.0
        for (i in offset until offset + count) mean += samples[i]
        mean /= count

        var totalEnergy = 0.0
        var bandEnergy = 0.0
        var rawSquares = 0.0
        for (i in offset until offset + count) {
            val raw = samples[i].toDouble()
            rawSquares += raw * raw
            val centered = raw - mean
            totalEnergy += centered * centered
            val band = lowPass.process(highPass.process(raw))
            bandEnergy += band * band
        }

        val rms = sqrt(rawSquares / count)
        val db = if (rms > 1.0) (20.0 * log10(rms)).toFloat().coerceIn(0f, 100f) else 0f
        val ratio = if (totalEnergy > 0.0) (bandEnergy / totalEnergy).toFloat().coerceIn(0f, 1f) else 0f
        return FrameFeatures(db, ratio)
    }

    companion object {
        const val SNORE_BAND_LOW_HZ = 60.0
        const val SNORE_BAND_HIGH_HZ = 500.0
    }
}
