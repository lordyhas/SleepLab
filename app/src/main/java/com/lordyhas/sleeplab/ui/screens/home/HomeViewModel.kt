package com.lordyhas.sleeplab.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import com.lordyhas.sleeplab.data.repository.SettingsRepository
import com.lordyhas.sleeplab.data.repository.SleepSessionRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
import com.lordyhas.sleeplab.service.SleepTrackingService
import com.lordyhas.sleeplab.service.TrackingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val latestSession: SleepSessionEntity? = null,
    val latestSessionTreatment: TreatmentEntity? = null,
    val totalSessionsCount: Int = 0,
    val averageSnoreDurationMinutes: Int = 0,
    val bestNightSnoreMinutes: Int? = null,
    val activeTreatments: List<TreatmentEntity> = emptyList(),
    val selectedTreatmentId: Int? = null
)

class HomeViewModel(
    private val sessionRepository: SleepSessionRepository = SleepLabApp.instance.sleepSessionRepository,
    private val treatmentRepository: TreatmentRepository = SleepLabApp.instance.treatmentRepository,
    private val settingsRepository: SettingsRepository = SleepLabApp.instance.settingsRepository
) : ViewModel() {

    val trackingState: StateFlow<TrackingState> = SleepTrackingService.trackingState

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.getAllSessions(),
        treatmentRepository.getActiveTreatments(),
        settingsRepository.treatmentChoice
    ) { sessions, activeTreatments, choice ->
        val completedSessions = sessions.filter { it.endTime > 0 }
        val latest = completedSessions.firstOrNull()
        val latestTreatment = latest?.treatmentId?.let { tid ->
            treatmentRepository.getTreatmentByIdDirect(tid)
        }
        val avgSnoreSec = if (completedSessions.isNotEmpty()) {
            completedSessions.map { it.totalSnoreDurationSeconds }.average().toInt()
        } else 0

        HomeUiState(
            latestSession = latest,
            latestSessionTreatment = latestTreatment,
            totalSessionsCount = completedSessions.size,
            averageSnoreDurationMinutes = avgSnoreSec / 60,
            bestNightSnoreMinutes = completedSessions.minOfOrNull { it.totalSnoreDurationSeconds }?.div(60),
            activeTreatments = activeTreatments,
            selectedTreatmentId = SettingsRepository.resolveTreatmentId(choice, activeTreatments)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun rateNight(sessionId: Int, rating: Int) {
        viewModelScope.launch { sessionRepository.updateMorningRating(sessionId, rating) }
    }

    fun selectTreatment(treatmentId: Int?) {
        settingsRepository.setTreatmentChoice(treatmentId)
    }

    fun startTracking(context: Context) {
        SleepTrackingService.startService(
            context,
            treatmentId = uiState.value.selectedTreatmentId,
            thresholdDb = settingsRepository.thresholdDb.value,
            autoCalibrate = settingsRepository.autoCalibrate.value
        )
    }
}
