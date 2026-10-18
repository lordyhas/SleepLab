package com.lordyhas.sleeplab

import android.app.Application
import com.lordyhas.sleeplab.data.local.AppDatabase
import com.lordyhas.sleeplab.data.repository.SleepSessionRepository
import com.lordyhas.sleeplab.data.repository.SnoreEventRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
import com.lordyhas.sleeplab.data.repository.NoiseSampleRepository
import com.lordyhas.sleeplab.data.repository.ClipRepository
import com.lordyhas.sleeplab.data.repository.SettingsRepository
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
    val clipRepository by lazy { ClipRepository(this, database.snoreEventDao(), settingsRepository) }

    /** App-lifetime scope for housekeeping that must outlive any screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch { clipRepository.purgeExpired() }
    }

    companion object {
        lateinit var instance: SleepLabApp
            private set
    }
}
