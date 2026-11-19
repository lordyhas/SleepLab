package com.lordyhas.sleeplab.ui.util

import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity

/**
 * Nights shorter than this (a quick test, a false start) are still listed and can be opened,
 * but are left out of averages, records, trends and insights so they don't skew them.
 */
const val MIN_STATS_NIGHT_MS = 60 * 60 * 1000L

/** How long after a night ends it is still called "last night" (and the morning prompt shows). */
const val LAST_NIGHT_WINDOW_MS = 18 * 60 * 60 * 1000L

fun isShortNight(startTime: Long, endTime: Long): Boolean = endTime - startTime < MIN_STATS_NIGHT_MS

val SleepSessionEntity.isShortNight: Boolean get() = isShortNight(startTime, endTime)

/** Completed nights that count in statistics. */
fun List<SleepSessionEntity>.forStats(): List<SleepSessionEntity> = filter { it.endTime > 0 && !it.isShortNight }

/** True while [endTime] is recent enough to call that night "last night". */
fun isLastNight(endTime: Long, now: Long = System.currentTimeMillis()): Boolean =
    endTime > 0 && now - endTime in 0 until LAST_NIGHT_WINDOW_MS
