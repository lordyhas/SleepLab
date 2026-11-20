package com.lordyhas.sleeplab.ui.screens.sessiondetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.data.local.entity.NoiseSampleEntity
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.SnoreEventEntity
import com.lordyhas.sleeplab.data.local.entity.FactorEntity
import com.lordyhas.sleeplab.ui.util.NightTimeline
import com.lordyhas.sleeplab.ui.util.NightTimelines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import com.lordyhas.sleeplab.ui.util.SnoreEpisode
import com.lordyhas.sleeplab.ui.util.SnoreEpisodes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    /** Snores grouped into stretches of snoring, in time order. */
    val episodes: List<SnoreEpisode> = emptyList(),
    /** The night cut into 10 s slices with their snoring band, for the chart and band totals. */
    val timeline: NightTimeline? = null,
    /** Neighbouring completed nights, for the previous / next arrows. */
    val previousSessionId: Int? = null,
    val nextSessionId: Int? = null,
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
    private val factorRepository = SleepLabApp.instance.factorRepository
    private val settingsRepository = SleepLabApp.instance.settingsRepository

    /** Offer to turn recordings on: they're off and the offer wasn't dismissed. */
    val showClipsNudge: StateFlow<Boolean> = combine(
        settingsRepository.clipsEnabled,
        settingsRepository.clipsNudgeDismissed
    ) { enabled, dismissed -> !enabled && !dismissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val factors: StateFlow<List<FactorEntity>> = factorRepository.getAllFactors()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val factorIds: StateFlow<Set<Int>> = factorRepository.getFactorIdsForSession(sessionId)
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

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
            val session = sessionRepository.getSessionByIdDirect(sessionId)
            val timeline = session?.takeIf { it.endTime > it.startTime }?.let {
                withContext(Dispatchers.Default) {
                    NightTimelines.build(it.startTime, it.endTime, noiseSamples, snoreEvents, it.thresholdDb)
                }
            }
            val nights = sessionRepository.getAllSessions().first()
                .filter { it.endTime > 0 }
                .sortedBy { it.startTime }
            val index = nights.indexOfFirst { it.id == sessionId }
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
                    snoreEventCount = snoreEvents.size,
                    episodes = SnoreEpisodes.group(snoreEvents),
                    timeline = timeline,
                    previousSessionId = nights.getOrNull(index - 1)?.id?.takeIf { index > 0 },
                    nextSessionId = nights.getOrNull(index + 1)?.id?.takeIf { index >= 0 }
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

    fun toggleFactor(factorId: Int) {
        val current = factorIds.value
        viewModelScope.launch {
            factorRepository.setSessionFactors(sessionId, if (factorId in current) current - factorId else current + factorId)
        }
    }

    fun addCustomFactor(name: String) {
        viewModelScope.launch {
            val id = factorRepository.addCustomFactor(name)
            factorRepository.setSessionFactors(sessionId, factorIds.value + id)
        }
    }

    fun updateNote(note: String) {
        viewModelScope.launch { sessionRepository.updateNote(sessionId, note) }
    }

    fun updateMorningRating(rating: Int?) {
        viewModelScope.launch { sessionRepository.updateMorningRating(sessionId, rating) }
    }

    fun enableClips() = settingsRepository.setClipsEnabled(true)

    fun dismissClipsNudge() = settingsRepository.dismissClipsNudge()

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
