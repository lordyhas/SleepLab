package com.lordyhas.sleeplab.ui.screens.sessiondetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lordyhas.sleeplab.ui.components.QuietBandColor
import com.lordyhas.sleeplab.ui.components.color
import com.lordyhas.sleeplab.ui.components.formatBandDuration
import com.lordyhas.sleeplab.ui.components.labelRes
import com.lordyhas.sleeplab.ui.util.NightTimeline
import com.lordyhas.sleeplab.ui.util.SnoreBand
import java.util.Calendar

/** dB levels are relative and a quiet room reads ~5–10, so the axis follows the night's own range. */
internal fun chartDbRange(values: List<Float>): Pair<Float, Float> {
    if (values.isEmpty()) return 0f to 40f
    val low = (kotlin.math.floor((values.min() - 2f) / 10f) * 10f).coerceAtLeast(0f)
    var high = kotlin.math.ceil((values.max() + 2f) / 10f) * 10f
    if (high - low < 20f) high = low + 20f
    return low to high
}

/**
 * The whole night at a glance: one bar per slice, coloured by how loud the snoring was (slate when
 * quiet). Touch or slide along it to pick a moment; [onSelect] gets the time under the finger.
 */
@Composable
fun NightIntensityChart(
    timeline: NightTimeline,
    selectedTime: Long?,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = Color.White.copy(alpha = 0.06f)
    val markerColor = MaterialTheme.colorScheme.primary
    val (minDb, maxDb) = remember(timeline) { chartDbRange(timeline.slots.map { it.level }) }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val start = timeline.startTime
    val span = (timeline.endTime - start).coerceAtLeast(1)

    fun timeAt(x: Float, width: Float): Long = start + (x / width).coerceIn(0f, 1f).times(span).toLong()

    Canvas(
        modifier = modifier
            .pointerInput(timeline) {
                detectTapGestures { currentOnSelect(timeAt(it.x, size.width.toFloat())) }
            }
            .pointerInput(timeline) {
                detectHorizontalDragGestures(
                    onDragStart = { currentOnSelect(timeAt(it.x, size.width.toFloat())) }
                ) { change, _ ->
                    change.consume()
                    currentOnSelect(timeAt(change.position.x, size.width.toFloat()))
                }
            }
    ) {
        val labelHeight = 20.dp.toPx()
        val chartHeight = size.height - labelHeight
        val markerSpace = 10.dp.toPx()
        val barsHeight = chartHeight - markerSpace
        fun xFor(t: Long) = (t - start).toFloat() / span * size.width
        fun hFor(level: Float) = ((level - minDb) / (maxDb - minDb)).coerceIn(0.04f, 1f) * barsHeight

        // Two faint guides, like the hour grid of a chart
        listOf(0.4f, 0.75f).forEach { f ->
            val y = chartHeight - barsHeight * f
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        // One bar per ~2 px: dense enough to look continuous, loudest moment kept per bar
        val buckets = timeline.buckets((size.width / 2.dp.toPx()).toInt().coerceAtLeast(1))
        buckets.forEach { slot ->
            val x = xFor(slot.startTime)
            val w = (xFor(slot.endTime) - x).coerceAtLeast(1f)
            val h = if (slot.band == SnoreBand.QUIET) hFor(slot.level).coerceAtMost(barsHeight * 0.22f) else hFor(slot.level)
            drawRect(
                color = if (slot.band == SnoreBand.QUIET) QuietBandColor else slot.band.color(),
                topLeft = Offset(x, chartHeight - h),
                size = Size(w + 0.5f, h)
            )
        }

        // Hour ticks, thinned on long nights so the labels never touch
        val cal = Calendar.getInstance().apply {
            timeInMillis = start
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.HOUR_OF_DAY, 1)
        }
        val hours = (span / 3_600_000L).toInt()
        val step = when { hours > 10 -> 3; hours > 5 -> 2; else -> 1 }
        while (cal.timeInMillis < timeline.endTime) {
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (hour % step == 0) {
                val x = xFor(cal.timeInMillis)
                val label = textMeasurer.measure("%02d:00".format(hour), labelStyle)
                val lx = (x - label.size.width / 2f).coerceIn(0f, size.width - label.size.width)
                drawText(label, topLeft = Offset(lx, chartHeight + 4.dp.toPx()))
            }
            cal.add(Calendar.HOUR_OF_DAY, 1)
        }

        // Selected moment: a line with a dot on top of the bar under it
        if (selectedTime != null) {
            val x = xFor(selectedTime)
            val slot = timeline.slots.firstOrNull { selectedTime < it.endTime } ?: timeline.slots.lastOrNull()
            val h = slot?.let { if (it.band == SnoreBand.QUIET) hFor(it.level).coerceAtMost(barsHeight * 0.22f) else hFor(it.level) } ?: 0f
            drawLine(markerColor, Offset(x, 0f), Offset(x, chartHeight), strokeWidth = 1.5.dp.toPx())
            drawCircle(markerColor, radius = 5.dp.toPx(), center = Offset(x, chartHeight - h))
            drawCircle(Color(0xFF0B1022), radius = 2.5.dp.toPx(), center = Offset(x, chartHeight - h))
        }
    }
}

/** Colour key with the time spent in each band. */
@Composable
fun BandLegend(timeline: NightTimeline, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SnoreBand.entries.forEach { band ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(width = 4.dp, height = 28.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .drawBehind { drawRect(band.color()) }
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        stringResource(band.labelRes()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatBandDuration(timeline.bandSeconds[band] ?: 0L),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

/** Shared height so the chart card keeps its size while data loads. */
val NightChartHeight = 220.dp
internal val ChartPlaceholderModifier = Modifier.fillMaxWidth().height(NightChartHeight)
