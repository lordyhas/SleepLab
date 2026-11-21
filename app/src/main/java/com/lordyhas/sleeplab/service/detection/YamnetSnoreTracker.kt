package com.lordyhas.sleeplab.service.detection

import kotlin.math.max

/**
 * Turns YAMNet's per-window "Snoring" score into snore bursts.
 *
 * YAMNet looks at overlapping ~1 s windows; each window is fed here with its score. A run of
 * snoring windows starts when the score reaches [enterScore] and continues while it stays at or
 * above [stayScore] (hysteresis, so one weaker window inside a snore doesn't split it). Each window
 * accounts for one hop of time, so overlapping windows are not counted twice. Long runs are cut
 * into bursts of at most [maxBurstMs] so the night timeline and episodes stay fine-grained.
 *
 * Pure and fed in time order by the caller, so tests are deterministic.
 */
class YamnetSnoreTracker(
    private val hopMs: Long,
    private val enterScore: Float = DEFAULT_ENTER_SCORE,
    private val stayScore: Float = DEFAULT_STAY_SCORE,
    private val maxBurstMs: Long = DEFAULT_MAX_BURST_MS
) {
    private var runStart = -1L
    private var runEnd = -1L
    private var runPeakDb = 0f

    /** True while the latest windows are snoring. */
    val isSnoring: Boolean get() = runStart >= 0

    /**
     * @param hopStartMs start of the time this window accounts for (one hop, centred in the window)
     * @param snoringScore YAMNet's score for the "Snoring" class, 0–1
     * @param peakDb loudest 100 ms frame inside the window
     * @return bursts finished by this window (usually empty)
     */
    fun onWindow(hopStartMs: Long, snoringScore: Float, peakDb: Float): List<SnoreBurst> {
        val out = mutableListOf<SnoreBurst>()
        val snoring = if (isSnoring) snoringScore >= stayScore else snoringScore >= enterScore
        if (snoring) {
            if (!isSnoring) {
                runStart = hopStartMs
                runPeakDb = peakDb
            }
            runEnd = hopStartMs + hopMs
            runPeakDb = max(runPeakDb, peakDb)
            if (runEnd - runStart >= maxBurstMs) {
                out += SnoreBurst(runStart, runEnd - runStart, runPeakDb)
                runStart = -1L
            }
        } else if (isSnoring) {
            out += SnoreBurst(runStart, runEnd - runStart, runPeakDb)
            runStart = -1L
        }
        return out
    }

    /** The burst still open when the night ends, if any. */
    fun flush(): SnoreBurst? {
        if (!isSnoring) return null
        return SnoreBurst(runStart, runEnd - runStart, runPeakDb).also { runStart = -1L }
    }

    companion object {
        const val DEFAULT_ENTER_SCORE = 0.3f
        const val DEFAULT_STAY_SCORE = 0.15f
        const val DEFAULT_MAX_BURST_MS = 10_000L
    }
}
