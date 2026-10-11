package com.lordyhas.sonrelab.ui.screens.sessiondetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sonrelab.R
import com.lordyhas.sonrelab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sonrelab.data.local.entity.SnoreEventEntity
import com.lordyhas.sonrelab.ui.theme.MidnightSurfaceCard
import com.lordyhas.sonrelab.ui.theme.SleepIndigoPrimary
import com.lordyhas.sonrelab.ui.theme.SleepTealAccent
import com.lordyhas.sonrelab.ui.theme.SnoreHigh
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Int,
    onNavigateBack: () -> Unit
) {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SessionDetailViewModel(sessionId) as T
        }
    }
    val viewModel: SessionDetailViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.night_detail), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SleepTealAccent)
            }
        } else if (uiState.session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.session_not_found))
            }
        } else {
            val session = uiState.session!!
            val dateFormat = remember { SimpleDateFormat("EEEE d MMMM", Locale.getDefault()) }
            val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header (Date, Time, Treatment)
                item {
                    val totalSleepSeconds = ((session.endTime - session.startTime) / 1000).coerceAtLeast(0)
                    val sleepHours = totalSleepSeconds / 3600
                    val sleepMinutes = (totalSleepSeconds % 3600) / 60

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = dateFormat.format(Date(session.startTime)).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp), tint = SleepTealAccent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${timeFormat.format(Date(session.startTime))} → ${timeFormat.format(Date(session.endTime))} (${sleepHours}h ${sleepMinutes}m)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (uiState.treatmentName != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = SleepIndigoPrimary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.treatmentName!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SleepTealAccent
                                    )
                                }
                            }
                        }
                    }
                }

                // Timeline Chart
                item {
                    Text(
                        stringResource(R.string.noise_timeline),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (uiState.noiseSamples.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_noise_data), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .padding(16.dp)
                            ) {
                                NightTimelineChart(
                                    noiseSamples = uiState.noiseSamples,
                                    snoreEvents = uiState.snoreEvents,
                                    sessionStartTime = session.startTime,
                                    sessionEndTime = session.endTime,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Stats row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatBox(modifier = Modifier.weight(1f), title = stringResource(R.string.avg_db), value = "${uiState.averageDb.toInt()}")
                        StatBox(modifier = Modifier.weight(1f), title = stringResource(R.string.max_db), value = "${uiState.maxDb.toInt()}")
                        StatBox(modifier = Modifier.weight(1f), title = stringResource(R.string.min_db), value = "${uiState.minDb.toInt()}")
                        StatBox(modifier = Modifier.weight(1f), title = stringResource(R.string.snore_episodes), value = "${uiState.snoreEventCount}")
                    }
                }

                // Snore Events List
                if (uiState.snoreEvents.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.snore_events_list),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(uiState.snoreEvents, key = { it.id }) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeFormat.format(Date(event.timestamp)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${event.amplitude.toInt()} dB",
                                    color = SnoreHigh,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${event.durationSeconds}s",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun NightTimelineChart(
    noiseSamples: List<NoiseSampleEntity>,
    snoreEvents: List<SnoreEventEntity>,
    sessionStartTime: Long,
    sessionEndTime: Long,
    modifier: Modifier = Modifier
) {
    if (noiseSamples.isEmpty() || sessionEndTime <= sessionStartTime) return
    
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val maxDb = 100f
        val durationMs = sessionEndTime - sessionStartTime
        
        // 1. Threshold line (50 dB)
        val thresholdY = height - (50f / maxDb) * height
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, thresholdY),
            end = Offset(width, thresholdY),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
        
        // 2. Snore events (vertical red bars)
        snoreEvents.forEach { event ->
            val eventRelativeTime = event.timestamp - sessionStartTime
            val x = (eventRelativeTime.toFloat() / durationMs.toFloat()) * width
            // width based on duration
            val eventWidth = ((event.durationSeconds * 1000f) / durationMs.toFloat()) * width
            val finalWidth = eventWidth.coerceAtLeast(2.dp.toPx()) // at least 2dp visible
            
            drawRect(
                color = SnoreHigh.copy(alpha = 0.4f),
                topLeft = Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(finalWidth, height)
            )
        }
        
        // 3. Noise samples path
        val path = Path()
        var isFirst = true
        var lastX = 0f
        
        noiseSamples.forEach { sample ->
            val relativeTime = sample.timestamp - sessionStartTime
            val x = (relativeTime.toFloat() / durationMs.toFloat()) * width
            val y = height - (sample.decibelLevel / maxDb) * height
            
            if (isFirst) {
                path.moveTo(x, y)
                isFirst = false
            } else {
                path.lineTo(x, y)
            }
            lastX = x
        }
        
        // Fill gradient
        val fillPath = Path().apply {
            addPath(path)
            lineTo(lastX, height)
            lineTo(0f, height) // assume started at 0 x
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(SleepTealAccent.copy(alpha = 0.5f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
        
        // Draw the stroke
        drawPath(
            path = path,
            color = SleepTealAccent,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun StatBox(modifier: Modifier = Modifier, title: String, value: String) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MidnightSurfaceCard)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SleepTealAccent)
        }
    }
}
