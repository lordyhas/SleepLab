package com.lordyhas.sleeplab.ui.screens.tracking

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.service.TrackingState
import com.lordyhas.sleeplab.ui.components.GaugeRing
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.HoldToConfirmButton
import com.lordyhas.sleeplab.ui.components.IconBadge
import com.lordyhas.sleeplab.ui.components.MetricTile
import com.lordyhas.sleeplab.ui.components.Pill
import com.lordyhas.sleeplab.ui.components.ScreenHeader
import com.lordyhas.sleeplab.ui.components.rememberMicPermissionGate
import com.lordyhas.sleeplab.ui.theme.MoonGlow
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.theme.SnoreHigh
import com.lordyhas.sleeplab.ui.theme.SnoreMedium
import com.lordyhas.sleeplab.ui.theme.snoreLevelColor
import com.lordyhas.sleeplab.ui.util.formatClock
import com.lordyhas.sleeplab.ui.util.formatDuration
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** dB range mapped onto the live gauge. */
private const val GAUGE_MIN_DB = 20f
private const val GAUGE_MAX_DB = 90f

@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel = viewModel(),
    onNavigateToTreatments: () -> Unit = {}
) {
    val context = LocalContext.current
    val trackingState by viewModel.trackingState.collectAsState()
    val allTreatments by viewModel.allTreatments.collectAsState()
    val plannedTreatment by viewModel.plannedTreatment.collectAsState()
    val thresholdDb by viewModel.thresholdDb.collectAsState()

    val startTracking = rememberMicPermissionGate { viewModel.startTracking(context) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Crossfade(
        targetState = trackingState.isTracking,
        animationSpec = tween(350),
        label = "trackingMode"
    ) { isTracking ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = statusBarTop + 12.dp)
        ) {
            if (isTracking) {
                val treatmentName = trackingState.treatmentId?.let { id ->
                    allTreatments.firstOrNull { it.id == id }?.name
                } ?: stringResource(R.string.no_specific_treatment)

                ActiveTrackingContent(
                    state = trackingState,
                    thresholdDb = thresholdDb,
                    treatmentName = treatmentName,
                    onStop = { viewModel.stopTracking(context) }
                )
            } else {
                InactiveTrackingContent(
                    plannedTreatmentName = plannedTreatment?.name ?: stringResource(R.string.no_treatment),
                    onTreatmentClick = onNavigateToTreatments,
                    onStart = startTracking
                )
            }
        }
    }
}

