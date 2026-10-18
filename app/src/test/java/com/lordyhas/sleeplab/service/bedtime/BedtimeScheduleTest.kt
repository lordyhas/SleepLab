package com.lordyhas.sleeplab.service.bedtime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BedtimeScheduleTest {

    private val paris = TimeZone.getTimeZone("Europe/Paris")

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(paris).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    private val h = 60L * 60 * 1000

    @Test
    fun `next occurrence later today`() {
        assertEquals(at(2026, 10, 16, 22, 30), BedtimeSchedule.nextOccurrence(at(2026, 10, 16, 20, 0), 22 * 60 + 30, paris))
    }

    @Test
    fun `next occurrence rolls to tomorrow once passed`() {
        assertEquals(at(2026, 10, 17, 22, 30), BedtimeSchedule.nextOccurrence(at(2026, 10, 16, 22, 30), 22 * 60 + 30, paris))
        assertEquals(at(2026, 10, 17, 22, 30), BedtimeSchedule.nextOccurrence(at(2026, 10, 16, 23, 0), 22 * 60 + 30, paris))
    }

    @Test
    fun `next occurrence keeps clock time across the DST change`() {
        // Clocks go back on Sun 25 Oct 2026 in Paris: that day lasts 25 h
        assertEquals(at(2026, 10, 25, 7, 30), BedtimeSchedule.nextOccurrence(at(2026, 10, 24, 23, 0), 7 * 60 + 30, paris))
    }

    @Test
    fun `auto-stop at the morning after an evening start`() {
        assertEquals(at(2026, 10, 17, 7, 30), BedtimeSchedule.stopAt(at(2026, 10, 16, 23, 15), true, 7 * 60 + 30, paris))
    }

    @Test
    fun `auto-stop after midnight start is the same morning`() {
        assertEquals(at(2026, 10, 17, 7, 30), BedtimeSchedule.stopAt(at(2026, 10, 17, 1, 0), true, 7 * 60 + 30, paris))
    }

    @Test
    fun `starting just before the wake-up time does not stop minutes later`() {
        val start = at(2026, 10, 17, 7, 10)
        // next 07:30 at least an hour away is tomorrow, but the 12 h cap comes first
        assertEquals(start + 12 * h, BedtimeSchedule.stopAt(start, true, 7 * 60 + 30, paris))
    }

    @Test
    fun `without auto-stop nights are capped at 12 hours`() {
        val start = at(2026, 10, 16, 23, 0)
        assertEquals(start + 12 * h, BedtimeSchedule.stopAt(start, false, 7 * 60 + 30, paris))
    }
}
