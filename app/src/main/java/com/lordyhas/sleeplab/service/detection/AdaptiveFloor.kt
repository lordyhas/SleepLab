package com.lordyhas.sleeplab.service.detection

import com.lordyhas.sleeplab.service.NoiseFloorCalibrator

/**
 * Keeps the snore threshold tracking the room during the night (heating, a fan turning on or off).
 *
 * Levels are averaged per second over a rolling [windowSeconds] window. Every [updateEveryMs] the
 * floor is re-estimated as a low percentile of that window — quiet moments between breaths, so
 * snoring itself barely moves it — and the threshold follows, by at most [maxStepDb] per update.
 */
class AdaptiveFloor(
    initialFloorDb: Float,
    private val marginDb: Float = NoiseFloorCalibrator.DEFAULT_MARGIN_DB,
    private val minThresholdDb: Float = NoiseFloorCalibrator.MIN_THRESHOLD_DB,
    private val maxThresholdDb: Float = NoiseFloorCalibrator.MAX_THRESHOLD_DB,
    private val windowSeconds: Int = 300,
    private val minSecondsForUpdate: Int = 60,
    private val updateEveryMs: Long = 60_000,
    private val maxStepDb: Float = 2f
) {
    var floorDb: Float = initialFloorDb
        private set

    var thresholdDb: Float = threshold(initialFloorDb)
        private set

    private val perSecond = ArrayDeque<Float>()
    private var secondSum = 0.0
    private var secondMs = 0L
    private var secondFrames = 0
    private var sinceUpdateMs = 0L

    /** Feeds one frame; returns true when [thresholdDb] changed. */
    fun onFrame(db: Float, frameMs: Long): Boolean {
        secondSum += db
        secondFrames++
        secondMs += frameMs
        if (secondMs >= 1_000) {
            perSecond.addLast((secondSum / secondFrames).toFloat())
            if (perSecond.size > windowSeconds) perSecond.removeFirst()
            secondSum = 0.0
            secondFrames = 0
            secondMs -= 1_000
        }

        sinceUpdateMs += frameMs
        if (sinceUpdateMs < updateEveryMs || perSecond.size < minSecondsForUpdate) return false
        sinceUpdateMs = 0

        val sorted = perSecond.sorted()
        val measuredFloor = sorted[((sorted.size - 1) * FLOOR_PERCENTILE).toInt()]
        val target = threshold(measuredFloor)
        val step = (target - thresholdDb).coerceIn(-maxStepDb, maxStepDb)
        if (step == 0f) return false
        thresholdDb += step
        floorDb = thresholdDb - marginDb
        return true
    }

    private fun threshold(floor: Float) = (floor + marginDb).coerceIn(minThresholdDb, maxThresholdDb)

    private companion object {
        const val FLOOR_PERCENTILE = 0.25f
    }
}
