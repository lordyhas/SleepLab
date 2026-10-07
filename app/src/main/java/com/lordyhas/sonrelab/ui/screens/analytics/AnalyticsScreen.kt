package com.lordyhas.sonrelab.ui.screens.analytics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.TrendingDown
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analyses & Efficacité",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (uiState.sessionsWithTreatments.isEmpty()) {
                item {
                    EmptyAnalyticsCard()
                }
            } else {
                // Treatment Effectiveness Hero Card
                if (uiState.overallImprovementPercentage != null) {
                    item {
                        TreatmentImpactHeroCard(
                            improvementPercent = uiState.overallImprovementPercentage!!,
                            avgNoTreatment = uiState.averageSnoreWithoutTreatmentMinutes,
                            avgWithTreatment = uiState.averageSnoreWithTreatmentMinutes
                        )
                    }
                }

                // Evolution Chart
                item {
                    SnoreEvolutionChartCard(sessions = uiState.sessionsWithTreatments)
                }

                // Per-treatment breakdown
                item {
                    Text(
                        "Détail par Traitement",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.treatmentImpacts, key = { it.treatmentName }) { impact ->
                    TreatmentPerformanceCard(impact = impact)
                }

                // History section
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Historique des Nuits",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(uiState.sessionsWithTreatments.reversed(), key = { it.session.id }) { item ->
                    SessionHistoryCard(item = item)
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun TreatmentImpactHeroCard(
    improvementPercent: Float,
    avgNoTreatment: Float,
    avgWithTreatment: Float
) {
    val isPositive = improvementPercent > 0

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isPositive) SleepTealAccent.copy(alpha = 0.2f) else SnoreMedium.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPositive) Icons.AutoMirrored.Filled.TrendingDown else Icons.Default.BarChart,
                        contentDescription = null,
                        tint = if (isPositive) SleepTealAccent else SnoreMedium,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        "Impact Médical des Traitements",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        if (isPositive) "Réduction observée des ronflements" else "Données en cours de stabilisation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPositive) "-${improvementPercent.toInt()}%" else "+${(-improvementPercent).toInt()}%",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) SleepTealAccent else SnoreMedium
                    )
                    Text(
                        text = if (isPositive) "de temps de ronflement" else "variation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Sans traitement : ${avgNoTreatment.toInt()} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = SnoreMedium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Avec traitement : ${avgWithTreatment.toInt()} min",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = SleepTealAccent
                    )
                }
            }
        }
    }
}

@Composable
fun SnoreEvolutionChartCard(
    sessions: List<SessionWithTreatment>
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

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
            Text(
                "Évolution des Ronflements (Minutes / Nuit)",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SleepIndigoPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sans traitement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SleepTealAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Avec traitement", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Canvas Bar Chart
            val recentSessions = sessions.takeLast(10)
            val maxMinutes = (recentSessions.maxOfOrNull { it.session.totalSnoreDurationSeconds / 60f } ?: 60f).coerceAtLeast(15f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barCount = recentSessions.size
                    if (barCount == 0) return@Canvas

                    val barWidth = (size.width / (barCount * 1.8f)).coerceAtMost(36.dp.toPx())
                    val totalSlotsWidth = size.width
                    val slotWidth = totalSlotsWidth / barCount

                    // Guide Lines
                    val lineY = size.height * 0.5f
                    drawLine(
                        color = Color.White.copy(alpha = 0.1f),
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    recentSessions.forEachIndexed { index, item ->
                        val minutes = item.session.totalSnoreDurationSeconds / 60f
                        val heightFraction = (minutes / maxMinutes).coerceIn(0.05f, 1f)
                        val barHeight = heightFraction * (size.height - 24.dp.toPx())

                        val centerX = (index * slotWidth) + (slotWidth / 2f)
                        val x = centerX - (barWidth / 2f)
                        val y = size.height - barHeight - 20.dp.toPx()

                        val isWithTreatment = item.session.treatmentId != null
                        val barColor = if (isWithTreatment) SleepTealAccent else SleepIndigoPrimary

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }

                // Date Labels Row under the bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    recentSessions.forEach { item ->
                        Text(
                            text = dateFormat.format(Date(item.session.startTime)),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TreatmentPerformanceCard(impact: TreatmentImpact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = impact.treatmentName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${impact.sessionCount} nuit(s) enregistrée(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${impact.averageSnoreMinutes.toInt()} min / nuit",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (impact.reductionPercentage != null) {
                    val isReduction = impact.reductionPercentage > 0
                    Text(
                        text = if (isReduction) "-${impact.reductionPercentage.toInt()}% ronflement" else "+${(-impact.reductionPercentage).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isReduction) SleepTealAccent else SnoreMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(item: SessionWithTreatment) {
    val dateFormat = remember { SimpleDateFormat("EEE d MMM HH:mm", Locale.getDefault()) }
    val totalSleepSeconds = ((item.session.endTime - item.session.startTime) / 1000).coerceAtLeast(0)
    val sleepHours = totalSleepSeconds / 3600
    val sleepMinutes = (totalSleepSeconds % 3600) / 60
    val snoreMin = item.session.totalSnoreDurationSeconds / 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = dateFormat.format(Date(item.session.startTime)),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (item.treatmentName != null) "🔬 ${item.treatmentName}" else "Sans traitement",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.treatmentName != null) SleepTealAccent else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${snoreMin} min de ronflements",
                    fontWeight = FontWeight.Bold,
                    color = if (snoreMin > 30) SnoreHigh else if (snoreMin > 10) SnoreMedium else SnoreLow,
                    fontSize = 13.sp
                )
                Text(
                    text = "Sommeil : ${sleepHours}h ${sleepMinutes}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyAnalyticsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MidnightSurfaceCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                tint = SleepIndigoLight,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Données insuffisantes",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Enregistrez vos premières nuits pour débloquer les graphiques d'évolution et l'analyse comparative de vos traitements.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
