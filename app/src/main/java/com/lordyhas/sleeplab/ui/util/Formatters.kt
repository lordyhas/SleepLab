package com.lordyhas.sleeplab.ui.util

import java.util.Locale

/** "01:02:03" — used for live timers. */
fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
}

/** Compact human duration: "7h 32m", "12m 05s", "45s". */
fun formatDuration(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return when {
        h > 0 -> "${h}h ${String.format(Locale.ROOT, "%02d", m)}m"
        m > 0 -> "${m}m ${String.format(Locale.ROOT, "%02d", sec)}s"
        else -> "${sec}s"
    }
}

fun sessionDurationSeconds(startTime: Long, endTime: Long): Long =
    ((endTime - startTime) / 1000).coerceAtLeast(0)