@Composable
private fun ActiveTrackingContent(
    state: TrackingState,
    thresholdDb: Float,
    treatmentName: String,
    onStop: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dbColor = snoreLevelColor(state.currentDb)
    val aboveThreshold = state.currentDb >= thresholdDb

    var showHoldHint by remember { mutableStateOf(false) }
    LaunchedEffect(showHoldHint) {
        if (showHoldHint) {
            delay(2500)
            showHoldHint = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = stringResource(R.string.sleep_tracking),
                overline = stringResource(R.string.tracking_since, timeFormat.format(Date(state.startTime)))
            )
            Spacer(Modifier.height(4.dp))
            Pill(
                text = treatmentName,
                color = SleepIndigoLight,
                icon = Icons.Default.MedicalServices,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(Modifier.height(20.dp))

            Text(
                formatClock(state.elapsedSeconds),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(16.dp))

            BreathingHalo(color = dbColor) {
                GaugeRing(
                    progress = (state.currentDb - GAUGE_MIN_DB) / (GAUGE_MAX_DB - GAUGE_MIN_DB),
                    color = dbColor,
                    strokeWidth = 12.dp,
                    animationMillis = 250,
                    modifier = Modifier.size(210.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${state.currentDb.toInt()}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.db_spl),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Pill(
                            text = stringResource(if (aboveThreshold) R.string.level_above_threshold else R.string.level_quiet),
                            color = dbColor,
                            icon = if (aboveThreshold) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.Bedtime
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            WaveformVisualizer(
                recentHistory = state.recentDbHistory,
                thresholdDb = thresholdDb,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.snoring_title),
                    value = "${state.snoreCount}",
                    caption = stringResource(R.string.events),
                    icon = Icons.Default.GraphicEq,
                    tint = SleepIndigoLight
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.snore_time),
                    value = formatDuration(state.totalSnoreSeconds.toLong()),
                    caption = stringResource(R.string.cumulative_duration),
                    icon = Icons.Default.Timer,
                    tint = SnoreMedium
                )
                MetricTile(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.peak_label),
                    value = "${state.maxDb.toInt()}",
                    caption = stringResource(R.string.db_max),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    tint = SnoreHigh
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        AnimatedVisibility(visible = showHoldHint, enter = fadeIn(), exit = fadeOut()) {
            Text(
                stringResource(R.string.hold_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
        HoldToConfirmButton(
            label = stringResource(R.string.hold_to_stop),
            icon = Icons.Default.Stop,
            color = MaterialTheme.colorScheme.error,
            onConfirm = onStop,
            onTapTooShort = { showHoldHint = true },
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}

/** Slow "breathing" glow behind the live gauge. */
@Composable
private fun BreathingHalo(color: Color, content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition(label = "breath")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(4000), RepeatMode.Reverse),
        label = "breathScale"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(250.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(color.copy(alpha = 0.16f), Color.Transparent)))
        )
        content()
    }
}

@Composable
private fun WaveformVisualizer(
    recentHistory: List<Float>,
    thresholdDb: Float,
    modifier: Modifier = Modifier
) {
    val emptyBarColor = Color.White.copy(alpha = 0.06f)
    val thresholdColor = Color.White.copy(alpha = 0.25f)

    GlassCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalBars = 40
            val slot = size.width / totalBars
            val barWidth = slot * 0.6f
            val padded = List((totalBars - recentHistory.size).coerceAtLeast(0)) { 0f } +
                    recentHistory.takeLast(totalBars)

            fun yFor(db: Float): Float {
                val fraction = ((db - GAUGE_MIN_DB) / (GAUGE_MAX_DB - GAUGE_MIN_DB)).coerceIn(0.06f, 1f)
                return size.height * (1f - fraction)
            }

            padded.forEachIndexed { index, db ->
                val top = yFor(db)
                drawRoundRect(
                    color = if (db <= 0f) emptyBarColor else snoreLevelColor(db).copy(
                        alpha = 0.35f + 0.65f * (index + 1f) / totalBars // older bars fade out
                    ),
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, top),
                    size = Size(barWidth, size.height - top),
                    cornerRadius = CornerRadius(barWidth / 2f)
                )
            }

            val thresholdY = yFor(thresholdDb)
            drawLine(
                color = thresholdColor,
                start = Offset(0f, thresholdY),
                end = Offset(size.width, thresholdY),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            )
        }
    }
}

@Composable
private fun InactiveTrackingContent(
    plannedTreatmentName: String,
    onTreatmentClick: () -> Unit,
    onStart: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(title = stringResource(R.string.sleep_tracking))

            Spacer(Modifier.height(24.dp))

            BreathingHalo(color = MoonGlow) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(MoonGlow.copy(alpha = 0.22f), MoonGlow.copy(alpha = 0.04f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Bedtime,
                        contentDescription = null,
                        tint = MoonGlow,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                stringResource(R.string.no_recording),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.place_phone_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(24.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tips_title), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                TipRow(Icons.Default.PhoneAndroid, SleepIndigoLight, stringResource(R.string.tip_placement))
                Spacer(Modifier.height(10.dp))
                TipRow(Icons.Default.BatteryChargingFull, SleepTealAccent, stringResource(R.string.tip_charger))
                Spacer(Modifier.height(10.dp))
                TipRow(Icons.AutoMirrored.Filled.VolumeOff, SnoreMedium, stringResource(R.string.tip_quiet))
            }
            Spacer(Modifier.height(16.dp))
        }

        Pill(
            text = stringResource(R.string.tonight_with, plannedTreatmentName),
            color = SleepIndigoLight,
            icon = Icons.Default.MedicalServices,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .clickable(onClick = onTreatmentClick)
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .height(60.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.buttonColors(
                containerColor = SleepTealAccent,
                contentColor = Color(0xFF042F2E)
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.start_night_tracking), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TipRow(icon: ImageVector, tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, tint = tint, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
