package com.lordyhas.sleeplab.service.bedtime

import java.util.Calendar
import java.util.TimeZone

/** Clock-time maths for reminders and auto-stop. Times of day are minutes after midnight. */
object BedtimeSchedule {
    /** No night is ever recorded longer than this, auto-stop or not. */
    const val MAX_SESSION_MS = 12L * 60 * 60 * 1000

    /**
     * The next moment strictly after [afterMillis] at which the local clock reads [minuteOfDay].
     * Uses the calendar, so DST days are handled.
     */
    fun nextOccurrence(afterMillis: Long, minuteOfDay: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val cal = Calendar.getInstance(zone).apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= afterMillis) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    /**
     * When a night started at [startMillis] should end: the chosen wake-up time if auto-stop is on,
     * never later than [MAX_SESSION_MS]. A wake-up time less than [MIN_AUTO_STOP_MS] after the start
     * (e.g. starting at 07:25 with stop at 07:30) means tomorrow, not in five minutes.
     */
    fun stopAt(
        startMillis: Long,
        autoStopEnabled: Boolean,
        autoStopMinuteOfDay: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): Long {
        val hardCap = startMillis + MAX_SESSION_MS
        if (!autoStopEnabled) return hardCap
        val wakeUp = nextOccurrence(startMillis + MIN_AUTO_STOP_MS, autoStopMinuteOfDay, zone)
        return minOf(wakeUp, hardCap)
    }

    /** Minimum night length before auto-stop may apply. */
    const val MIN_AUTO_STOP_MS = 60L * 60 * 1000
}
