package com.lordyhas.sleeplab.ui.screens.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class InsightsTest {

    private val zone = TimeZone.getTimeZone("Europe/Paris")
    private val monday = Locale.FRANCE

    private fun at(month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(2026, month - 1, day, hour, minute)
        }.timeInMillis

    private fun night(id: Int, start: Long, minutes: Float, rating: Int? = null) = NightData(id, start, minutes, rating)

    @Test
    fun `a night after midnight belongs to the previous evening`() {
        assertEquals(20261016, Insights.nightDayKey(at(10, 16, 23, 30), zone))
        assertEquals(20261016, Insights.nightDayKey(at(10, 17, 0, 30), zone))
        assertEquals(20261017, Insights.nightDayKey(at(10, 17, 21, 0), zone))
    }

    @Test
    fun `factor impact compares nights with and without`() {
        val nights = listOf(
            night(1, at(10, 1, 23), 30f), night(2, at(10, 2, 23), 20f), night(3, at(10, 3, 23), 40f),
            night(4, at(10, 4, 23), 10f), night(5, at(10, 5, 23), 10f), night(6, at(10, 6, 23), 10f)
        )
        val alcohol = 7
        val impact = Insights.factorImpacts(nights, mapOf(1 to setOf(alcohol), 2 to setOf(alcohol), 3 to setOf(alcohol)), listOf(alcohol)).single()
        assertEquals(3, impact.nightsWith)
        assertEquals(3, impact.nightsWithout)
        assertEquals(30f, impact.avgWith)
        assertEquals(10f, impact.avgWithout)
        assertEquals(200, impact.changePercent)
        assertTrue(impact.isReliable)
    }

    @Test
    fun `factor impact needs three nights on each side`() {
        val nights = listOf(night(1, at(10, 1, 23), 30f), night(2, at(10, 2, 23), 10f), night(3, at(10, 3, 23), 10f))
        val impact = Insights.factorImpacts(nights, mapOf(1 to setOf(9)), listOf(9)).single()
        assertFalse(impact.isReliable)
        assertEquals(3, impact.nightsNeeded) // 2 more with + 1 more without
    }

    @Test
    fun `no baseline gives no percentage`() {
        val impact = FactorImpact(1, nightsWith = 3, nightsWithout = 3, avgWith = 5f, avgWithout = 0f)
        assertNull(impact.changePercent)
    }

    @Test
    fun `weekly trend groups nights by week starting Monday`() {
        val nights = listOf(
            night(1, at(10, 12, 23), 10f), // Mon 12 Oct
            night(2, at(10, 18, 23), 20f), // Sun 18 Oct, same week
            night(3, at(10, 19, 1), 60f),  // Mon 19 Oct 01:00 → Sunday night, same week
            night(4, at(10, 19, 23), 40f)  // Mon 19 Oct evening → next week
        )
        val weeks = Insights.trend(nights, TrendPeriod.WEEKS, limit = 12, zone = zone, locale = monday)
        assertEquals(2, weeks.size)
        assertEquals(at(10, 12, 0), weeks[0].startMillis)
        assertEquals(3, weeks[0].nights)
        assertEquals(30f, weeks[0].avgSnoreMinutes)
        assertEquals(40f, weeks[1].avgSnoreMinutes)
    }

    @Test
    fun `monthly trend and limit keep the most recent periods`() {
        val nights = (1..14).map { m -> night(m, at(((m - 1) % 12) + 1, 10, 23) + (if (m > 12) 365L * 86_400_000 else 0), m.toFloat()) }
        val months = Insights.trend(nights, TrendPeriod.MONTHS, limit = 12, zone = zone, locale = monday)
        assertEquals(12, months.size)
        assertEquals(14f, months.last().avgSnoreMinutes)
    }

    @Test
    fun `nights trend keeps the last nights with their ids`() {
        val nights = (1..20).map { night(it, at(10, 1, 23) + it * 86_400_000L, it.toFloat()) }
        val points = Insights.trend(nights, TrendPeriod.NIGHTS, limit = 14, zone = zone)
        assertEquals(14, points.size)
        assertEquals(20, points.last().sessionId)
    }

    @Test
    fun `rating breakdown needs enough rated nights`() {
        val few = (1..4).map { night(it, at(10, it, 23), 10f, rating = 3) }
        assertTrue(Insights.ratingBreakdown(few).isEmpty())

        val enough = listOf(
            night(1, at(10, 1, 23), 40f, 1), night(2, at(10, 2, 23), 30f, 2), night(3, at(10, 3, 23), 5f, 5),
            night(4, at(10, 4, 23), 15f, 5), night(5, at(10, 5, 23), 20f, 3), night(6, at(10, 6, 23), 99f, null)
        )
        val breakdown = Insights.ratingBreakdown(enough)
        assertEquals(5, breakdown.size)
        assertEquals(10f, breakdown.first { it.rating == 5 }.avgSnoreMinutes)
        assertEquals(0, breakdown.first { it.rating == 4 }.nights)
    }

    @Test
    fun `minutes by day adds up nights of the same evening`() {
        val nights = listOf(night(1, at(10, 16, 22), 10f), night(2, at(10, 17, 2), 5f))
        assertEquals(mapOf(20261016 to 15f), Insights.minutesByDay(nights, zone))
    }
}
