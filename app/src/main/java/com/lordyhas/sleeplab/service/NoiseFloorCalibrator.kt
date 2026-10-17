package com.lordyhas.sleeplab.service

data class CalibrationResult(
    /** Typical background level of the room (dB). */
    val noiseFloorDb: Float,
    /** Snore threshold derived from the floor (dB). */
    val thresholdDb: Float
)

/**
 * Measures the room's background noise during the first [durationMs] of a night and derives a
 * snore threshold from it.
 *
 * The floor is a low percentile of the measured levels rather than the mean, so a cough, a door
 * or a few words while settling in don't push the threshold up.
 */
class NoiseFloorCalibrator(
    private val durationMs: Long = DEFAULT_DURATION_MS,
    private val marginDb: Float = DEFAULT_MARGIN_DB,
    private val minThresholdDb: Float = MIN_THRESHOLD_DB,
    private val maxThresholdDb: Float = MAX_THRESHOLD_DB
) {
    private val levels = ArrayList<Float>()
    private var elapsedMs = 0L

    val progress: Float
        get() = (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)

    val isComplete: Boolean
        get() = elapsedMs >= durationMs && levels.isNotEmpty()

    /** Records one analysis window of [windowMs] at level [db]. Ignored once complete. */
    fun add(db: Float, windowMs: Long) {
        if (isComplete) return
        levels += db
        elapsedMs += windowMs
    }

    /** The result so far, or null if nothing was measured. */
    fun result(): CalibrationResult? {
        if (levels.isEmpty()) return null
        val sorted = levels.sorted()
        val floor = sorted[((sorted.size - 1) * FLOOR_PERCENTILE).toInt()]
        return CalibrationResult(
            noiseFloorDb = floor,
            thresholdDb = (floor + marginDb).coerceIn(minThresholdDb, maxThresholdDb)
        )
    }

    companion object {
        const val DEFAULT_DURATION_MS = 30_000L
        /** How far above the room's background a sound must be to count as snoring. */
        const val DEFAULT_MARGIN_DB = 12f
        const val MIN_THRESHOLD_DB = 35f
        const val MAX_THRESHOLD_DB = 75f
        private const val FLOOR_PERCENTILE = 0.25f
    }
}
