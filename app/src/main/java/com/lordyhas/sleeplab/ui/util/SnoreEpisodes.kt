package com.lordyhas.sleeplab.ui.util

import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity

/**
 * A stretch of snoring: consecutive snores (one per breath) with no pause longer than
 * [SnoreEpisodes.MAX_GAP_MS] between them. This is what people mean by "I snored from 1 to 1:30".
 */
data class SnoreEpisode(
    val startTime: Long,
    val endTime: Long,
    /** Snores (breaths) in the episode, in time order. */
    val events: List<SnoreEventEntity>
) {
    val durationMs: Long get() = endTime - startTime
    val snoreCount: Int get() = events.size
    val peakDb: Float get() = events.maxOf { it.amplitude }

    /** The loudest snore of the episode that has a saved clip, if any. */
    val clipPath: String? get() = events.filter { it.clipPath != null }.maxByOrNull { it.amplitude }?.clipPath
}

object SnoreEpisodes {
    /** Snores further apart than this start a new episode (a breath is 2–9 s). */
    const val MAX_GAP_MS = 60_000L

    fun group(events: List<SnoreEventEntity>, maxGapMs: Long = MAX_GAP_MS): List<SnoreEpisode> {
        val episodes = mutableListOf<SnoreEpisode>()
        var current = mutableListOf<SnoreEventEntity>()
        var currentEnd = 0L
        for (event in events.sortedBy { it.timestamp }) {
            val eventEnd = event.timestamp + event.durationSeconds * 1000L
            if (current.isNotEmpty() && event.timestamp - currentEnd > maxGapMs) {
                episodes += SnoreEpisode(current.first().timestamp, currentEnd, current)
                current = mutableListOf()
            }
            current += event
            currentEnd = if (current.size == 1) eventEnd else maxOf(currentEnd, eventEnd)
        }
        if (current.isNotEmpty()) episodes += SnoreEpisode(current.first().timestamp, currentEnd, current)
        return episodes
    }
}
