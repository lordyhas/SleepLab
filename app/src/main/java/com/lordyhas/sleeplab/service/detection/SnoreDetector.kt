package com.lordyhas.sleeplab.service.detection

import kotlin.math.max
import kotlin.math.roundToInt

/** A confirmed snore: when it started, how loud it peaked, how long it lasted. */
data class SnoreBurst(
    val startMs: Long,
    val durationMs: Long,
    val peakDb: Float
) {
    val durationSeconds: Int get() = max(1, (durationMs / 1000f).roundToInt())
}

/** What the current frame sounds like, for live feedback. */
enum class SoundClass { QUIET, NOISE, SNORE_LIKE }

/**
 * Pure snore detector fed one analysis frame at a time.
 *
 * 1. **Bursts** — consecutive frames that are both above [thresholdDb] and low-pitched
 *    (low-band ratio ≥ [minLowBandRatio]). Short dips up to [gapToleranceMs] don't end a burst.
 *    Bursts shorter than [minBurstMs] (clicks) or longer than [maxBurstMs] (talking, TV, a passing
 *    truck) are discarded.
 * 2. **Rhythm** — snoring repeats with breathing. A burst is only reported once [confirmBursts]
 *    bursts in a row start [minIntervalMs]–[maxIntervalMs] apart with roughly regular spacing.
 *    After that, each burst that keeps the cadence is reported immediately; a silence longer than
 *    [lockTimeoutMs] drops the lock.
 *
 * Time comes from the caller (frame timestamps), so tests are deterministic.
 */
class SnoreDetector(
    var thresholdDb: Float,
    private val minLowBandRatio: Float = DEFAULT_MIN_LOW_BAND_RATIO,
    private val minBurstMs: Long = 300,
    private val maxBurstMs: Long = 4_000,
    private val gapToleranceMs: Long = 200,
    private val minIntervalMs: Long = 2_000,
    private val maxIntervalMs: Long = 9_000,
    private val confirmBursts: Int = 3,
    private val maxIrregularity: Float = 1.8f,
    private val lockTimeoutMs: Long = 20_000
) {
    // Burst being built
    private var burstStart = -1L
    private var burstLastActive = -1L
    private var burstPeak = 0f

    // Recent valid bursts not yet reported (waiting for rhythm confirmation)
    private val pending = ArrayDeque<SnoreBurst>()
    private var lastReported: SnoreBurst? = null

    /** True while a breathing rhythm is established. */
    var isRhythmLocked: Boolean = false
        private set

    /**
     * Feeds one frame. Returns bursts confirmed as snoring by this frame (usually empty;
     * several when the rhythm locks and earlier bursts are confirmed together).
     */
    fun onFrame(timestampMs: Long, frameMs: Long, features: FrameFeatures): List<SnoreBurst> {
        val qualifies = classify(features) == SoundClass.SNORE_LIKE
        val frameEnd = timestampMs + frameMs
        var finished: SnoreBurst? = null

        if (qualifies) {
            if (burstStart < 0) {
                burstStart = timestampMs
                burstPeak = 0f
            }
            burstLastActive = frameEnd
            burstPeak = max(burstPeak, features.db)
        } else if (burstStart >= 0 && frameEnd - burstLastActive > gapToleranceMs) {
            val duration = burstLastActive - burstStart
            if (duration in minBurstMs..maxBurstMs) {
                finished = SnoreBurst(burstStart, duration, burstPeak)
            }
            burstStart = -1L
        }

        // Drop the lock after a long silence
        val lastActivity = lastReported?.startMs ?: pending.lastOrNull()?.startMs
        if (isRhythmLocked && lastActivity != null && timestampMs - lastActivity > lockTimeoutMs) {
            isRhythmLocked = false
            pending.clear()
        }

        return if (finished != null) onBurst(finished) else emptyList()
    }

    fun classify(features: FrameFeatures): SoundClass = when {
        features.db < thresholdDb -> SoundClass.QUIET
        features.lowBandRatio >= minLowBandRatio -> SoundClass.SNORE_LIKE
        else -> SoundClass.NOISE
    }

    private fun onBurst(burst: SnoreBurst): List<SnoreBurst> {
        val previous = lastReported
        if (isRhythmLocked && previous != null) {
            val interval = burst.startMs - previous.startMs
            if (interval in minIntervalMs..maxIntervalMs) {
                lastReported = burst
                return listOf(burst)
            }
            if (interval < minIntervalMs) return emptyList() // extra burst within one breath: ignore
            // Cadence broken: start looking for a rhythm again from this burst
            isRhythmLocked = false
            pending.clear()
        }

        pending.addLast(burst)
        // Bursts too far apart can't belong to the same rhythm
        while (pending.size >= 2 && pending[pending.size - 1].startMs - pending[pending.size - 2].startMs > maxIntervalMs) {
            repeat(pending.size - 1) { pending.removeFirst() }
        }
        // A burst too close to the previous one is part of the same breath: keep the louder one
        if (pending.size >= 2) {
            val last = pending[pending.size - 1]
            val before = pending[pending.size - 2]
            if (last.startMs - before.startMs < minIntervalMs) {
                pending.removeLast()
                if (last.peakDb > before.peakDb) {
                    pending.removeLast()
                    pending.addLast(last)
                }
            }
        }
        while (pending.size > confirmBursts) pending.removeFirst()

        if (pending.size == confirmBursts && isRegular(pending)) {
            isRhythmLocked = true
            val confirmed = pending.toList()
            lastReported = confirmed.last()
            pending.clear()
            return confirmed
        }
        return emptyList()
    }

    private fun isRegular(bursts: List<SnoreBurst>): Boolean {
        val intervals = bursts.zipWithNext { a, b -> b.startMs - a.startMs }
        if (intervals.any { it !in minIntervalMs..maxIntervalMs }) return false
        return intervals.max().toFloat() / intervals.min() <= maxIrregularity
    }

    companion object {
        /** Minimum share of energy in the 60–500 Hz band for a frame to sound like snoring. */
        const val DEFAULT_MIN_LOW_BAND_RATIO = 0.55f
    }
}
