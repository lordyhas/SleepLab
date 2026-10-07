package com.lordyhas.sonrelab.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sonrelab.SonreLabApp
import com.lordyhas.sonrelab.data.local.entity.SleepSessionEntity
import com.lordyhas.sonrelab.data.local.entity.TreatmentEntity
import com.lordyhas.sonrelab.data.repository.SleepSessionRepository
import com.lordyhas.sonrelab.data.repository.TreatmentRepository
import com.lordyhas.sonrelab.service.SleepTrackingService
import com.lordyhas.sonrelab.service.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val latestSession: SleepSessionEntity? = null,
    val latestSessionTreatment: TreatmentEntity? = null,
    val totalSessionsCount: Int = 0,
    val averageSnoreDurationMinutes: Int = 0,
    val activeTreatments: List<TreatmentEntity> = emptyList(),
    val selectedTreatmentId: Int? = null
)

class HomeViewModel(
    private val sessionRepository: SleepSessionRepository = SonreLabApp.instance.sleepSessionRepository,
    private val treatmentRepository: TreatmentRepository = SonreLabApp.instance.treatmentRepository
) : ViewModel() {

    val trackingState: StateFlow<TrackingState> = SleepTrackingService.trackingState

    private val _selectedTreatmentId = MutableStateFlow<Int?>(null)
    val selectedTreatmentId: StateFlow<Int?> = _selectedTreatmentId.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.getAllSessions(),
        treatmentRepository.getActiveTreatments(),
        _selectedTreatmentId
    ) { sessions, activeTreatments, selectedId ->
        val latest = sessions.firstOrNull { it.endTime > 0 }
        val latestTreatment = latest?.treatmentId?.let { tid ->
            treatmentRepository.getTreatmentByIdDirect(tid)
        }
        val completedSessions = sessions.filter { it.endTime > 0 }
        val avgSnoreSec = if (completedSessions.isNotEmpty()) {
            completedSessions.map { it.totalSnoreDurationSeconds }.average().toInt()
        } else 0

        HomeUiState(
            latestSession = latest,
            latestSessionTreatment = latestTreatment,
            totalSessionsCount = completedSessions.size,
            averageSnoreDurationMinutes = avgSnoreSec / 60,
            activeTreatments = activeTreatments,
            selectedTreatmentId = selectedId ?: activeTreatments.firstOrNull()?.id
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectTreatment(treatmentId: Int?) {
        _selectedTreatmentId.value = treatmentId
    }

    fun startTracking(context: Context) {
        val treatmentId = _selectedTreatmentId.value ?: uiState.value.selectedTreatmentId
        SleepTrackingService.startService(context, treatmentId)
    }

    fun stopTracking(context: Context) {
        SleepTrackingService.stopService(context)
    }
}
