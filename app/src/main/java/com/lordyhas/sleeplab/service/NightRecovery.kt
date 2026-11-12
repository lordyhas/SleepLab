package com.lordyhas.sleeplab.service

import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import com.lordyhas.sleeplab.service.detection.SnoreScore

/**
 * Closes a night whose recording was cut short (process killed, crash, reboot) from what had
 * already been written: the session row is still open (`endTime == 0`) but its snore events and
 * 10-second noise samples are in the database.
 *
 * Pure Kotlin so it can be unit-tested.
 */
object NightRecovery {

    /** Shorter than this, an interrupted night holds nothing worth keeping. */
    const val MIN_KEEP_MS = 60_000L

    data class Recovered(
        val endTime: Long,
        val totalSnoreDurationSeconds: Int,
        val snoreIntensityScore: Float
    )

    /**
     * @return how to complete [session], or null when too little was recorded and the night
     *   should be deleted instead.
     */
    fun recover(
        session: SleepSessionEntity,
        events: List<SnoreEventEntity>,
        samples: List<NoiseSampleEntity>
    ): Recovered? {
        // The last thing written is the best evidence of when recording stopped.
        val lastSample = samples.maxOfOrNull { it.timestamp } ?: 0L
        val lastEvent = events.maxOfOrNull { it.timestamp + it.durationSeconds * 1000L } ?: 0L
        val endTime = maxOf(lastSample, lastEvent)
        if (endTime - session.startTime < MIN_KEEP_MS) return null

        val sortedSamples = samples.sortedBy { it.timestamp }
        val episodes = events.map { event ->
            // The threshold can move during the night (adaptive floor); samples record it.
            val threshold = sortedSamples.lastOrNull { it.timestamp <= event.timestamp }?.thresholdDb
                ?: sortedSamples.firstOrNull()?.thresholdDb
                ?: session.thresholdDb
            SnoreScore.Episode(event.durationSeconds, event.amplitude - threshold)
        }
        return Recovered(
            endTime = endTime,
            totalSnoreDurationSeconds = events.sumOf { it.durationSeconds },
            snoreIntensityScore = SnoreScore.compute(episodes, (endTime - session.startTime) / 1000)
        )
    }
}
