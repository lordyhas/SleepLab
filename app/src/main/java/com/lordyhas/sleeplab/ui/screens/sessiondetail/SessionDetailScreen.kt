package com.lordyhas.sleeplab.ui.screens.sessiondetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.lordyhas.sleeplab.ui.components.FactorPicker
import com.lordyhas.sleeplab.ui.components.MorningRatingSelector
import com.lordyhas.sleeplab.ui.components.rememberClipPlayer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import com.lordyhas.sleeplab.ui.components.Dot
import com.lordyhas.sleeplab.ui.components.GaugeRing
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.MetricTile
import com.lordyhas.sleeplab.ui.components.Pill
import com.lordyhas.sleeplab.ui.components.SectionHeader
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.theme.SnoreHigh
import com.lordyhas.sleeplab.ui.theme.SnoreLow
import com.lordyhas.sleeplab.ui.theme.SnoreMedium
import com.lordyhas.sleeplab.ui.theme.snoreLevelColor
import com.lordyhas.sleeplab.ui.util.formatDuration
import com.lordyhas.sleeplab.ui.util.scoreDisplay
import com.lordyhas.sleeplab.ui.util.sessionDurationSeconds
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val CHART_MIN_DB = 20f
private const val CHART_MAX_DB = 100f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailScreen(
    sessionId: Int,
    onNavigateBack: () -> Unit
) {
    val factory = remember(sessionId) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SessionDetailViewModel(sessionId) as T
            }
        }
    }
    val viewModel: SessionDetailViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    val factors by viewModel.factors.collectAsState()
    val nightFactorIds by viewModel.factorIds.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val clipPlayer = rememberClipPlayer()
    var showNoteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.night_detail), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.session != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete_night))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        val session = uiState.session
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SleepTealAccent)
            }

            uiState.isDeleted -> Unit

            session == null -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.session_not_found))
            }

            else -> {
                val dateFormat = remember { SimpleDateFormat("EEEE d MMMM", Locale.getDefault()) }
                val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val display = session.scoreDisplay()
                val score = display.value

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding() + 4.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        dateFormat.format(Date(session.startTime)).replaceFirstChar { it.titlecase() },
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${timeFormat.format(Date(session.startTime))} → ${timeFormat.format(Date(session.endTime))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        formatDuration(sessionDurationSeconds(session.startTime, session.endTime)),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Pill(
                                        text = uiState.treatmentName ?: stringResource(R.string.no_treatment),
                                        color = if (uiState.treatmentName != null) SleepIndigoLight else MaterialTheme.colorScheme.onSurfaceVariant,
                                        icon = Icons.Default.MedicalServices
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        session.noiseFloorDb?.let {
                                            stringResource(R.string.detection_info_auto, it.toInt(), session.thresholdDb.toInt())
                                        } ?: stringResource(R.string.detection_info_manual, session.thresholdDb.toInt()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (display.isLegacy) {
                                        Text(
                                            stringResource(R.string.legacy_score_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                GaugeRing(
                                    progress = score / 100f,
                                    color = display.color,
                                    strokeWidth = 8.dp,
                                    animationMillis = 900,
                                    modifier = Modifier.size(96.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${score.toInt()}", style = MaterialTheme.typography.headlineSmall)
                                        Text(
                                            stringResource(display.labelRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.morning_rating_label), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            MorningRatingSelector(
                                rating = session.morningRating,
                                onRatingChange = viewModel::updateMorningRating
                            )
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            Spacer(Modifier.height(4.dp))
                            NoteRow(note = session.note, onEdit = { showNoteDialog = true })
                            HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.night_factors), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(8.dp))
                            FactorPicker(
                                factors = factors,
                                selectedIds = nightFactorIds,
                                onToggle = viewModel::toggleFactor,
                                onAddCustom = viewModel::addCustomFactor
                            )
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.avg_db),
                                    value = "${uiState.averageDb.toInt()}",
                                    icon = Icons.AutoMirrored.Filled.ShowChart,
                                    tint = SleepTealAccent
                                )
                                MetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.max_db),
                                    value = "${uiState.maxDb.toInt()}",
                                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                                    tint = SnoreHigh
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                MetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.min_db),
                                    value = "${uiState.minDb.toInt()}",
                                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                                    tint = SnoreLow
                                )
                                MetricTile(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.snore_episodes),
                                    value = "${uiState.snoreEventCount}",
                                    caption = formatDuration(session.totalSnoreDurationSeconds.toLong()),
                                    icon = Icons.Default.GraphicEq,
                                    tint = SnoreMedium
                                )
                            }
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.noise_timeline), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                LegendItem(SleepTealAccent, stringResource(R.string.db_spl))
                                LegendItem(SnoreHigh.copy(alpha = 0.6f), stringResource(R.string.snoring_title))
                            }
                            Spacer(Modifier.height(14.dp))
                            if (uiState.noiseSamples.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        stringResource(R.string.no_noise_data),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                NightTimelineChart(
                                    noiseSamples = uiState.noiseSamples,
                                    snoreEvents = uiState.snoreEvents,
                                    sessionStartTime = session.startTime,
                                    sessionEndTime = session.endTime,
                                    thresholdDb = session.thresholdDb,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                    }

                    if (uiState.snoreEvents.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            SectionHeader(stringResource(R.string.snore_events_list))
                        }
                        item {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                uiState.snoreEvents.forEachIndexed { index, event ->
                                    if (index > 0) {
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                    }
                                    SnoreEventRow(
                                        event = event,
                                        time = timeFormat.format(Date(event.timestamp)),
                                        isPlaying = event.clipPath != null && clipPlayer.playingPath == event.clipPath,
                                        onPlay = { event.clipPath?.let(clipPlayer::toggle) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_night_title)) },
            text = { Text(stringResource(R.string.delete_night_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteNight()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showNoteDialog) {
        NoteDialog(
            initial = uiState.session?.note.orEmpty(),
            onDismiss = { showNoteDialog = false },
            onSave = { note ->
                viewModel.updateNote(note)
                showNoteDialog = false
            }
        )
    }
}

@Composable
private fun NoteRow(note: String?, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onEdit)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            tint = SleepIndigoLight,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.night_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                note ?: stringResource(R.string.add_note),
                style = MaterialTheme.typography.bodyMedium,
                color = if (note != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
            )
        }
        Icon(
            Icons.Default.Edit,
            contentDescription = stringResource(if (note != null) R.string.edit_note else R.string.add_note),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun NoteDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.isEmpty()) R.string.add_note else R.string.edit_note)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.note_hint)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(onClick = { onSave(text) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
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
private fun SnoreEventRow(event: SnoreEventEntity, time: String, isPlaying: Boolean, onPlay: () -> Unit) {
    val color = snoreLevelColor(event.amplitude)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (event.clipPath != null) 4.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (event.clipPath != null) {
            FilledTonalIconButton(
                onClick = onPlay,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = color.copy(alpha = 0.16f),
                    contentColor = color
                )
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.stop_clip else R.string.play_clip)
                )
            }
            Spacer(Modifier.width(8.dp))
        } else {
            Dot(color = color, size = 10.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(time, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(
            formatDuration(event.durationSeconds.toLong()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Text(
            "${event.amplitude.toInt()} dB",
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.width(56.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun NightTimelineChart(
    noiseSamples: List<NoiseSampleEntity>,
    snoreEvents: List<SnoreEventEntity>,
    sessionStartTime: Long,
    sessionEndTime: Long,
    thresholdDb: Float,
    modifier: Modifier = Modifier
) {
    if (noiseSamples.isEmpty() || sessionEndTime <= sessionStartTime) return

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    // Only samples where the threshold changed, as (timestamp, threshold)
    val thresholdSteps = remember(noiseSamples) {
        noiseSamples.mapNotNull { s -> s.thresholdDb?.let { s.timestamp to it } }
            .fold(mutableListOf<Pair<Long, Float>>()) { acc, step ->
                if (acc.lastOrNull()?.second != step.second) acc += step
                acc
            }
    }
    val finalThreshold = thresholdSteps.lastOrNull()?.second ?: thresholdDb
    val thresholdLabel = stringResource(R.string.threshold_line, finalThreshold.toInt())
    val gridColor = Color.White.copy(alpha = 0.07f)

    Canvas(modifier = modifier) {
        val axisWidth = 26.dp.toPx()
        val bottomLabelHeight = 18.dp.toPx()
        val left = axisWidth
        val width = size.width - axisWidth
        val height = size.height - bottomLabelHeight
        val durationMs = (sessionEndTime - sessionStartTime).toFloat()

        fun xFor(timestamp: Long) = left + ((timestamp - sessionStartTime) / durationMs).coerceIn(0f, 1f) * width
        fun yFor(db: Float) = height - ((db - CHART_MIN_DB) / (CHART_MAX_DB - CHART_MIN_DB)).coerceIn(0f, 1f) * height

        // Y gridlines
        listOf(40f, 60f, 80f).forEach { db ->
            val y = yFor(db)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure("${db.toInt()}", labelStyle)
            drawText(label, topLeft = Offset(0f, y - label.size.height / 2f))
        }

        // Snore events as translucent bands
        snoreEvents.forEach { event ->
            val x = xFor(event.timestamp)
            val bandWidth = ((event.durationSeconds * 1000f) / durationMs * width).coerceAtLeast(2.dp.toPx())
            drawRect(
                color = SnoreHigh.copy(alpha = 0.22f),
                topLeft = Offset(x, 0f),
                size = Size(bandWidth, height)
            )
        }

        // Threshold: stepped when the adaptive floor moved it during the night, flat otherwise
        val thresholdPath = Path()
        if (thresholdSteps.isEmpty()) {
            thresholdPath.moveTo(left, yFor(thresholdDb))
            thresholdPath.lineTo(size.width, yFor(thresholdDb))
        } else {
            var y = yFor(thresholdSteps.first().second)
            thresholdPath.moveTo(left, y)
            thresholdSteps.forEach { (timestamp, value) ->
                val x = xFor(timestamp)
                thresholdPath.lineTo(x, y)
                y = yFor(value)
                thresholdPath.lineTo(x, y)
            }
            thresholdPath.lineTo(size.width, y)
        }
        drawPath(
            path = thresholdPath,
            color = Color.White.copy(alpha = 0.35f),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            )
        )
        val thresholdY = yFor(finalThreshold)
        val thresholdText = textMeasurer.measure(thresholdLabel, labelStyle)
        drawText(
            thresholdText,
            topLeft = Offset(size.width - thresholdText.size.width, thresholdY - thresholdText.size.height - 2.dp.toPx())
        )

        // Noise line + gradient fill
        val path = Path()
        val firstX = xFor(noiseSamples.first().timestamp)
        var lastX = firstX
        noiseSamples.forEachIndexed { index, sample ->
            val x = xFor(sample.timestamp)
            val y = yFor(sample.decibelLevel)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            lastX = x
        }
        val fillPath = Path().apply {
            addPath(path)
            lineTo(lastX, height)
            lineTo(firstX, height)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(SleepTealAccent.copy(alpha = 0.35f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
        drawPath(
            path = path,
            color = SleepTealAccent,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // X labels: start, middle, end
        listOf(
            sessionStartTime,
            sessionStartTime + (sessionEndTime - sessionStartTime) / 2,
            sessionEndTime
        ).forEachIndexed { index, ts ->
            val label = textMeasurer.measure(timeFormat.format(Date(ts)), labelStyle)
            val x = when (index) {
                0 -> left
                2 -> size.width - label.size.width
                else -> xFor(ts) - label.size.width / 2f
            }
            drawText(label, topLeft = Offset(x, height + 4.dp.toPx()))
        }
    }
}
