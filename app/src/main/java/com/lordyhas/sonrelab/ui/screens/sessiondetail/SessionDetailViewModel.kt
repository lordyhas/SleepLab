package com.lordyhas.sonrelab.ui.screens.sessiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sonrelab.SonreLabApp
import com.lordyhas.sonrelab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sonrelab.data.local.entity.SleepSessionEntity
import com.lordyhas.sonrelab.data.local.entity.SnoreEventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionDetailUiState(
    val isLoading: Boolean = true,
    val session: SleepSessionEntity? = null,
    val treatmentName: String? = null,
    val noiseSamples: List<NoiseSampleEntity> = emptyList(),
    val snoreEvents: List<SnoreEventEntity> = emptyList(),
    val averageDb: Float = 0f,
    val maxDb: Float = 0f,
    val minDb: Float = 0f,
    val snoreEventCount: Int = 0
)

class SessionDetailViewModel(
    private val sessionId: Int
) : ViewModel() {

    private val sessionRepository = SonreLabApp.instance.sleepSessionRepository
    private val noiseSampleRepository = SonreLabApp.instance.noiseSampleRepository
    private val snoreEventRepository = SonreLabApp.instance.snoreEventRepository
    private val treatmentRepository = SonreLabApp.instance.treatmentRepository

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        loadSessionData()
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            val session = sessionRepository.getSessionByIdDirect(sessionId)
            if (session == null) {
                _uiState.value = SessionDetailUiState(isLoading = false)
                return@launch
            }

            val treatmentName = session.treatmentId?.let {
                treatmentRepository.getTreatmentByIdDirect(it)?.name
            }

            val noiseSamples = noiseSampleRepository.getSamplesForSession(sessionId)
            val snoreEvents = snoreEventRepository.getEventsForSessionDirect(sessionId)
            val avgDb = noiseSampleRepository.getAverageDbForSession(sessionId) ?: 0f
            val maxDb = noiseSampleRepository.getMaxDbForSession(sessionId) ?: 0f
            val minDb = noiseSampleRepository.getMinDbForSession(sessionId) ?: 0f

            _uiState.value = SessionDetailUiState(
                isLoading = false,
                session = session,
                treatmentName = treatmentName,
                noiseSamples = noiseSamples,
                snoreEvents = snoreEvents,
                averageDb = avgDb,
                maxDb = maxDb,
                minDb = minDb,
                snoreEventCount = snoreEvents.size
            )
        }
    }
}
