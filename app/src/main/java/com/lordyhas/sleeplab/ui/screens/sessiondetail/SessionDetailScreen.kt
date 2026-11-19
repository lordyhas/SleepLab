package com.lordyhas.sleeplab.ui.screens.sessiondetail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.res.pluralStringResource
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.ui.components.IconBadge
import com.lordyhas.sleeplab.ui.components.icon
import com.lordyhas.sleeplab.ui.components.label
import com.lordyhas.sleeplab.ui.util.SnoreEpisode
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.lordyhas.sleeplab.ui.theme.NightSkyTop
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

/** dB levels are relative and a quiet room reads ~5–10, so the axis follows the night's own range. */
internal fun chartDbRange(values: List<Float>): Pair<Float, Float> {
    if (values.isEmpty()) return 0f to 40f
    val low = (kotlin.math.floor((values.min() - 2f) / 10f) * 10f).coerceAtLeast(0f)
    var high = kotlin.math.ceil((values.max() + 2f) / 10f) * 10f
    if (high - low < 20f) high = low + 20f
    return low to high
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val showClipsNudge by viewModel.showClipsNudge.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val clipPlayer = rememberClipPlayer()
    var showNoteDialog by remember { mutableStateOf(false) }
    var showClipsConsent by remember { mutableStateOf(false) }
    var showAllSnores by rememberSaveable { mutableStateOf(false) }
    var editingFactors by rememberSaveable { mutableStateOf(false) }
    var showTechnical by rememberSaveable { mutableStateOf(false) }
    // The bar turns solid once content scrolls under it, so rows never mix with the title.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = NightSkyTop
                ),
                scrollBehavior = scrollBehavior
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
                val episodes = uiState.episodes

                val morningCard: LazyListScope.() -> Unit = {
                    item(key = "morning") {
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
                        }
                    }
                }

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
                    item(key = "summary") {
                        NightSummaryCard(
                            date = dateFormat.format(Date(session.startTime)).replaceFirstChar { it.titlecase() },
                            times = "${timeFormat.format(Date(session.startTime))} → ${timeFormat.format(Date(session.endTime))}",
                            session = session,
                            treatmentName = uiState.treatmentName
                        )
                    }

                    // Not rated yet: ask right away, it is what the morning visit is for.
                    if (session.morningRating == null) morningCard()

                    if (showClipsNudge && episodes.isNotEmpty()) {
                        item(key = "clipsNudge") {
                            ClipsNudgeCard(
                                onEnable = { showClipsConsent = true },
                                onDismiss = viewModel::dismissClipsNudge
                            )
                        }
                    }

                    item(key = "timeline") {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.noise_timeline), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                LegendItem(SleepTealAccent, stringResource(R.string.sound_level))
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
                                    episodes = episodes,
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

                    if (episodes.isNotEmpty()) {
                        item(key = "episodesHeader") {
                            Spacer(Modifier.height(4.dp))
                            SectionHeader(stringResource(R.string.episodes_title, episodes.size))
                            Text(
                                stringResource(R.string.episodes_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item(key = "episodes") {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                episodes.forEachIndexed { index, episode ->
                                    if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                    val clip = episode.clipPath
                                    EpisodeRow(
                                        episode = episode,
                                        timeRange = timeRange(timeFormat.format(Date(episode.startTime)), timeFormat.format(Date(episode.endTime))),
                                        isPlaying = clip != null && clipPlayer.playingPath == clip,
                                        onPlay = { clip?.let(clipPlayer::toggle) }
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                TextButton(
                                    onClick = { showAllSnores = !showAllSnores },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (showAllSnores) stringResource(R.string.hide_all_snores)
                                        else stringResource(R.string.show_all_snores, uiState.snoreEventCount)
                                    )
                                }
                                if (showAllSnores) {
                                    uiState.snoreEvents.forEach { event ->
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.04f))
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

                    if (session.morningRating != null) morningCard()

                    item(key = "factors") {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(R.string.night_factors),
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { editingFactors = !editingFactors }) {
                                    Text(stringResource(if (editingFactors) R.string.done else R.string.edit_factors))
                                }
                            }
                            if (editingFactors) {
                                FactorPicker(
                                    factors = factors,
                                    selectedIds = nightFactorIds,
                                    onToggle = viewModel::toggleFactor,
                                    onAddCustom = viewModel::addCustomFactor
                                )
                            } else {
                                val selected = factors.filter { it.id in nightFactorIds }
                                if (selected.isEmpty()) {
                                    Text(
                                        stringResource(R.string.no_factors_tagged),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        selected.forEach { factor ->
                                            Pill(text = factor.label(), color = SleepIndigoLight, icon = factor.icon())
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "technical") {
                        TechnicalDetailsCard(
                            expanded = showTechnical,
                            onToggle = { showTechnical = !showTechnical },
                            session = session,
                            averageDb = uiState.averageDb,
                            maxDb = uiState.maxDb,
                            minDb = uiState.minDb,
                            snoreCount = uiState.snoreEventCount
                        )
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

    if (showClipsConsent) {
        // Same consent as in settings: recordings stay opt-in.
        AlertDialog(
            onDismissRequest = { showClipsConsent = false },
            icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
            title = { Text(stringResource(R.string.clips_consent_title)) },
            text = { Text(stringResource(R.string.clips_consent_message)) },
            confirmButton = {
                Button(onClick = {
                    showClipsConsent = false
                    viewModel.enableClips()
                }) { Text(stringResource(R.string.clips_consent_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClipsConsent = false }) { Text(stringResource(R.string.cancel)) }
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

/** Score, verdict and how much of the night was spent snoring: the answer to "how was my night?". */
@Composable
private fun NightSummaryCard(
    date: String,
    times: String,
    session: SleepSessionEntity,
    treatmentName: String?
) {
    val display = session.scoreDisplay()
    val score = display.value
    val nightSeconds = sessionDurationSeconds(session.startTime, session.endTime)
    val snoreSeconds = session.totalSnoreDurationSeconds.toLong()

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(date, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(times, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatDuration(nightSeconds), style = MaterialTheme.typography.headlineSmall)
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
        Spacer(Modifier.height(14.dp))
        Text(
            if (snoreSeconds == 0L) stringResource(R.string.no_snoring_detected)
            else stringResource(R.string.snoring_of_night, formatDuration(snoreSeconds), nightShare(snoreSeconds, nightSeconds)),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(text = stringResource(display.levelRes), color = display.color)
            Pill(
                text = treatmentName ?: stringResource(R.string.no_treatment),
                color = if (treatmentName != null) SleepIndigoLight else MaterialTheme.colorScheme.onSurfaceVariant,
                icon = Icons.Default.MedicalServices
            )
        }
    }
}

/** "01:04 – 01:29", or just "01:03" when the episode starts and ends in the same minute. */
private fun timeRange(start: String, end: String) = if (start == end) start else "$start – $end"

/** "12%", or "<1%" so a few minutes of snoring never reads as zero. */
internal fun nightShare(snoreSeconds: Long, nightSeconds: Long): String {
    if (nightSeconds <= 0) return "0%"
    val percent = snoreSeconds * 100.0 / nightSeconds
    return if (snoreSeconds > 0 && percent < 1.0) "<1%" else "${percent.toInt()}%"
}

@Composable
private fun ClipsNudgeCard(onEnable: () -> Unit, onDismiss: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.GraphicEq, tint = SnoreMedium)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.clips_nudge_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.clips_nudge_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) }
            Spacer(Modifier.width(4.dp))
            Button(onClick = onEnable) { Text(stringResource(R.string.clips_nudge_enable)) }
        }
    }
}

/** dB values and detection settings: useful to check the detector, not to read the night. */
@Composable
private fun TechnicalDetailsCard(
    expanded: Boolean,
    onToggle: () -> Unit,
    session: SleepSessionEntity,
    averageDb: Float,
    maxDb: Float,
    minDb: Float,
    snoreCount: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.technical_details),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    session.noiseFloorDb?.let {
                        stringResource(R.string.detection_info_auto, it.toInt(), session.thresholdDb.toInt())
                    } ?: stringResource(R.string.detection_info_manual, session.thresholdDb.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.avg_db),
                        value = "${averageDb.toInt()}",
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        tint = SleepTealAccent
                    )
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.max_db),
                        value = "${maxDb.toInt()}",
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        tint = SnoreHigh
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.min_db),
                        value = "${minDb.toInt()}",
                        icon = Icons.AutoMirrored.Filled.VolumeDown,
                        tint = SnoreLow
                    )
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.snores_detected),
                        value = "$snoreCount",
                        icon = Icons.Default.GraphicEq,
                        tint = SnoreMedium
                    )
                }
                Text(
                    stringResource(R.string.technical_details_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (session.scoreDisplay().isLegacy) {
                    Text(
                        stringResource(R.string.legacy_score_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: SnoreEpisode, timeRange: String, isPlaying: Boolean, onPlay: () -> Unit) {
    val color = snoreLevelColor(episode.peakDb)
    val hasClip = episode.clipPath != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (hasClip) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasClip) {
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
            Spacer(Modifier.width(10.dp))
        } else {
            Dot(color = color, size = 10.dp)
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(timeRange, style = MaterialTheme.typography.titleSmall)
            Text(
                "${formatDuration(episode.durationMs / 1000)} · " +
                    pluralStringResource(R.plurals.snores_count, episode.snoreCount, episode.snoreCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            stringResource(R.string.peak_db, episode.peakDb.toInt()),
            style = MaterialTheme.typography.labelLarge,
            color = color
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
    episodes: List<SnoreEpisode>,
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
    val (chartMinDb, chartMaxDb) = remember(noiseSamples, snoreEvents, thresholdDb) {
        chartDbRange(
            noiseSamples.map { it.decibelLevel } + snoreEvents.map { it.amplitude } +
                thresholdSteps.map { it.second } + thresholdDb
        )
    }
    val gridStep = if (chartMaxDb - chartMinDb > 40f) 20f else 10f

    Canvas(modifier = modifier) {
        val axisWidth = 26.dp.toPx()
        val bottomLabelHeight = 18.dp.toPx()
        val left = axisWidth
        val width = size.width - axisWidth
        val height = size.height - bottomLabelHeight
        val durationMs = (sessionEndTime - sessionStartTime).toFloat()

        fun xFor(timestamp: Long) = left + ((timestamp - sessionStartTime) / durationMs).coerceIn(0f, 1f) * width
        fun yFor(db: Float) = height - ((db - chartMinDb) / (chartMaxDb - chartMinDb)).coerceIn(0f, 1f) * height

        // Y gridlines (the bottom one is the axis itself)
        generateSequence(chartMinDb + gridStep) { it + gridStep }.takeWhile { it <= chartMaxDb }.forEach { db ->
            val y = yFor(db)
            drawLine(gridColor, Offset(left, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val label = textMeasurer.measure("${db.toInt()}", labelStyle)
            drawText(label, topLeft = Offset(0f, (y - label.size.height / 2f).coerceAtLeast(0f)))
        }

        // Snoring episodes as translucent bands
        episodes.forEach { episode ->
            val x = xFor(episode.startTime)
            val bandWidth = (episode.durationMs / durationMs * width).coerceAtLeast(2.dp.toPx())
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
