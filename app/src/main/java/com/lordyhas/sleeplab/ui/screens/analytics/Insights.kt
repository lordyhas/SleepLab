package com.lordyhas.sleeplab.ui.screens.analytics

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/** The parts of a night the insights need; keeps the calculator independent of Room. */
data class NightData(
    val sessionId: Int,
    val startTime: Long,
    val snoreMinutes: Float,
    val morningRating: Int?
)

enum class TrendPeriod { NIGHTS, WEEKS, MONTHS }

data class TrendPoint(
    /** Start of the period (for NIGHTS: the night's start time). */
    val startMillis: Long,
    val avgSnoreMinutes: Float,
    val nights: Int,
    /** For NIGHTS only: the night, so taps can open it. */
    val sessionId: Int? = null
)

data class FactorImpact(
    val factorId: Int,
    val nightsWith: Int,
    val nightsWithout: Int,
    val avgWith: Float,
    val avgWithout: Float
) {
    /** True once both sides have enough nights to compare. */
    val isReliable: Boolean get() = nightsWith >= MIN_NIGHTS && nightsWithout >= MIN_NIGHTS

    /** Positive = more snoring with the factor. Null when nothing to compare against. */
    val changePercent: Int?
        get() = if (avgWithout > 0f) ((avgWith - avgWithout) / avgWithout * 100f).roundToInt() else null

    /** Nights still needed (on the scarcer side) before the comparison is shown. */
    val nightsNeeded: Int get() = maxOf(0, MIN_NIGHTS - nightsWith) + maxOf(0, MIN_NIGHTS - nightsWithout)

    companion object {
        const val MIN_NIGHTS = 3
    }
}

data class RatingPoint(val rating: Int, val avgSnoreMinutes: Float, val nights: Int)

/**
 * Pure insight maths over completed nights. Dates follow the evening a night belongs to:
 * a night started at 00:30 counts for the previous day ([nightDayKey]).
 */
object Insights {
    /** Minimum rated nights before the rating chart means anything. */
    const val MIN_RATED_NIGHTS = 5

    private const val NIGHT_OFFSET_MS = 12L * 60 * 60 * 1000

    /** yyyyMMdd of the evening the night belongs to. */
    fun nightDayKey(startTime: Long, zone: TimeZone = TimeZone.getDefault()): Int {
        val cal = Calendar.getInstance(zone).apply { timeInMillis = startTime - NIGHT_OFFSET_MS }
        return cal.get(Calendar.YEAR) * 10_000 + (cal.get(Calendar.MONTH) + 1) * 100 + cal.get(Calendar.DAY_OF_MONTH)
    }

    fun factorImpacts(
        nights: List<NightData>,
        factorsByNight: Map<Int, Set<Int>>,
        factorIds: Collection<Int>
    ): List<FactorImpact> = factorIds.map { factorId ->
        val (with, without) = nights.partition { factorId in factorsByNight[it.sessionId].orEmpty() }
        FactorImpact(
            factorId = factorId,
            nightsWith = with.size,
            nightsWithout = without.size,
            avgWith = with.averageSnore(),
            avgWithout = without.averageSnore()
        )
    }

    fun trend(
        nights: List<NightData>,
        period: TrendPeriod,
        limit: Int,
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): List<TrendPoint> {
        val sorted = nights.sortedBy { it.startTime }
        if (period == TrendPeriod.NIGHTS) {
            return sorted.takeLast(limit).map { TrendPoint(it.startTime, it.snoreMinutes, 1, it.sessionId) }
        }
        return sorted
            .groupBy { periodStart(it.startTime, period, zone, locale) }
            .map { (start, group) -> TrendPoint(start, group.averageSnore(), group.size) }
            .sortedBy { it.startMillis }
            .takeLast(limit)
    }

    fun ratingBreakdown(nights: List<NightData>): List<RatingPoint> {
        val rated = nights.filter { it.morningRating != null }
        if (rated.size < MIN_RATED_NIGHTS) return emptyList()
        return (1..5).map { rating ->
            val group = rated.filter { it.morningRating == rating }
            RatingPoint(rating, group.averageSnore(), group.size)
        }
    }

    /** Total snore minutes per night-day, for the calendar heatmap (several nights on one day add up). */
    fun minutesByDay(nights: List<NightData>, zone: TimeZone = TimeZone.getDefault()): Map<Int, Float> =
        nights.groupBy { nightDayKey(it.startTime, zone) }.mapValues { (_, g) -> g.sumOf { it.snoreMinutes.toDouble() }.toFloat() }

    /** Start of the week (locale's first day) or month containing the night's evening. */
    fun periodStart(
        startTime: Long,
        period: TrendPeriod,
        zone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): Long {
        val cal = Calendar.getInstance(zone, locale).apply {
            timeInMillis = startTime - NIGHT_OFFSET_MS
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        when (period) {
            TrendPeriod.WEEKS -> {
                val diff = (cal.get(Calendar.DAY_OF_WEEK) - cal.firstDayOfWeek + 7) % 7
                cal.add(Calendar.DAY_OF_YEAR, -diff)
            }
            TrendPeriod.MONTHS -> cal.set(Calendar.DAY_OF_MONTH, 1)
            TrendPeriod.NIGHTS -> Unit
        }
        return cal.timeInMillis
    }

    private fun List<NightData>.averageSnore(): Float =
        if (isEmpty()) 0f else sumOf { it.snoreMinutes.toDouble() }.toFloat() / size
}
