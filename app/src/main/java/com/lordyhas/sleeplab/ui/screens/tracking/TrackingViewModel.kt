package com.lordyhas.sleeplab.ui.screens.tracking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import com.lordyhas.sleeplab.data.repository.SettingsRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
import com.lordyhas.sleeplab.service.SleepTrackingService
import com.lordyhas.sleeplab.service.TrackingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class TrackingViewModel(
    private val treatmentRepository: TreatmentRepository = SleepLabApp.instance.treatmentRepository,
    private val settingsRepository: SettingsRepository = SleepLabApp.instance.settingsRepository
) : ViewModel() {

    val trackingState: StateFlow<TrackingState> = SleepTrackingService.trackingState

    val thresholdDb: StateFlow<Float> = settingsRepository.thresholdDb

    val allTreatments: StateFlow<List<TreatmentEntity>> = treatmentRepository.getAllTreatments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Treatment that will be recorded if a night is started from this screen. */
    val plannedTreatment: StateFlow<TreatmentEntity?> = combine(
        treatmentRepository.getActiveTreatments(),
        settingsRepository.treatmentChoice
    ) { active, choice ->
        val id = SettingsRepository.resolveTreatmentId(choice, active)
        active.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun startTracking(context: Context) {
        SleepTrackingService.startService(
            context,
            treatmentId = plannedTreatment.value?.id,
            thresholdDb = settingsRepository.thresholdDb.value
        )
    }

    fun stopTracking(context: Context) {
        SleepTrackingService.stopService(context)
    }
}
