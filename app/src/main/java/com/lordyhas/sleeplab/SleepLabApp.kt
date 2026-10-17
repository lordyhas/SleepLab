package com.lordyhas.sleeplab

import android.app.Application
import com.lordyhas.sleeplab.data.local.AppDatabase
import com.lordyhas.sleeplab.data.repository.SleepSessionRepository
import com.lordyhas.sleeplab.data.repository.SnoreEventRepository
import com.lordyhas.sleeplab.data.repository.TreatmentRepository
import com.lordyhas.sleeplab.data.repository.NoiseSampleRepository
import com.lordyhas.sleeplab.data.repository.SettingsRepository

class SleepLabApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val treatmentRepository by lazy { TreatmentRepository(database.treatmentDao()) }
    val sleepSessionRepository by lazy { SleepSessionRepository(database.sleepSessionDao()) }
    val snoreEventRepository by lazy { SnoreEventRepository(database.snoreEventDao()) }
    val noiseSampleRepository by lazy { NoiseSampleRepository(database.noiseSampleDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SleepLabApp
            private set
    }
}
