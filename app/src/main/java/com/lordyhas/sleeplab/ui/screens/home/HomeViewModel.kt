package com.lordyhas.sleeplab.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.data.local.entity.FactorEntity
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import com.lordyhas.sleeplab.data.repository.FactorRepository
import com.lordyhas.sleeplab.data.repository.SettingsRepository
import com.lordyhas.sleeplab.data.repository.SleepSessionRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    /** False until the database has answered once (avoids flashing "no night yet"). */
    val isLoaded: Boolean = false,
    val latestSession: SleepSessionEntity? = null,
    val latestSessionTreatment: TreatmentEntity? = null,
    val activeTreatments: List<TreatmentEntity> = emptyList(),
    val factors: List<FactorEntity> = emptyList(),
    val tonightFactorIds: Set<Int> = emptySet(),
    val selectedTreatmentId: Int? = null
)

/** State for the Tonight tab when no night is recorded: tonight's choices and the latest night. */
class HomeViewModel(
    private val sessionRepository: SleepSessionRepository = SleepLabApp.instance.sleepSessionRepository,
    private val treatmentRepository: TreatmentRepository = SleepLabApp.instance.treatmentRepository,
    private val settingsRepository: SettingsRepository = SleepLabApp.instance.settingsRepository,
    private val factorRepository: FactorRepository = SleepLabApp.instance.factorRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        sessionRepository.getAllSessions(),
        treatmentRepository.getActiveTreatments(),
        settingsRepository.treatmentChoice,
        factorRepository.getAllFactors(),
        settingsRepository.tonightFactorIds
    ) { sessions, activeTreatments, choice, factors, tonightFactorIds ->
        val latest = sessions.firstOrNull { it.endTime > 0 }
        val latestTreatment = latest?.treatmentId?.let { tid ->
            treatmentRepository.getTreatmentByIdDirect(tid)
        }

        HomeUiState(
            isLoaded = true,
            latestSession = latest,
            latestSessionTreatment = latestTreatment,
            activeTreatments = activeTreatments,
            factors = factors,
            tonightFactorIds = tonightFactorIds,
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

    fun toggleTonightFactor(factorId: Int) {
        val current = settingsRepository.tonightFactorIds.value
        settingsRepository.setTonightFactorIds(if (factorId in current) current - factorId else current + factorId)
    }

    fun addCustomFactor(name: String) {
        viewModelScope.launch {
            val id = factorRepository.addCustomFactor(name)
            settingsRepository.setTonightFactorIds(settingsRepository.tonightFactorIds.value + id)
        }
    }

    fun selectTreatment(treatmentId: Int?) {
        settingsRepository.setTreatmentChoice(treatmentId)
    }
}
