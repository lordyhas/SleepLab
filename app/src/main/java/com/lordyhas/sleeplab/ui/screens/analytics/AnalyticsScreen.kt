package com.lordyhas.sleeplab.ui.screens.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.ui.components.Dot
import com.lordyhas.sleeplab.ui.components.EmptyState
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.IconBadge
import com.lordyhas.sleeplab.ui.components.MetricTile
import com.lordyhas.sleeplab.ui.components.Pill
import com.lordyhas.sleeplab.ui.components.ScreenHeader
import com.lordyhas.sleeplab.ui.components.SectionHeader
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.theme.SleepIndigoPrimary
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.theme.SnoreHigh
import com.lordyhas.sleeplab.ui.theme.SnoreLow
import com.lordyhas.sleeplab.ui.theme.SnoreMedium
import com.lordyhas.sleeplab.ui.util.formatDuration
import com.lordyhas.sleeplab.ui.util.sessionDurationSeconds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil

private const val CHART_NIGHTS = 14

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateToSessionDetail: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarTop + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(title = stringResource(R.string.analytics_and_efficiency)) }

        if (!uiState.isLoaded) return@LazyColumn

        if (uiState.sessionsWithTreatments.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.BarChart,
                    title = stringResource(R.string.insufficient_data),
                    message = stringResource(R.string.record_first_nights_unlock),
                    tint = SleepIndigoLight
                )
            }
            return@LazyColumn
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.nights_tracked),
                    value = "${uiState.sessionsWithTreatments.size}",
                    icon = Icons.Default.BarChart,
                    tint = SleepIndigoLight
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.avg_per_night),
                    value = stringResource(R.string.minutes_value, uiState.averageSnoreMinutes.toInt()),
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    tint = SnoreMedium
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.time_in_bed),
                    value = formatDuration(uiState.averageSleepSeconds),
                    icon = Icons.Default.Schedule,
                    tint = SleepTealAccent
                )
            }
        }

        uiState.overallImprovementPercentage?.let { improvement ->
            item {
                TreatmentImpactHeroCard(
                    improvementPercent = improvement,
                    avgNoTreatment = uiState.averageSnoreWithoutTreatmentMinutes,
                    avgWithTreatment = uiState.averageSnoreWithTreatmentMinutes
                )
            }
        }

        item { SnoreEvolutionChartCard(sessions = uiState.sessionsWithTreatments.takeLast(CHART_NIGHTS)) }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.detail_by_treatment))
        }

        val maxImpactMinutes = uiState.treatmentImpacts.maxOfOrNull { it.averageSnoreMinutes } ?: 0f
        items(uiState.treatmentImpacts, key = { it.treatmentId ?: -1 }) { impact ->
            TreatmentPerformanceCard(impact = impact, maxMinutes = maxImpactMinutes)
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.nights_history))
        }

        items(uiState.sessionsWithTreatments.reversed(), key = { it.session.id }) { item ->
            SessionHistoryRow(
                item = item,
                onClick = { onNavigateToSessionDetail(item.session.id) }
            )
        }
    }
}

@Composable
private fun TreatmentImpactHeroCard(
    improvementPercent: Float,
    avgNoTreatment: Float,
    avgWithTreatment: Float
) {
    val isPositive = improvementPercent > 0
    val accent = if (isPositive) SleepTealAccent else SnoreMedium
    val maxValue = maxOf(avgNoTreatment, avgWithTreatment, 1f)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                icon = if (isPositive) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                tint = accent
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.medical_impact), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(if (isPositive) R.string.observed_reduction else R.string.data_stabilizing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = if (isPositive) "−${improvementPercent.toInt()}%" else "+${abs(improvementPercent).toInt()}%",
            style = MaterialTheme.typography.displayMedium,
            color = accent
        )
        Text(
            stringResource(R.string.vs_no_treatment),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        ComparisonBar(
            label = stringResource(R.string.no_treatment),
            minutes = avgNoTreatment,
            fraction = avgNoTreatment / maxValue,
            color = SleepIndigoPrimary
        )
        Spacer(Modifier.height(10.dp))
        ComparisonBar(
            label = stringResource(R.string.with_treatment),
            minutes = avgWithTreatment,
            fraction = avgWithTreatment / maxValue,
            color = SleepTealAccent
        )
    }
}

