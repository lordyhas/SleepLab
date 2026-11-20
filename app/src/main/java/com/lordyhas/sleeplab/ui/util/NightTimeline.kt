package com.lordyhas.sleeplab.ui.util

import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity

/** How loud a stretch of the night was, from silence to the loudest snoring. */
enum class SnoreBand { QUIET, LIGHT, LOUD, VERY_LOUD }

/**
 * One slice of the night (10 s by default). [level] is what a chart draws: the loudest snore in
 * the slice if there was one, otherwise the room level. [band] is QUIET outside snoring episodes.
 */
data class TimelineSlot(val startTime: Long, val endTime: Long, val level: Float, val band: SnoreBand)

data class NightTimeline(
    val startTime: Long,
    val endTime: Long,
    val slots: List<TimelineSlot>,
    /** Time spent in each band, in seconds. */
    val bandSeconds: Map<SnoreBand, Long>
) {
    /** Time inside snoring episodes: what people mean by "how long I snored". */
    val snoringSeconds: Long get() = bandSeconds.filterKeys { it != SnoreBand.QUIET }.values.sum()

    /** At most [count] slots for a chart: each keeps the loudest level and band it covers. */
    fun buckets(count: Int): List<TimelineSlot> {
        if (slots.size <= count || count <= 0) return slots
        val per = slots.size.toDouble() / count
        return (0 until count).map { i ->
            val from = (i * per).toInt()
            val to = ((i + 1) * per).toInt().coerceAtMost(slots.size).coerceAtLeast(from + 1)
            val group = slots.subList(from, to)
            TimelineSlot(
                startTime = group.first().startTime,
                endTime = group.last().endTime,
                level = group.maxOf { it.level },
                band = group.maxOf { it.band }
            )
        }
    }
}

object NightTimelines {
    const val SLOT_MS = 10_000L

    /** dB above the detection threshold where a snore becomes LOUD, then VERY_LOUD. */
    const val LOUD_ABOVE_DB = 5f
    const val VERY_LOUD_ABOVE_DB = 10f

    fun bandFor(snoreDb: Float, thresholdDb: Float): SnoreBand {
        val above = snoreDb - thresholdDb
        return when {
            above >= VERY_LOUD_ABOVE_DB -> SnoreBand.VERY_LOUD
            above >= LOUD_ABOVE_DB -> SnoreBand.LOUD
            else -> SnoreBand.LIGHT
        }
    }

    /**
     * Cuts the night into [slotMs] slices. A slice inside a snoring episode gets the band of its
     * loudest snore (LIGHT between two breaths); every other slice is QUIET.
     */
    fun build(
        startTime: Long,
        endTime: Long,
        samples: List<NoiseSampleEntity>,
        events: List<SnoreEventEntity>,
        thresholdDb: Float,
        slotMs: Long = SLOT_MS
    ): NightTimeline {
        if (endTime <= startTime) return NightTimeline(startTime, endTime, emptyList(), emptyMap())
        val sortedSamples = samples.sortedBy { it.timestamp }
        val sortedEvents = events.sortedBy { it.timestamp }
        val episodes = SnoreEpisodes.group(sortedEvents)

        val slots = ArrayList<TimelineSlot>(((endTime - startTime) / slotMs + 1).toInt())
        val bandMs = HashMap<SnoreBand, Long>()
        var sampleIdx = 0
        var eventIdx = 0
        var episodeIdx = 0
        var threshold = thresholdDb
        var lastLevel = sortedSamples.firstOrNull()?.decibelLevel ?: 0f

        var slotStart = startTime
        while (slotStart < endTime) {
            val slotEnd = minOf(slotStart + slotMs, endTime)

            var roomLevel: Float? = null
            while (sampleIdx < sortedSamples.size && sortedSamples[sampleIdx].timestamp < slotEnd) {
                val s = sortedSamples[sampleIdx]
                if (s.timestamp >= slotStart) roomLevel = maxOf(roomLevel ?: s.decibelLevel, s.decibelLevel)
                s.thresholdDb?.let { threshold = it }
                sampleIdx++
            }
            if (roomLevel != null) lastLevel = roomLevel

            var snorePeak: Float? = null
            while (eventIdx < sortedEvents.size && sortedEvents[eventIdx].timestamp < slotEnd) {
                val e = sortedEvents[eventIdx]
                if (e.timestamp >= slotStart) snorePeak = maxOf(snorePeak ?: e.amplitude, e.amplitude)
                eventIdx++
            }

            while (episodeIdx < episodes.size && episodes[episodeIdx].endTime <= slotStart) episodeIdx++
            val inEpisode = episodeIdx < episodes.size && episodes[episodeIdx].startTime < slotEnd

            val band = when {
                !inEpisode -> SnoreBand.QUIET
                snorePeak != null -> bandFor(snorePeak, threshold)
                else -> SnoreBand.LIGHT
            }
            slots += TimelineSlot(slotStart, slotEnd, snorePeak ?: roomLevel ?: lastLevel, band)
            bandMs[band] = (bandMs[band] ?: 0L) + (slotEnd - slotStart)
            slotStart = slotEnd
        }
        return NightTimeline(startTime, endTime, slots, bandMs.mapValues { it.value / 1000 })
    }
}
