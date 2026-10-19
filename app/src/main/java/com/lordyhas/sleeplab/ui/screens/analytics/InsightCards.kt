package com.lordyhas.sleeplab.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.data.local.entity.FactorEntity
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.IconBadge
import com.lordyhas.sleeplab.ui.components.Pill
import com.lordyhas.sleeplab.ui.components.icon
import com.lordyhas.sleeplab.ui.components.label
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.theme.SnoreHigh
import com.lordyhas.sleeplab.ui.theme.SnoreLow
import com.lordyhas.sleeplab.ui.theme.SnoreMedium
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Severity of a night's total snoring, same bands as the history list. */
internal fun minutesColor(minutes: Float): Color = when {
    minutes > 30f -> SnoreHigh
    minutes > 10f -> SnoreMedium
    else -> SnoreLow
}

/**
 * Month grid; each night is a coloured cell. Months are addressed as offsets from the current one
 * (0 = this month, -1 = last month…).
 */
@Composable
fun CalendarCard(
    minutesByDay: Map<Int, Float>,
    sessionIdByDay: Map<Int, Int>,
    onNightClick: (sessionId: Int) -> Unit
) {
    var monthOffset by rememberSaveable { mutableIntStateOf(0) }
    val locale = Locale.getDefault()
    val month = remember(monthOffset) {
        Calendar.getInstance(locale).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val title = remember(monthOffset) {
        SimpleDateFormat("LLLL yyyy", locale).format(month.time).replaceFirstChar { it.titlecase(locale) }
    }
    val firstDayOfWeek = month.firstDayOfWeek
    val leadingBlanks = (month.get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
    val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
    val keyBase = month.get(Calendar.YEAR) * 10_000 + (month.get(Calendar.MONTH) + 1) * 100
    val weekdayInitials = remember(locale) {
        val names = DateFormatSymbols(locale).shortWeekdays // index 1 = Sunday
        (0 until 7).map { i -> names[(firstDayOfWeek - 1 + i) % 7 + 1].take(1).uppercase(locale) }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            IconButton(onClick = { monthOffset-- }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
            }
            IconButton(onClick = { monthOffset++ }, enabled = monthOffset < 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayInitials.forEach { initial ->
                Text(
                    initial,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val cells = leadingBlanks + daysInMonth
        val rows = (cells + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (row in 0 until rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val day = row * 7 + col - leadingBlanks + 1
                        if (day < 1 || day > daysInMonth) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val key = keyBase + day
                            DayCell(
                                day = day,
                                minutes = minutesByDay[key],
                                onClick = sessionIdByDay[key]?.let { id -> { onNightClick(id) } },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CalendarLegend(SnoreLow, stringResource(R.string.calendar_low))
            CalendarLegend(SnoreMedium, stringResource(R.string.calendar_medium))
            CalendarLegend(SnoreHigh, stringResource(R.string.calendar_high))
        }
    }
}

@Composable
private fun DayCell(day: Int, minutes: Float?, onClick: (() -> Unit)?, modifier: Modifier) {
    val shape = MaterialTheme.shapes.small
    val color = minutes?.let(::minutesColor)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(color?.copy(alpha = 0.28f) ?: Color.Transparent)
            .border(1.dp, color?.copy(alpha = 0.6f) ?: Color.White.copy(alpha = 0.05f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$day",
            style = MaterialTheme.typography.labelMedium,
            color = if (color != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun CalendarLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(color.copy(alpha = 0.5f))
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** With-vs-without comparison for each factor that has been used at least once. */
@Composable
fun FactorImpactCard(impacts: List<Pair<FactorEntity, FactorImpact>>) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.factors_title), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        if (impacts.isEmpty()) {
            Text(
                stringResource(R.string.factors_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@GlassCard
        }
        impacts.forEach { (factor, impact) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                IconBadge(icon = factor.icon(), tint = SleepIndigoLight, size = 36.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(factor.label(), style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(
                        if (impact.isReliable) {
                            stringResource(R.string.factor_compare, impact.avgWith.toInt(), impact.avgWithout.toInt())
                        } else {
                            pluralStringResource(R.plurals.factor_needs_nights, impact.nightsNeeded, impact.nightsNeeded)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val change = impact.changePercent
                if (impact.isReliable && change != null) {
                    when {
                        change >= 10 -> Pill(stringResource(R.string.factor_more, change), if (change >= 50) SnoreHigh else SnoreMedium)
                        change <= -10 -> Pill(stringResource(R.string.factor_less, -change), SnoreLow)
                        else -> Pill(stringResource(R.string.factor_no_effect), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** Average snoring for each morning rating — does a quieter night feel better? */
@Composable
fun RatingCard(breakdown: List<RatingPoint>, ratedNights: Int) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.rating_vs_snoring_title), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(4.dp))
        if (breakdown.isEmpty()) {
            Text(
                stringResource(R.string.rating_vs_snoring_hint, Insights.MIN_RATED_NIGHTS, ratedNights),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@GlassCard
        }
        Spacer(Modifier.height(8.dp))
        val max = breakdown.maxOf { it.avgSnoreMinutes }.coerceAtLeast(1f)
        val labels = listOf(R.string.rating_1, R.string.rating_2, R.string.rating_3, R.string.rating_4, R.string.rating_5)
        breakdown.reversed().forEach { point ->
            if (point.nights == 0) return@forEach
            ComparisonBar(
                label = "${stringResource(labels[point.rating - 1])} · ${pluralStringResource(R.plurals.nights_count, point.nights, point.nights)}",
                minutes = point.avgSnoreMinutes,
                fraction = point.avgSnoreMinutes / max,
                color = if (point.rating >= 4) SleepTealAccent else if (point.rating == 3) SnoreMedium else SnoreHigh
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}
