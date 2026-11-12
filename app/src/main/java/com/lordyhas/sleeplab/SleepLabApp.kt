package com.lordyhas.sleeplab

import android.app.Application
import com.lordyhas.sleeplab.data.local.AppDatabase
import com.lordyhas.sleeplab.data.repository.SleepSessionRepository
import com.lordyhas.sleeplab.data.repository.SnoreEventRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
import com.lordyhas.sleeplab.data.repository.NoiseSampleRepository
import com.lordyhas.sleeplab.data.repository.ClipRepository
import com.lordyhas.sleeplab.data.repository.FactorRepository
import com.lordyhas.sleeplab.data.repository.SettingsRepository
import android.util.Log
import com.lordyhas.sleeplab.service.NightRecovery
import com.lordyhas.sleeplab.service.SleepTrackingService
import com.lordyhas.sleeplab.service.detection.SnoreScore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SleepLabApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val treatmentRepository by lazy { TreatmentRepository(database.treatmentDao()) }
    val sleepSessionRepository by lazy { SleepSessionRepository(database.sleepSessionDao()) }
    val snoreEventRepository by lazy { SnoreEventRepository(database.snoreEventDao()) }
    val noiseSampleRepository by lazy { NoiseSampleRepository(database.noiseSampleDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val factorRepository by lazy { FactorRepository(database.factorDao()) }
    val clipRepository by lazy { ClipRepository(this, database.snoreEventDao(), settingsRepository) }

    /** App-lifetime scope for housekeeping that must outlive any screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch {
            recoverInterruptedNights()
            clipRepository.purgeExpired()
        }
    }

    /**
     * A night whose recording died with the process (killed by the system, crash, reboot) is left
     * open in the database. The service lives in this process, so at startup every open night is
     * such a leftover: close it from what was recorded, or drop it if that is next to nothing.
     */
    private suspend fun recoverInterruptedNights() {
        val liveSessionId = SleepTrackingService.trackingState.value.sessionId
            .takeIf { SleepTrackingService.trackingState.value.isTracking }
        sleepSessionRepository.getOpenSessions()
            .filter { it.id != liveSessionId }
            .forEach { session ->
                try {
                    val recovered = NightRecovery.recover(
                        session = session,
                        events = snoreEventRepository.getEventsForSessionDirect(session.id),
                        samples = noiseSampleRepository.getSamplesForSession(session.id)
                    )
                    if (recovered == null) {
                        clipRepository.deleteSessionClips(session.id)
                        sleepSessionRepository.deleteNight(session.id)
                    } else {
                        sleepSessionRepository.completeSession(
                            sessionId = session.id,
                            endTime = recovered.endTime,
                            totalSnoreDurationSeconds = recovered.totalSnoreDurationSeconds,
                            snoreIntensityScore = recovered.snoreIntensityScore,
                            scoreVersion = SnoreScore.VERSION
                        )
                    }
                } catch (e: Exception) {
                    Log.e("SleepLabApp", "Could not recover night ${session.id}", e)
                }
            }
    }

    companion object {
        lateinit var instance: SleepLabApp
            private set
    }
}
