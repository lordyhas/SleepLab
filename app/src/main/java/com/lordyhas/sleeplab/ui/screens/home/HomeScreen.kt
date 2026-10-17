package com.lordyhas.sleeplab.ui.screens.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import com.lordyhas.sleeplab.ui.components.GaugeRing
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.MetricTile
import com.lordyhas.sleeplab.ui.components.Pill
import com.lordyhas.sleeplab.ui.components.ScreenHeader
import com.lordyhas.sleeplab.ui.components.SectionHeader
import com.lordyhas.sleeplab.ui.components.rememberMicPermissionGate
import com.lordyhas.sleeplab.ui.theme.MoonGlow
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.theme.SnoreLow
import com.lordyhas.sleeplab.ui.theme.SnoreMedium
import com.lordyhas.sleeplab.ui.theme.snoreLevelColor
import com.lordyhas.sleeplab.ui.util.formatClock
import com.lordyhas.sleeplab.ui.util.formatDuration
import com.lordyhas.sleeplab.ui.util.sessionDurationSeconds
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToTracking: () -> Unit = {},
    onNavigateToTreatments: () -> Unit = {},
    onNavigateToSessionDetail: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val trackingState by viewModel.trackingState.collectAsState()

    val startTracking = rememberMicPermissionGate {
        viewModel.startTracking(context)
        onNavigateToTracking()
    }

    val todayFormat = remember { SimpleDateFormat("EEEE d MMMM", Locale.getDefault()) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarTop + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(greetingRes()),
                overline = todayFormat.format(Date())
            )
        }

        item {
            HeroCard(
                isTracking = trackingState.isTracking,
                elapsedSeconds = trackingState.elapsedSeconds,
                snoreCount = trackingState.snoreCount,
                onStartClick = startTracking,
                onViewLiveClick = onNavigateToTracking
            )
        }

        if (!trackingState.isTracking) {
            item {
                TreatmentSelectorSection(
                    activeTreatments = uiState.activeTreatments,
                    selectedId = uiState.selectedTreatmentId,
                    onSelect = viewModel::selectTreatment,
                    onManageClick = onNavigateToTreatments
                )
            }
        }

        item {
            LastNightCard(
                latestSession = uiState.latestSession,
                treatment = uiState.latestSessionTreatment,
                onClick = { uiState.latestSession?.id?.let(onNavigateToSessionDetail) }
            )
        }

        if (uiState.totalSessionsCount > 0) {
            item {
                SectionHeader(stringResource(R.string.overview))
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.nights_tracked),
                        value = "${uiState.totalSessionsCount}",
                        icon = Icons.Default.NightsStay,
                        tint = SleepIndigoLight
                    )
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.avg_per_night),
                        value = stringResource(R.string.minutes_value, uiState.averageSnoreDurationMinutes),
                        icon = Icons.Default.GraphicEq,
                        tint = SnoreMedium
                    )
                    MetricTile(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.best_night),
                        value = stringResource(R.string.minutes_value, uiState.bestNightSnoreMinutes ?: 0),
                        icon = Icons.Default.EmojiEvents,
                        tint = SnoreLow
                    )
                }
            }
        }
    }
}

private fun greetingRes(): Int = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> R.string.greeting_morning
    in 12..17 -> R.string.greeting_afternoon
    in 18..21 -> R.string.greeting_evening
    else -> R.string.greeting_night
}

@Composable
private fun HeroCard(
    isTracking: Boolean,
    elapsedSeconds: Long,
    snoreCount: Int,
    onStartClick: () -> Unit,
    onViewLiveClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.extraLarge
    val gradient = if (isTracking) {
        Brush.linearGradient(listOf(Color(0xFF0F3D3A), Color(0xFF0B1F2E)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF151541)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            .drawBehind {
                // Moon glow in the corner
                val center = Offset(size.width * 0.88f, size.height * 0.12f)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf((if (isTracking) SleepTealAccent else MoonGlow).copy(alpha = 0.25f), Color.Transparent),
                        center = center,
                        radius = size.minDimension * 0.6f
                    ),
                    radius = size.minDimension * 0.6f,
                    center = center
                )
            }
            .padding(24.dp)
    ) {
        AnimatedContent(
            targetState = isTracking,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
            label = "heroContent"
        ) { tracking ->
            if (tracking) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LiveIndicator(text = stringResource(R.string.active_sleep_tracking))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        formatClock(elapsedSeconds),
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    Text(
                        "$snoreCount ${stringResource(R.string.events)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onViewLiveClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF0F3D3A)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.view_live_stop))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MoonGlow,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.ready_for_good_night),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.record_evaluate_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = onStartClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleepTealAccent,
                            contentColor = Color(0xFF042F2E)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.start_sleep), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveIndicator(text: String) {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveAlpha"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(SleepTealAccent.copy(alpha = alpha))
        )
        Spacer(Modifier.width(8.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = SleepTealAccent)
    }
}

@Composable
private fun TreatmentSelectorSection(
    activeTreatments: List<TreatmentEntity>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    onManageClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.tonight_treatment),
            actionLabel = stringResource(R.string.manage),
            onAction = onManageClick
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                TreatmentChip(
                    label = stringResource(R.string.no_treatment),
                    selected = selectedId == null,
                    onClick = { onSelect(null) }
                )
            }
            items(activeTreatments, key = { it.id }) { treatment ->
                TreatmentChip(
                    label = treatment.name,
                    selected = selectedId == treatment.id,
                    onClick = { onSelect(treatment.id) }
                )
            }
        }
    }
}

@Composable
private fun TreatmentChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.04f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.1f),
            selectedBorderColor = Color.Transparent
        )
    )
}

@Composable
private fun LastNightCard(
    latestSession: SleepSessionEntity?,
    treatment: TreatmentEntity?,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEE d MMM", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (latestSession != null) onClick else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.last_night),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            if (latestSession != null) {
                Text(
                    dateFormat.format(Date(latestSession.startTime)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.see_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (latestSession == null) {
            Text(
                stringResource(R.string.no_night_recorded),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@GlassCard
        }

        val score = latestSession.snoreIntensityScore
        val levelColor = snoreLevelColor(score)
        val levelLabel = stringResource(
            when {
                score < 45f -> R.string.night_quiet
                score < 60f -> R.string.night_moderate
                else -> R.string.night_loud
            }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            GaugeRing(
                progress = score / 100f,
                color = levelColor,
                strokeWidth = 9.dp,
                animationMillis = 900,
                modifier = Modifier.size(112.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${score.toInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        stringResource(R.string.intensity_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(text = levelLabel, color = levelColor)
                LabeledValue(
                    icon = Icons.Default.Schedule,
                    tint = SleepIndigoLight,
                    label = stringResource(R.string.time_in_bed),
                    value = formatDuration(sessionDurationSeconds(latestSession.startTime, latestSession.endTime))
                )
                LabeledValue(
                    icon = Icons.Default.GraphicEq,
                    tint = SnoreMedium,
                    label = stringResource(R.string.snoring_title),
                    value = formatDuration(latestSession.totalSnoreDurationSeconds.toLong())
                )
            }
        }

        if (treatment != null) {
            Spacer(Modifier.height(16.dp))
            Pill(
                text = treatment.name,
                color = SleepIndigoLight,
                icon = Icons.Default.MedicalServices
            )
        }
    }
}

@Composable
private fun LabeledValue(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
