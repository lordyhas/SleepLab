package com.lordyhas.sonrelab.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sonrelab.SonreLabApp
import com.lordyhas.sonrelab.data.local.entity.SleepSessionEntity
import com.lordyhas.sonrelab.data.local.entity.TreatmentEntity
import com.lordyhas.sonrelab.data.repository.SleepSessionRepository
import com.lordyhas.sonrelab.data.repository.TreatmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SessionWithTreatment(
    val session: SleepSessionEntity,
    val treatmentName: String?
)

data class TreatmentImpact(
    val treatmentId: Int?,
    val treatmentName: String,
    val sessionCount: Int,
    val averageSnoreMinutes: Float,
    val averageIntensityScore: Float,
    val reductionPercentage: Float? // Compared to no treatment
)

data class AnalyticsUiState(
    val sessionsWithTreatments: List<SessionWithTreatment> = emptyList(),
    val treatmentImpacts: List<TreatmentImpact> = emptyList(),
    val overallImprovementPercentage: Float? = null,
    val averageSnoreWithoutTreatmentMinutes: Float = 0f,
    val averageSnoreWithTreatmentMinutes: Float = 0f
)

class AnalyticsViewModel(
    sessionRepository: SleepSessionRepository = SonreLabApp.instance.sleepSessionRepository,
    treatmentRepository: TreatmentRepository = SonreLabApp.instance.treatmentRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        sessionRepository.getAllSessions(),
        treatmentRepository.getAllTreatments()
    ) { sessions, treatments ->
        val treatmentMap = treatments.associateBy { it.id }

        val completedSessions = sessions.filter { it.endTime > 0 }
            .sortedBy { it.startTime }

        val sessionsWithTreatments = completedSessions.map { session ->
            SessionWithTreatment(
                session = session,
                treatmentName = session.treatmentId?.let { treatmentMap[it]?.name }
            )
        }

        // Calculate metrics
        val noTreatmentSessions = completedSessions.filter { it.treatmentId == null }
        val withTreatmentSessions = completedSessions.filter { it.treatmentId != null }

        val avgNoTreatmentMinutes = if (noTreatmentSessions.isNotEmpty()) {
            noTreatmentSessions.map { it.totalSnoreDurationSeconds / 60f }.average().toFloat()
        } else 0f

        val avgWithTreatmentMinutes = if (withTreatmentSessions.isNotEmpty()) {
            withTreatmentSessions.map { it.totalSnoreDurationSeconds / 60f }.average().toFloat()
        } else 0f

        val overallImprovement = if (avgNoTreatmentMinutes > 0f && withTreatmentSessions.isNotEmpty()) {
            ((avgNoTreatmentMinutes - avgWithTreatmentMinutes) / avgNoTreatmentMinutes * 100f)
        } else null

        // Per-treatment breakdown
        val impacts = mutableListOf<TreatmentImpact>()

        if (noTreatmentSessions.isNotEmpty()) {
            impacts.add(
                TreatmentImpact(
                    treatmentId = null,
                    treatmentName = "Sans traitement",
                    sessionCount = noTreatmentSessions.size,
                    averageSnoreMinutes = avgNoTreatmentMinutes,
                    averageIntensityScore = noTreatmentSessions.map { it.snoreIntensityScore }.average().toFloat(),
                    reductionPercentage = null
                )
            )
        }

        val groupedByTreatment = withTreatmentSessions.groupBy { it.treatmentId }
        groupedByTreatment.forEach { (tid, group) ->
            val tName = treatmentMap[tid]?.name ?: "Traitement #$tid"
            val avgMin = group.map { it.totalSnoreDurationSeconds / 60f }.average().toFloat()
            val avgScore = group.map { it.snoreIntensityScore }.average().toFloat()
            val reduction = if (avgNoTreatmentMinutes > 0f) {
                ((avgNoTreatmentMinutes - avgMin) / avgNoTreatmentMinutes * 100f)
            } else null

            impacts.add(
                TreatmentImpact(
                    treatmentId = tid,
                    treatmentName = tName,
                    sessionCount = group.size,
                    averageSnoreMinutes = avgMin,
                    averageIntensityScore = avgScore,
                    reductionPercentage = reduction
                )
            )
        }

        AnalyticsUiState(
            sessionsWithTreatments = sessionsWithTreatments,
            treatmentImpacts = impacts,
            overallImprovementPercentage = overallImprovement,
            averageSnoreWithoutTreatmentMinutes = avgNoTreatmentMinutes,
            averageSnoreWithTreatmentMinutes = avgWithTreatmentMinutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )
}
