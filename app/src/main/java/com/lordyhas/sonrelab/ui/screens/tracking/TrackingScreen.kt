package com.lordyhas.sonrelab.ui.screens.tracking

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sonrelab.service.TrackingState
import com.lordyhas.sonrelab.ui.theme.SleepIndigoPrimary
import com.lordyhas.sonrelab.ui.theme.SleepTealAccent
import com.lordyhas.sonrelab.ui.theme.SnoreHigh
import com.lordyhas.sonrelab.ui.theme.SnoreLow
import com.lordyhas.sonrelab.ui.theme.SnoreMedium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel = viewModel(),
    onNavigateToHome: () -> Unit = {}
) {
    val context = LocalContext.current
    val trackingState by viewModel.trackingState.collectAsState()
    val activeTreatments by viewModel.activeTreatments.collectAsState()

    val currentTreatmentName = remember(trackingState.treatmentId, activeTreatments) {
        if (trackingState.treatmentId != null) {
            activeTreatments.find { it.id == trackingState.treatmentId }?.name ?: "Traitement actif"
        } else {
            "Aucun traitement spécifique"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Suivi du Sommeil",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (trackingState.isTracking) {
                ActiveTrackingContent(
                    state = trackingState,
                    treatmentName = currentTreatmentName,
                    onStop = {
                        viewModel.stopTracking(context)
                    }
                )
            } else {
                InactiveTrackingContent(
                    onStart = {
                        val firstActive = activeTreatments.firstOrNull()?.id
                        viewModel.startTracking(context, firstActive)
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveTrackingContent(
    state: TrackingState,
    treatmentName: String,
    onStop: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val dbColor = when {
        state.currentDb < 45f -> SnoreLow
        state.currentDb < 60f -> SnoreMedium
        else -> SnoreHigh
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Treatment Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🔬 $treatmentName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Timer
        val hours = state.elapsedSeconds / 3600
        val minutes = (state.elapsedSeconds % 3600) / 60
        val seconds = state.elapsedSeconds % 60
        val timerString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        Text(
            text = timerString,
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Enregistrement en cours",
            style = MaterialTheme.typography.bodySmall,
            color = SleepTealAccent
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Animated Pulse Circle with dB gauge
        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dbColor.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
                .border(2.dp, dbColor.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = dbColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.currentDb.toInt()}",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "dB SPL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Realtime Waveform Canvas
        WaveformVisualizer(
            recentHistory = state.recentDbHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Ronflements",
                value = "${state.snoreCount}",
                unit = "événements",
                iconColor = SleepIndigoPrimary
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Temps ronflé",
                value = "${state.totalSnoreSeconds / 60}m ${state.totalSnoreSeconds % 60}s",
                unit = "durée cumulée",
                iconColor = SnoreMedium
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Pic sonore",
                value = "${state.maxDb.toInt()}",
                unit = "dB max",
                iconColor = SnoreHigh
            )
        }
    }

    // Stop Button
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Arrêter et Enregistrer",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun WaveformVisualizer(
    recentHistory: List<Float>,
    modifier: Modifier = Modifier
) {
    val barColor = SleepIndigoPrimary.copy(alpha = 0.8f)
    val highlightColor = SleepTealAccent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val totalBars = 35
            val barWidth = size.width / (totalBars * 1.5f)
            val spacing = barWidth * 0.5f

            val historyPadded = if (recentHistory.size < totalBars) {
                List(totalBars - recentHistory.size) { 0f } + recentHistory
            } else {
                recentHistory.takeLast(totalBars)
            }

            historyPadded.forEachIndexed { index, db ->
                val normalizedHeight = (db / 90f).coerceIn(0.08f, 1f) * size.height
                val x = index * (barWidth + spacing)
                val y = size.height - normalizedHeight

                val color = if (db >= 50f) SnoreMedium else if (db >= 65f) SnoreHigh else highlightColor

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, normalizedHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    iconColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = iconColor
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InactiveTrackingContent(
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Aucun enregistrement en cours",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Posez votre téléphone sur votre table de nuit et lancez le suivi avant de dormir.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lancer le suivi de nuit", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
