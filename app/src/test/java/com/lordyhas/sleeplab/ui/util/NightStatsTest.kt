package com.lordyhas.sleeplab.ui.util

import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightStatsTest {
    private val hour = 60 * 60 * 1000L

    private fun night(id: Int, start: Long, durationMs: Long) =
        SleepSessionEntity(id = id, startTime = start, endTime = start + durationMs)

    @Test
    fun `a night under an hour is short`() {
        assertTrue(isShortNight(0, 69_000))
        assertTrue(isShortNight(0, hour - 1))
        assertFalse(isShortNight(0, hour))
    }

    @Test
    fun `stats keep only completed nights of an hour or more`() {
        val nights = listOf(
            night(1, 0, 10 * hour),
            night(2, 0, 60_000),
            SleepSessionEntity(id = 3, startTime = 0, endTime = 0),
            night(4, 0, 2 * hour)
        )
        assertEquals(listOf(1, 4), nights.forStats().map { it.id })
    }

    @Test
    fun `last night only while the night ended recently`() {
        val now = 100 * hour
        assertTrue(isLastNight(endTime = now - 7 * hour, now = now))
        assertFalse(isLastNight(endTime = now - 3 * 24 * hour, now = now))
        assertFalse(isLastNight(endTime = 0, now = now))
    }
}
