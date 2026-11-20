package com.lordyhas.sleeplab.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.ui.theme.SnoreHigh
import com.lordyhas.sleeplab.ui.theme.SnoreLow
import com.lordyhas.sleeplab.ui.theme.SnoreMedium
import com.lordyhas.sleeplab.ui.util.SnoreBand
import com.lordyhas.sleeplab.ui.util.TimelineSlot

/** Slate for quiet time, then green / amber / red as snoring gets louder. */
val QuietBandColor = Color(0xFF3A4A68)

fun SnoreBand.color(): Color = when (this) {
    SnoreBand.QUIET -> QuietBandColor
    SnoreBand.LIGHT -> SnoreLow
    SnoreBand.LOUD -> SnoreMedium
    SnoreBand.VERY_LOUD -> SnoreHigh
}

@StringRes
fun SnoreBand.labelRes(): Int = when (this) {
    SnoreBand.QUIET -> R.string.band_quiet
    SnoreBand.LIGHT -> R.string.band_light
    SnoreBand.LOUD -> R.string.band_loud
    SnoreBand.VERY_LOUD -> R.string.band_very_loud
}

/** "5h 30m", "13 min", "<1 min": band totals read at a glance. */
fun formatBandDuration(seconds: Long): String = when {
    seconds >= 3600 -> "${seconds / 3600}h ${String.format(java.util.Locale.ROOT, "%02d", (seconds % 3600) / 60)}m"
    seconds >= 60 -> "${seconds / 60} min"
    seconds > 0 -> "<1 min"
    else -> "0 min"
}

/**
 * A whole night in one strip: one bar per slot, quiet time as a low slate base and snoring as
 * coloured bars whose height follows how loud it was. Used for the rows of the Nights list.
 */
@Composable
fun NightStrip(
    slots: List<TimelineSlot>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (slots.isEmpty()) return@Canvas
        val start = slots.first().startTime
        val span = (slots.last().endTime - start).coerceAtLeast(1).toFloat()
        val base = size.height * 0.28f
        val maxLevel = slots.maxOf { it.level }
        val minLevel = slots.minOf { it.level }
        val range = (maxLevel - minLevel).coerceAtLeast(1f)

        // Quiet base across the whole night
        drawRoundRect(
            color = QuietBandColor,
            topLeft = Offset(0f, size.height - base),
            size = Size(size.width, base),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
        slots.forEach { slot ->
            if (slot.band == SnoreBand.QUIET) return@forEach
            val x = (slot.startTime - start) / span * size.width
            val w = ((slot.endTime - slot.startTime) / span * size.width).coerceAtLeast(1.5f)
            val h = base + (size.height - base) * ((slot.level - minLevel) / range).coerceIn(0.15f, 1f)
            drawRect(slot.band.color(), topLeft = Offset(x, size.height - h), size = Size(w, h))
        }
    }
}
