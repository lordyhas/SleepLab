package com.lordyhas.sleeplab.ui.screens.treatments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import com.lordyhas.sleeplab.data.repository.SettingsRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TreatmentsViewModel(
    private val treatmentRepository: TreatmentRepository = SleepLabApp.instance.treatmentRepository,
    private val settingsRepository: SettingsRepository = SleepLabApp.instance.settingsRepository
) : ViewModel() {

    val treatments: StateFlow<List<TreatmentEntity>> = treatmentRepository.getAllTreatments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val thresholdDb: StateFlow<Float> = settingsRepository.thresholdDb

    val autoCalibrate: StateFlow<Boolean> = settingsRepository.autoCalibrate

    fun setAutoCalibrate(enabled: Boolean) {
        settingsRepository.setAutoCalibrate(enabled)
    }

    fun updateThreshold(value: Float) {
        settingsRepository.setThresholdDb(value)
    }

    fun addTreatment(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            treatmentRepository.insertTreatment(name.trim())
        }
    }

    fun toggleTreatmentActive(treatment: TreatmentEntity) {
        viewModelScope.launch {
            treatmentRepository.updateTreatment(treatment.copy(isActive = !treatment.isActive))
        }
    }

    fun deleteTreatment(treatment: TreatmentEntity) {
        viewModelScope.launch {
            treatmentRepository.deleteTreatment(treatment)
        }
    }
}
