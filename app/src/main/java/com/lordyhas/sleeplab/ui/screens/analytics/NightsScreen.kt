package com.lordyhas.sleeplab.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.ui.components.EmptyState
import com.lordyhas.sleeplab.ui.components.GaugeRing
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.NightStrip
import com.lordyhas.sleeplab.ui.components.ScreenHeader
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.util.TimelineSlot
import com.lordyhas.sleeplab.ui.util.formatDuration
import com.lordyhas.sleeplab.ui.util.isShortNight
import com.lordyhas.sleeplab.ui.util.scoreDisplay
import com.lordyhas.sleeplab.ui.util.sessionDurationSeconds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/**
 * Every recorded night, newest first and grouped by month. Each row shows the whole night as a
 * strip (snoring in colour) next to its score, so a bad night stands out without opening it.
 */
@Composable
fun NightsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateToSessionDetail: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val monthFormat = remember { SimpleDateFormat("LLLL yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarTop + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenHeader(title = stringResource(R.string.screen_nights)) }

        if (!uiState.isLoaded) return@LazyColumn

        if (uiState.historySessions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.NightsStay,
                    title = stringResource(R.string.no_nights_title),
                    message = stringResource(R.string.no_night_recorded),
                    tint = SleepIndigoLight
                )
            }
            return@LazyColumn
        }

        // A night belongs to the evening it started, so 00:30 stays with the previous day.
        val byMonth = uiState.historySessions.groupBy {
            monthFormat.format(Date(it.session.startTime - Insights.NIGHT_OFFSET_MS)).replaceFirstChar { c -> c.titlecase() }
        }
        byMonth.forEach { (month, nights) ->
            item(key = "month-$month") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(month, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(
                        pluralStringResource(R.plurals.nights_count, nights.size, nights.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(nights, key = { it.session.id }) { item ->
                val strip by produceState<List<TimelineSlot>?>(null, item.session.id) {
                    value = viewModel.nightStrip(item.session)
                }
                NightRow(
                    item = item,
                    strip = strip,
                    onClick = { onNavigateToSessionDetail(item.session.id) }
                )
            }
        }
    }
}

@Composable
private fun NightRow(item: SessionWithTreatment, strip: List<TimelineSlot>?, onClick: () -> Unit) {
    val session = item.session
    val dayFormat = remember { SimpleDateFormat("EEE d", Locale.getDefault()) }
    val display = session.scoreDisplay()
    val short = session.isShortNight

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(76.dp)) {
                Text(
                    dayFormat.format(Date(session.startTime - Insights.NIGHT_OFFSET_MS)).replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    formatDuration(sessionDurationSeconds(session.startTime, session.endTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (short) stringResource(R.string.short_night_label)
                    else item.treatmentName ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (short) MaterialTheme.colorScheme.outline else SleepIndigoLight,
                    maxLines = 1
                )
            }
            NightStrip(
                slots = strip.orEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
            Spacer(Modifier.width(12.dp))
            GaugeRing(
                progress = if (short) 0f else display.value / 100f,
                color = if (short) MaterialTheme.colorScheme.outline else display.color,
                strokeWidth = 5.dp,
                animationMillis = 600,
                modifier = Modifier.size(54.dp)
            ) {
                Text(
                    if (short) "–" else "${display.value.toInt()}",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
