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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackingViewModel(
    private val treatmentRepository: TreatmentRepository = SleepLabApp.instance.treatmentRepository,
    private val settingsRepository: SettingsRepository = SleepLabApp.instance.settingsRepository
) : ViewModel() {

    val trackingState: StateFlow<TrackingState> = SleepTrackingService.trackingState

    val allTreatments: StateFlow<List<TreatmentEntity>> = treatmentRepository.getAllTreatments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Whether the dark night screen takes over once the phone is left alone during a night. */
    val nightScreenEnabled: StateFlow<Boolean> = settingsRepository.nightScreenEnabled

    fun startTracking(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            // Read tonight's treatment from the database at start time: nothing on screen keeps a
            // cached copy current, and a tile/reminder start can happen before any UI loaded it.
            val treatmentId = SettingsRepository.resolveTreatmentId(
                settingsRepository.treatmentChoice.value,
                treatmentRepository.getActiveTreatments().first()
            )
            SleepTrackingService.startService(
                appContext,
                treatmentId = treatmentId,
                thresholdDb = settingsRepository.thresholdDb.value,
                autoCalibrate = settingsRepository.autoCalibrate.value
            )
        }
    }

    fun stopTracking(context: Context) {
        SleepTrackingService.stopService(context)
    }
}