@Composable
private fun ComparisonBar(label: String, minutes: Float, fraction: Float, color: Color) {
    val animated by animateFloatAsState(fraction.coerceIn(0.02f, 1f), tween(800), label = "comparisonBar")
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(R.string.avg_snore_val, minutes.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animated)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun SnoreEvolutionChartCard(sessions: List<SessionWithTreatment>) {
    val dateFormat = remember { SimpleDateFormat("d/M", Locale.getDefault()) }
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = Color.White.copy(alpha = 0.07f)
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    val growth = remember { Animatable(0f) }
    LaunchedEffect(sessions.size) {
        growth.snapTo(0f)
        growth.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }

    val maxMinutes = sessions.maxOfOrNull { it.session.totalSnoreDurationSeconds / 60f } ?: 0f
    // Round the axis up to a friendly step so gridline labels are clean numbers.
    val axisStep = when {
        maxMinutes <= 15f -> 5f
        maxMinutes <= 60f -> 15f
        else -> 30f
    }
    val axisMax = (ceil(maxMinutes / axisStep) * axisStep).coerceAtLeast(axisStep)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.snoring_evolution), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendItem(SleepIndigoPrimary, stringResource(R.string.no_treatment))
            LegendItem(SleepTealAccent, stringResource(R.string.with_treatment))
        }
        Spacer(Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val axisWidth = 28.dp.toPx()
            val bottomLabelHeight = 18.dp.toPx()
            val chartLeft = axisWidth
            val chartWidth = size.width - axisWidth
            val chartHeight = size.height - bottomLabelHeight

            // Gridlines + Y labels
            val gridCount = (axisMax / axisStep).toInt()
            for (i in 0..gridCount) {
                val value = i * axisStep
                val y = chartHeight - (value / axisMax) * chartHeight
                drawLine(
                    color = gridColor,
                    start = Offset(chartLeft, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (i == 0) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
                val label = textMeasurer.measure("${value.toInt()}", labelStyle)
                drawText(
                    label,
                    topLeft = Offset(0f, (y - label.size.height / 2f).coerceIn(0f, chartHeight - label.size.height))
                )
            }

            val count = sessions.size
            if (count == 0) return@Canvas
            val slot = chartWidth / count
            val barWidth = (slot * 0.56f).coerceAtMost(22.dp.toPx())
            // Skip date labels when bars are too dense to fit them.
            val labelEvery = if (count > 7) 2 else 1

            sessions.forEachIndexed { index, item ->
                val minutes = item.session.totalSnoreDurationSeconds / 60f
                val barHeight = ((minutes / axisMax) * chartHeight * growth.value).coerceAtLeast(3.dp.toPx())
                val centerX = chartLeft + slot * index + slot / 2f
                val color = if (item.session.treatmentId != null) SleepTealAccent else SleepIndigoPrimary

                drawRoundRect(
                    color = color,
                    topLeft = Offset(centerX - barWidth / 2f, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )

                if ((count - 1 - index) % labelEvery == 0) {
                    val dateLabel = textMeasurer.measure(dateFormat.format(Date(item.session.startTime)), labelStyle)
                    drawText(
                        dateLabel,
                        topLeft = Offset(centerX - dateLabel.size.width / 2f, chartHeight + 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color = color)
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TreatmentPerformanceCard(impact: TreatmentImpact, maxMinutes: Float) {
    val isBaseline = impact.treatmentId == null
    val color = if (isBaseline) SleepIndigoPrimary else SleepTealAccent

    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    impact.treatmentName ?: stringResource(R.string.no_treatment),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
                Text(
                    pluralStringResource(R.plurals.nights_count, impact.sessionCount, impact.sessionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val reduction = impact.reductionPercentage
            when {
                isBaseline -> Pill(stringResource(R.string.baseline_label), MaterialTheme.colorScheme.onSurfaceVariant)
                reduction != null && reduction > 0 ->
                    Pill(stringResource(R.string.less_snoring, reduction.toInt()), SleepTealAccent)
                reduction != null ->
                    Pill(stringResource(R.string.more_snoring, abs(reduction).toInt()), SnoreMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        ComparisonBar(
            label = stringResource(R.string.avg_per_night),
            minutes = impact.averageSnoreMinutes,
            fraction = if (maxMinutes > 0f) impact.averageSnoreMinutes / maxMinutes else 0f,
            color = color
        )
    }
}

@Composable
private fun SessionHistoryRow(
    item: SessionWithTreatment,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()) }
    val snoreMin = item.session.totalSnoreDurationSeconds / 60
    val severity = when {
        snoreMin > 30 -> SnoreHigh
        snoreMin > 10 -> SnoreMedium
        else -> SnoreLow
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 36.dp)
                    .clip(CircleShape)
                    .background(severity)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(item.session.startTime)).replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    item.treatmentName ?: stringResource(R.string.no_treatment),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.treatmentName != null) SleepTealAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.snore_minutes_short, snoreMin),
                    style = MaterialTheme.typography.labelLarge,
                    color = severity
                )
                Text(
                    formatDuration(sessionDurationSeconds(item.session.startTime, item.session.endTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
