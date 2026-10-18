package com.lordyhas.sleeplab.service.detection

import kotlin.math.sqrt

/**
 * Night snore score, 0 (silent) to 100 (snoring most of the night, loudly).
 *
 * Built from the share of the night spent snoring, each episode weighted by how far it rose above
 * the threshold: just over the threshold counts half, [LOUDNESS_RANGE_DB] or more above counts
 * fully. A square-root curve keeps small amounts of snoring visible: 1 % of the night ≈ 20,
 * 5 % ≈ 45, 25 % or more = 100.
 *
 * Replaces the v1 score `(avgDb + maxDb) / 2`, which mostly measured how loud the room was.
 */
object SnoreScore {
    /** Value stored in `SleepSessionEntity.scoreVersion` for scores computed here. */
    const val VERSION = 2

    private const val LOUDNESS_RANGE_DB = 20f
    private const val FULL_SCORE_FRACTION = 0.25f

    /** @param dbAboveThreshold peak level minus the threshold in force when it was detected. */
    data class Episode(val durationSeconds: Int, val dbAboveThreshold: Float)

    fun compute(episodes: List<Episode>, sessionSeconds: Long): Float {
        if (sessionSeconds <= 0 || episodes.isEmpty()) return 0f
        val weightedSeconds = episodes.sumOf { e ->
            val loudness = (e.dbAboveThreshold / LOUDNESS_RANGE_DB).coerceIn(0f, 1f)
            e.durationSeconds * (0.5 + 0.5 * loudness)
        }
        val fraction = (weightedSeconds / sessionSeconds).toFloat()
        return (100f * sqrt(fraction / FULL_SCORE_FRACTION)).coerceIn(0f, 100f)
    }
}
