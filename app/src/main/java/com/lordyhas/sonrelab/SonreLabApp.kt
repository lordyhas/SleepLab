package com.lordyhas.sonrelab

import android.app.Application
import com.lordyhas.sonrelab.data.local.AppDatabase
import com.lordyhas.sonrelab.data.repository.SleepSessionRepository
import com.lordyhas.sonrelab.data.repository.SnoreEventRepository
import com.lordyhas.sonrelab.data.repository.TreatmentRepository

class SonreLabApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val treatmentRepository by lazy { TreatmentRepository(database.treatmentDao()) }
    val sleepSessionRepository by lazy { SleepSessionRepository(database.sleepSessionDao()) }
    val snoreEventRepository by lazy { SnoreEventRepository(database.snoreEventDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: SonreLabApp
            private set
    }
}
