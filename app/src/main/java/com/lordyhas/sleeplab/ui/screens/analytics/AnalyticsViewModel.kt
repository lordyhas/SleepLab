package com.lordyhas.sleeplab.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import com.lordyhas.sleeplab.data.local.entity.FactorEntity
import com.lordyhas.sleeplab.data.repository.FactorRepository
import com.lordyhas.sleeplab.data.repository.SleepSessionRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
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
    val treatmentName: String?, // null = nights without treatment (baseline)
    val sessionCount: Int,
    val averageSnoreMinutes: Float,
    val averageIntensityScore: Float,
    val reductionPercentage: Float? // Compared to no treatment
)

data class AnalyticsUiState(
    /** False until the database has answered once. */
    val isLoaded: Boolean = false,
    val sessionsWithTreatments: List<SessionWithTreatment> = emptyList(),
    val treatmentImpacts: List<TreatmentImpact> = emptyList(),
    val overallImprovementPercentage: Float? = null,
    val averageSnoreWithoutTreatmentMinutes: Float = 0f,
    val averageSnoreWithTreatmentMinutes: Float = 0f,
    val averageSnoreMinutes: Float = 0f,
    val averageSleepSeconds: Long = 0L,
    val weekTrend: List<TrendPoint> = emptyList(),
    val monthTrend: List<TrendPoint> = emptyList(),
    /** Factors used at least once, with their impact; reliable comparisons first. */
    val factorImpacts: List<Pair<FactorEntity, FactorImpact>> = emptyList(),
    val ratingBreakdown: List<RatingPoint> = emptyList(),
    val ratedNights: Int = 0,
    val minutesByDay: Map<Int, Float> = emptyMap(),
    /** Latest night of each night-day, for calendar taps. */
    val sessionIdByDay: Map<Int, Int> = emptyMap()
)

class AnalyticsViewModel(
    sessionRepository: SleepSessionRepository = SleepLabApp.instance.sleepSessionRepository,
    treatmentRepository: TreatmentRepository = SleepLabApp.instance.treatmentRepository,
    factorRepository: FactorRepository = SleepLabApp.instance.factorRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = combine(
        sessionRepository.getAllSessions(),
        treatmentRepository.getAllTreatments(),
        factorRepository.getAllFactors(),
        factorRepository.getAllSessionFactors()
    ) { sessions, treatments, factors, sessionFactors ->
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
                    treatmentName = null,
                    sessionCount = noTreatmentSessions.size,
                    averageSnoreMinutes = avgNoTreatmentMinutes,
                    averageIntensityScore = noTreatmentSessions.map { it.snoreIntensityScore }.average().toFloat(),
                    reductionPercentage = null
                )
            )
        }

        val groupedByTreatment = withTreatmentSessions.groupBy { it.treatmentId }
        groupedByTreatment.forEach { (tid, group) ->
            val tName = treatmentMap[tid]?.name ?: "#$tid"
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

        val nights = completedSessions.map {
            NightData(it.id, it.startTime, it.totalSnoreDurationSeconds / 60f, it.morningRating)
        }
        val factorsByNight = sessionFactors.groupBy({ it.sessionId }, { it.factorId }).mapValues { it.value.toSet() }
        val factorById = factors.associateBy { it.id }
        val factorImpacts = Insights.factorImpacts(nights, factorsByNight, factors.map { it.id })
            .filter { it.nightsWith > 0 }
            .sortedWith(compareByDescending<FactorImpact> { it.isReliable }.thenByDescending { kotlin.math.abs(it.changePercent ?: 0) })
            .mapNotNull { impact -> factorById[impact.factorId]?.let { it to impact } }

        AnalyticsUiState(
            isLoaded = true,
            weekTrend = Insights.trend(nights, TrendPeriod.WEEKS, limit = 12),
            monthTrend = Insights.trend(nights, TrendPeriod.MONTHS, limit = 12),
            factorImpacts = factorImpacts,
            ratingBreakdown = Insights.ratingBreakdown(nights),
            ratedNights = nights.count { it.morningRating != null },
            minutesByDay = Insights.minutesByDay(nights),
            sessionIdByDay = nights.sortedBy { it.startTime }.associate { Insights.nightDayKey(it.startTime) to it.sessionId },
            sessionsWithTreatments = sessionsWithTreatments,
            treatmentImpacts = impacts,
            overallImprovementPercentage = overallImprovement,
            averageSnoreWithoutTreatmentMinutes = avgNoTreatmentMinutes,
            averageSnoreWithTreatmentMinutes = avgWithTreatmentMinutes,
            averageSnoreMinutes = completedSessions.map { it.totalSnoreDurationSeconds / 60f }
                .average().toFloat().takeIf { !it.isNaN() } ?: 0f,
            averageSleepSeconds = completedSessions.map { (it.endTime - it.startTime) / 1000 }
                .average().takeIf { !it.isNaN() }?.toLong() ?: 0L
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )
}
