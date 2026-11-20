package com.lordyhas.sleeplab.ui.screens.sessiondetail

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bed
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.lordyhas.sleeplab.ui.components.color
import com.lordyhas.sleeplab.ui.components.formatBandDuration
import com.lordyhas.sleeplab.ui.util.NightTimeline
import com.lordyhas.sleeplab.ui.util.NightTimelines
import kotlinx.coroutines.launch
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
import com.lordyhas.sleeplab.ui.screens.analytics.Insights
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionDetailScreen(
    sessionId: Int,
    onNavigateBack: () -> Unit,
    /** Open another night in place of this one (previous / next arrows). */
    onOpenNight: (Int) -> Unit = {}
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
    var selectedTime by rememberSaveable { mutableStateOf<Long?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // The bar turns solid once content scrolls under it, so rows never mix with the title.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    // Named after the evening the night started, like the Nights list
    val titleFormat = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.session?.let { titleFormat.format(Date(it.startTime - Insights.NIGHT_OFFSET_MS)).replaceFirstChar { c -> c.titlecase() } }
                            ?: stringResource(R.string.night_detail),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState.session != null) {
                        IconButton(
                            onClick = { uiState.previousSessionId?.let(onOpenNight) },
                            enabled = uiState.previousSessionId != null
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_night))
                        }
                        IconButton(
                            onClick = { uiState.nextSessionId?.let(onOpenNight) },
                            enabled = uiState.nextSessionId != null
                        ) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_night))
                        }
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
                val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
                val episodes = uiState.episodes
                val timeline = uiState.timeline
                val threshold = session.thresholdDb

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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding() + 4.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // The night first: the picture answers "how was it?" before any number.
                    item(key = "chart") {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    timeFormat.format(Date(session.startTime)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    timeFormat.format(Date(session.endTime)),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            if (timeline == null || timeline.slots.isEmpty()) {
                                Box(ChartPlaceholderModifier, contentAlignment = Alignment.Center) {
                                    Text(
                                        stringResource(R.string.no_noise_data),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                NightIntensityChart(
                                    timeline = timeline,
                                    selectedTime = selectedTime,
                                    onSelect = { selectedTime = it },
                                    modifier = ChartPlaceholderModifier
                                )
                                Spacer(Modifier.height(14.dp))
                                BandLegend(timeline)
                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                                MomentPanel(
                                    selectedTime = selectedTime,
                                    timeline = timeline,
                                    episodes = episodes,
                                    threshold = threshold,
                                    timeFormat = timeFormat,
                                    playingPath = clipPlayer.playingPath,
                                    onPlay = clipPlayer::toggle
                                )
                            }
                        }
                    }

                    item(key = "summary") {
                        NightSummaryCard(
                            session = session,
                            snoringSeconds = timeline?.snoringSeconds ?: session.totalSnoreDurationSeconds.toLong(),
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
                                        color = NightTimelines.bandFor(episode.peakDb, threshold).color(),
                                        timeRange = timeRange(timeFormat.format(Date(episode.startTime)), timeFormat.format(Date(episode.endTime))),
                                        isPlaying = clip != null && clipPlayer.playingPath == clip,
                                        onPlay = { clip?.let(clipPlayer::toggle) },
                                        // Show it on the chart: select its loudest moment and scroll up
                                        onClick = {
                                            selectedTime = episode.events.maxBy { it.amplitude }.timestamp
                                            scope.launch { listState.animateScrollToItem(0) }
                                        }
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
                                            color = NightTimelines.bandFor(event.amplitude, threshold).color(),
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

/**
 * What happened at the moment picked on the chart: the snoring episode there (with ▶ when a clip
 * was kept), or that the room was quiet. Before any pick, a hint that the chart can be explored.
 */
@Composable
private fun MomentPanel(
    selectedTime: Long?,
    timeline: NightTimeline,
    episodes: List<SnoreEpisode>,
    threshold: Float,
    timeFormat: SimpleDateFormat,
    playingPath: String?,
    onPlay: (String) -> Unit
) {
    if (selectedTime == null) {
        Text(
            stringResource(R.string.chart_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp)
        )
        return
    }
    // The episode under the finger, or one within 2 min so short ones are easy to hit
    val episode = episodes.firstOrNull { selectedTime in it.startTime..it.endTime }
        ?: episodes.minByOrNull { minOf(kotlin.math.abs(it.startTime - selectedTime), kotlin.math.abs(it.endTime - selectedTime)) }
            ?.takeIf { minOf(kotlin.math.abs(it.startTime - selectedTime), kotlin.math.abs(it.endTime - selectedTime)) < 2 * 60_000L }

    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            timeFormat.format(Date(selectedTime)),
            style = MaterialTheme.typography.titleMedium
        )
        if (episode != null) {
            val clip = episode.clipPath
            EpisodeRow(
                episode = episode,
                color = NightTimelines.bandFor(episode.peakDb, threshold).color(),
                timeRange = timeRange(timeFormat.format(Date(episode.startTime)), timeFormat.format(Date(episode.endTime))),
                isPlaying = clip != null && playingPath == clip,
                onPlay = { clip?.let(onPlay) }
            )
            if (clip == null) {
                Text(
                    stringResource(R.string.no_clip_for_episode),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            val slot = timeline.slots.firstOrNull { selectedTime < it.endTime }
            Text(
                stringResource(R.string.quiet_at_time, (slot?.level ?: 0f).toInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

/** Time in bed, time snoring and the score: the numbers behind the chart. */
@Composable
private fun NightSummaryCard(
    session: SleepSessionEntity,
    snoringSeconds: Long,
    treatmentName: String?
) {
    val display = session.scoreDisplay()
    val score = display.value
    val nightSeconds = sessionDurationSeconds(session.startTime, session.endTime)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SummaryLine(
                    icon = Icons.Default.Bed,
                    tint = SleepIndigoLight,
                    label = stringResource(R.string.time_in_bed),
                    value = formatDuration(nightSeconds)
                )
                SummaryLine(
                    icon = Icons.Default.GraphicEq,
                    tint = SnoreMedium,
                    label = stringResource(R.string.time_snoring),
                    value = if (snoringSeconds == 0L) stringResource(R.string.no_snoring_detected)
                    else "${formatBandDuration(snoringSeconds)} · ${nightShare(snoringSeconds, nightSeconds)}"
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GaugeRing(
                    progress = score / 100f,
                    color = display.color,
                    strokeWidth = 10.dp,
                    animationMillis = 900,
                    modifier = Modifier.size(104.dp)
                ) {
                    Text("${score.toInt()}", style = MaterialTheme.typography.displaySmall)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(display.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
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

@Composable
private fun SummaryLine(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, tint = tint, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
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
private fun EpisodeRow(
    episode: SnoreEpisode,
    color: Color,
    timeRange: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val hasClip = episode.clipPath != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
private fun SnoreEventRow(event: SnoreEventEntity, color: Color, time: String, isPlaying: Boolean, onPlay: () -> Unit) {
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
