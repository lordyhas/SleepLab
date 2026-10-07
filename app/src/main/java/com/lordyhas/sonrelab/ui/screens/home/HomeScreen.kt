package com.lordyhas.sonrelab.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sonrelab.data.local.entity.SleepSessionEntity
import com.lordyhas.sonrelab.data.local.entity.TreatmentEntity
import com.lordyhas.sonrelab.ui.theme.MidnightSurfaceCard
import com.lordyhas.sonrelab.ui.theme.SleepIndigoLight
import com.lordyhas.sonrelab.ui.theme.SleepIndigoPrimary
import com.lordyhas.sonrelab.ui.theme.SleepTealAccent
import com.lordyhas.sonrelab.ui.theme.SnoreHigh
import com.lordyhas.sonrelab.ui.theme.SnoreLow
import com.lordyhas.sonrelab.ui.theme.SnoreMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onNavigateToTracking: () -> Unit = {},
    onNavigateToTreatments: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val trackingState by viewModel.trackingState.collectAsState()

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            viewModel.startTracking(context)
            onNavigateToTracking()
        }
    }

    fun launchTrackingWithPermissionCheck() {
        val permissions = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionsLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(SleepIndigoPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Nightlight,
                                contentDescription = null,
                                tint = SleepIndigoLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "SnoreTracker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Start Sleep Card
            item {
                HeroStartSleepCard(
                    isTracking = trackingState.isTracking,
                    elapsedSeconds = trackingState.elapsedSeconds,
                    onStartClick = {
                        launchTrackingWithPermissionCheck()
                    },
                    onViewLiveClick = onNavigateToTracking
                )
            }

            // Treatment Selector for tonight
            item {
                TreatmentSelectorSection(
                    activeTreatments = uiState.activeTreatments,
                    selectedId = uiState.selectedTreatmentId,
                    onSelect = { viewModel.selectTreatment(it) },
                    onManageClick = onNavigateToTreatments
                )
            }

            // Last Night Summary Card
            item {
                LastNightSummaryCard(
                    latestSession = uiState.latestSession,
                    treatment = uiState.latestSessionTreatment
                )
            }

            // Quick Stats Overview
            item {
                QuickStatsSection(
                    totalSessions = uiState.totalSessionsCount,
                    avgSnoreMinutes = uiState.averageSnoreDurationMinutes
                )
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
fun HeroStartSleepCard(
    isTracking: Boolean,
    elapsedSeconds: Long,
    onStartClick: () -> Unit,
    onViewLiveClick: () -> Unit
) {
    val gradient = if (isTracking) {
        Brush.linearGradient(
            listOf(Color(0xFF065F46), Color(0xFF047857))
        )
    } else {
        Brush.linearGradient(
            listOf(Color(0xFF3730A3), Color(0xFF1E1B4B))
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isTracking) {
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60
                    val timerStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                    Text(
                        "Suivi du sommeil actif",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        timerStr,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleepTealAccent
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onViewLiveClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF065F46)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voir le direct & Arrêter", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Prêt pour une bonne nuit ?",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Enregistrez vos ronflements et évaluez vos traitements",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onStartClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleepTealAccent,
                            contentColor = Color(0xFF042F2E)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Démarrer le sommeil", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreatmentSelectorSection(
    activeTreatments: List<TreatmentEntity>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit,
    onManageClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Traitement appliqué cette nuit :",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Gérer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onManageClick() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedId == null,
                    onClick = { onSelect(null) },
                    label = { Text("Sans traitement") },
                    leadingIcon = if (selectedId == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }

            items(activeTreatments, key = { it.id }) { treatment ->
                FilterChip(
                    selected = selectedId == treatment.id,
                    onClick = { onSelect(treatment.id) },
                    label = { Text(treatment.name) },
                    leadingIcon = if (selectedId == treatment.id) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

@Composable
fun LastNightSummaryCard(
    latestSession: SleepSessionEntity?,
    treatment: TreatmentEntity?
) {
    val dateFormat = remember { SimpleDateFormat("EEEE d MMMM", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Dernière Nuit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (latestSession != null) {
                    Text(
                        dateFormat.format(Date(latestSession.startTime)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (latestSession == null) {
                Text(
                    "Aucune nuit enregistrée pour le moment. Lancez votre premier suivi pour voir vos résultats !",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val totalSleepSeconds = ((latestSession.endTime - latestSession.startTime) / 1000).coerceAtLeast(0)
                val sleepHours = totalSleepSeconds / 3600
                val sleepMinutes = (totalSleepSeconds % 3600) / 60

                val snoreMinutes = latestSession.totalSnoreDurationSeconds / 60
                val snoreSeconds = latestSession.totalSnoreDurationSeconds % 60

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        title = "Sommeil",
                        value = "${sleepHours}h ${sleepMinutes}m",
                        icon = Icons.Default.Timer,
                        tint = SleepIndigoLight
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        title = "Ronflements",
                        value = "${snoreMinutes}m ${snoreSeconds}s",
                        icon = Icons.Default.GraphicEq,
                        tint = SnoreMedium
                    )
                    SummaryMetric(
                        modifier = Modifier.weight(1f),
                        title = "Score dB",
                        value = "${latestSession.snoreIntensityScore.toInt()} / 100",
                        icon = Icons.Default.Speed,
                        tint = if (latestSession.snoreIntensityScore > 55) SnoreHigh else SnoreLow
                    )
                }

                if (treatment != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Traitement associé : ${treatment.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryMetric(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun QuickStatsSection(
    totalSessions: Int,
    avgSnoreMinutes: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Total Nuits Suivies",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$totalSessions",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleepTealAccent
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Moyenne Ronflements",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${avgSnoreMinutes} min / nuit",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SleepIndigoLight
                )
            }
        }
    }
}
