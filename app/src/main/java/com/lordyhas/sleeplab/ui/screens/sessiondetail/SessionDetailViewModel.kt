package com.lordyhas.sleeplab.ui.screens.sessiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val snoreEventCount: Int = 0,
    /** True once the night has been deleted; the screen should close. */
    val isDeleted: Boolean = false
)

class SessionDetailViewModel(
    private val sessionId: Int
) : ViewModel() {

    private val sessionRepository = SleepLabApp.instance.sleepSessionRepository
    private val noiseSampleRepository = SleepLabApp.instance.noiseSampleRepository
    private val snoreEventRepository = SleepLabApp.instance.snoreEventRepository
    private val treatmentRepository = SleepLabApp.instance.treatmentRepository

    private val _uiState = MutableStateFlow(SessionDetailUiState())
    val uiState: StateFlow<SessionDetailUiState> = _uiState.asStateFlow()

    init {
        loadRecordings()
        observeSession()
    }

    /** Events and samples never change after a night ends, so they're loaded once. */
    private fun loadRecordings() {
        viewModelScope.launch {
            val noiseSamples = noiseSampleRepository.getSamplesForSession(sessionId)
            val snoreEvents = snoreEventRepository.getEventsForSessionDirect(sessionId)
            val avgDb = noiseSampleRepository.getAverageDbForSession(sessionId) ?: 0f
            val maxDb = noiseSampleRepository.getMaxDbForSession(sessionId) ?: 0f
            val minDb = noiseSampleRepository.getMinDbForSession(sessionId) ?: 0f
            _uiState.update {
                it.copy(
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

    /** The session row itself is observed so note / rating edits show up immediately. */
    private fun observeSession() {
        viewModelScope.launch {
            sessionRepository.getSessionById(sessionId).collect { session ->
                val treatmentName = session?.treatmentId?.let {
                    treatmentRepository.getTreatmentByIdDirect(it)?.name
                }
                _uiState.update {
                    it.copy(isLoading = false, session = session, treatmentName = treatmentName)
                }
            }
        }
    }

    fun updateNote(note: String) {
        viewModelScope.launch { sessionRepository.updateNote(sessionId, note) }
    }

    fun updateMorningRating(rating: Int?) {
        viewModelScope.launch { sessionRepository.updateMorningRating(sessionId, rating) }
    }

    fun deleteNight() {
        // Flag first: deleting makes the observed session emit null, which must not
        // flash "session not found" before the screen closes.
        _uiState.update { it.copy(isDeleted = true) }
        // App scope, not viewModelScope: the screen closes right away and clearing this
        // ViewModel would otherwise cancel the delete half-way.
        SleepLabApp.instance.appScope.launch {
            SleepLabApp.instance.clipRepository.deleteSessionClips(sessionId)
            sessionRepository.deleteNight(sessionId)
        }
    }
}
