package com.lordyhas.sonrelab.ui.screens.tracking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lordyhas.sonrelab.SonreLabApp
import com.lordyhas.sonrelab.data.local.entity.TreatmentEntity
import com.lordyhas.sonrelab.data.repository.TreatmentRepository
import com.lordyhas.sonrelab.service.SleepTrackingService
import com.lordyhas.sonrelab.service.TrackingState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TrackingViewModel(
    treatmentRepository: TreatmentRepository = SonreLabApp.instance.treatmentRepository
) : ViewModel() {

    val trackingState: StateFlow<TrackingState> = SleepTrackingService.trackingState

    val activeTreatments: StateFlow<List<TreatmentEntity>> = treatmentRepository.getActiveTreatments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startTracking(context: Context, treatmentId: Int? = null, thresholdDb: Float = 50f) {
        SleepTrackingService.startService(context, treatmentId, thresholdDb)
    }

    fun stopTracking(context: Context) {
        SleepTrackingService.stopService(context)
    }
}
