package com.lordyhas.sleeplab.service.clips

import kotlin.math.abs

/**
 * Decides which snores of a night get an audio clip, so storage stays bounded:
 * at most [maxClips] per night and at most one per [minSpacingMs]. When a slot is contested,
 * the louder snore wins and the quieter clips are evicted.
 *
 * Keys identify snores (the burst start time is used); pure and deterministic.
 */
class ClipBudget(
    private val maxClips: Int = DEFAULT_MAX_CLIPS,
    private val minSpacingMs: Long = DEFAULT_MIN_SPACING_MS
) {
    sealed interface Decision {
        /** Keep the clip after deleting the clips with keys [evictKeys]. */
        data class Accept(val evictKeys: List<Long>) : Decision
        data object Reject : Decision
    }

    private data class Kept(val key: Long, val peakDb: Float)

    private val kept = mutableListOf<Kept>()

    val size: Int get() = kept.size

    fun offer(key: Long, peakDb: Float): Decision {
        // Spacing: compete with every kept clip that is too close in time.
        val nearby = kept.filter { abs(it.key - key) < minSpacingMs }
        if (nearby.isNotEmpty()) {
            if (nearby.any { it.peakDb >= peakDb }) return Decision.Reject
            kept.removeAll(nearby)
            kept += Kept(key, peakDb)
            return Decision.Accept(nearby.map { it.key })
        }
        // Count: when full, only a snore louder than the quietest kept one gets in.
        if (kept.size < maxClips) {
            kept += Kept(key, peakDb)
            return Decision.Accept(emptyList())
        }
        val quietest = kept.minBy { it.peakDb }
        if (peakDb <= quietest.peakDb) return Decision.Reject
        kept.remove(quietest)
        kept += Kept(key, peakDb)
        return Decision.Accept(listOf(quietest.key))
    }

    companion object {
        const val DEFAULT_MAX_CLIPS = 30
        const val DEFAULT_MIN_SPACING_MS = 30_000L
    }
}
