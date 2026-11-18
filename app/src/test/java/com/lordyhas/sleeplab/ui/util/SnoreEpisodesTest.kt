package com.lordyhas.sleeplab.ui.util

import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoreEpisodesTest {
    private fun snore(atSec: Long, db: Float = 40f, durationSec: Int = 1, clip: String? = null) =
        SnoreEventEntity(sessionId = 1, timestamp = atSec * 1000, amplitude = db, durationSeconds = durationSec, clipPath = clip)

    @Test
    fun `no snores, no episodes`() {
        assertTrue(SnoreEpisodes.group(emptyList()).isEmpty())
    }

    @Test
    fun `snores at breathing cadence form one episode`() {
        val events = (0 until 50).map { snore(it * 6L) }
        val episodes = SnoreEpisodes.group(events)
        assertEquals(1, episodes.size)
        assertEquals(50, episodes[0].snoreCount)
        assertEquals(0L, episodes[0].startTime)
        assertEquals((49 * 6 + 1) * 1000L, episodes[0].endTime)
    }

    @Test
    fun `a pause longer than a minute splits episodes`() {
        val events = listOf(snore(0), snore(6), snore(12), snore(200), snore(206))
        val episodes = SnoreEpisodes.group(events)
        assertEquals(listOf(3, 2), episodes.map { it.snoreCount })
        assertEquals(200_000L, episodes[1].startTime)
    }

    @Test
    fun `gap is measured from the end of the previous snore`() {
        // ends at 4 s, next starts 63 s later: still within the minute
        val episodes = SnoreEpisodes.group(listOf(snore(0, durationSec = 4), snore(63)))
        assertEquals(1, episodes.size)
    }

    @Test
    fun `unsorted input is ordered first`() {
        val episodes = SnoreEpisodes.group(listOf(snore(12), snore(0), snore(6)))
        assertEquals(listOf(0L, 6_000L, 12_000L), episodes[0].events.map { it.timestamp })
    }

    @Test
    fun `peak and clip come from the loudest snores`() {
        val episode = SnoreEpisodes.group(
            listOf(snore(0, 38f, clip = "a.m4a"), snore(6, 45f), snore(12, 42f, clip = "b.m4a"))
        ).single()
        assertEquals(45f, episode.peakDb)
        assertEquals("b.m4a", episode.clipPath)
        assertNull(SnoreEpisodes.group(listOf(snore(0))).single().clipPath)
    }
}
