package com.lordyhas.sleeplab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lordyhas.sleeplab.MainActivity
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.ui.util.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SleepTrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var timerJob: Job? = null
    private var noiseSamplingJob: Job? = null
    private var audioAnalyzer: AudioAnalyzer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var currentSessionId: Int? = null
    private var currentTreatmentId: Int? = null
    private var startTimeMillis: Long = 0L

    companion object {
        const val CHANNEL_ID = "snore_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.lordyhas.sleeplab.action.START_TRACKING"
        const val ACTION_STOP = "com.lordyhas.sleeplab.action.STOP_TRACKING"
        const val EXTRA_TREATMENT_ID = "extra_treatment_id"
        const val EXTRA_THRESHOLD_DB = "extra_threshold_db"

        private val _trackingState = MutableStateFlow(TrackingState())
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        fun startService(context: Context, treatmentId: Int? = null, thresholdDb: Float = 50f) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                action = ACTION_START
                if (treatmentId != null) {
                    putExtra(EXTRA_TREATMENT_ID, treatmentId)
                }
                putExtra(EXTRA_THRESHOLD_DB, thresholdDb)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val treatmentId = if (intent.hasExtra(EXTRA_TREATMENT_ID)) {
                    intent.getIntExtra(EXTRA_TREATMENT_ID, -1).takeIf { it != -1 }
                } else null
                val thresholdDb = intent.getFloatExtra(EXTRA_THRESHOLD_DB, 50f)
                startTracking(treatmentId, thresholdDb)
            }
            ACTION_STOP -> {
                stopTracking()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTracking(treatmentId: Int?, thresholdDb: Float) {
        if (_trackingState.value.isTracking) return

        // Defensive guard: RECORD_AUDIO must be granted at runtime before starting
        // a foreground service with type=microphone (Android 14+ requirement).
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return
        }

        startTimeMillis = System.currentTimeMillis()
        currentTreatmentId = treatmentId

        // Acquire WakeLock
        acquireWakeLock()

        // Start Foreground
        val notification = buildNotification(0L, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Create Room Session
        serviceScope.launch(Dispatchers.IO) {
            val app = SleepLabApp.instance
            val sessionId = app.sleepSessionRepository.startNewSession(treatmentId).toInt()
            currentSessionId = sessionId

            _trackingState.update {
                it.copy(
                    isTracking = true,
                    sessionId = sessionId,
                    treatmentId = treatmentId,
                    startTime = startTimeMillis,
                    elapsedSeconds = 0L,
                    currentDb = 0f,
                    snoreCount = 0,
                    totalSnoreSeconds = 0,
                    maxDb = 0f,
                    recentDbHistory = emptyList()
                )
            }

            // Start Audio Analyzer
            audioAnalyzer = AudioAnalyzer(
                scope = serviceScope,
                thresholdDb = thresholdDb,
                onDecibelUpdate = { db ->
                    handleDecibelUpdate(db)
                },
                onSnoreDetected = { amplitudeDb, durationSec ->
                    handleSnoreDetected(sessionId, amplitudeDb, durationSec)
                }
            ).also { it.start() }

            startNoiseSampling(sessionId)

            // Timer for elapsed seconds
            startElapsedTimer()
        }
    }

    private fun startNoiseSampling(sessionId: Int) {
        noiseSamplingJob?.cancel()
        noiseSamplingJob = serviceScope.launch(Dispatchers.IO) {
            val app = SleepLabApp.instance
            while (isActive) {
                delay(10_000L) // 10 seconds
                val currentDb = _trackingState.value.currentDb
                app.noiseSampleRepository.recordSample(
                    sessionId = sessionId,
                    decibelLevel = currentDb
                )
            }
        }
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val elapsed = (System.currentTimeMillis() - startTimeMillis) / 1000
                _trackingState.update { it.copy(elapsedSeconds = elapsed) }
                updateNotification(elapsed, _trackingState.value.snoreCount)
            }
        }
    }

    private fun handleDecibelUpdate(db: Float) {
        _trackingState.update { state ->
            val updatedHistory = (state.recentDbHistory + db).takeLast(40)
            val updatedMax = if (db > state.maxDb) db else state.maxDb
            state.copy(
                currentDb = db,
                maxDb = updatedMax,
                recentDbHistory = updatedHistory
            )
        }
    }

    private fun handleSnoreDetected(sessionId: Int, amplitudeDb: Float, durationSec: Int) {
        serviceScope.launch(Dispatchers.IO) {
            val app = SleepLabApp.instance
            app.snoreEventRepository.recordSnoreEvent(
                sessionId = sessionId,
                amplitude = amplitudeDb,
                durationSeconds = durationSec
            )

            _trackingState.update { state ->
                state.copy(
                    snoreCount = state.snoreCount + 1,
                    totalSnoreSeconds = state.totalSnoreSeconds + durationSec
                )
            }
        }
    }

    private fun stopTracking() {
        val sessionId = currentSessionId
        val totalSnoreSeconds = _trackingState.value.totalSnoreSeconds
        val maxDb = _trackingState.value.maxDb

        noiseSamplingJob?.cancel()
        noiseSamplingJob = null
        audioAnalyzer?.stop()
        audioAnalyzer = null
        timerJob?.cancel()
        timerJob = null

        serviceScope.launch(Dispatchers.IO) {
            if (sessionId != null) {
                val app = SleepLabApp.instance
                val avgDb = app.snoreEventRepository.getAverageAmplitudeForSession(sessionId) ?: 0f
                val intensityScore = if (avgDb > 0) ((avgDb + maxDb) / 2f).coerceIn(0f, 100f) else 0f

                app.sleepSessionRepository.completeSession(
                    sessionId = sessionId,
                    endTime = System.currentTimeMillis(),
                    totalSnoreDurationSeconds = totalSnoreSeconds,
                    snoreIntensityScore = intensityScore
                )
            }

            _trackingState.update {
                it.copy(
                    isTracking = false,
                    currentDb = 0f
                )
            }

            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(elapsedSeconds: Long, snoreCount: Int): Notification {
        val timeString = formatClock(elapsedSeconds)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SleepTrackingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text, timeString, snoreCount))
            .setSmallIcon(R.drawable.ic_night_recording)
            .setOngoing(true)
            .setContentIntent(pendingOpenIntent)
            .addAction(android.R.drawable.ic_media_pause, getString(R.string.notif_stop), pendingStopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun updateNotification(elapsedSeconds: Long, snoreCount: Int) {
        val notification = buildNotification(elapsedSeconds, snoreCount)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "SleepLab:SleepTrackingWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 60 * 1000L) // Max 10 hours safety
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            wakeLock = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        noiseSamplingJob?.cancel()
        audioAnalyzer?.stop()
        audioAnalyzer = null
        timerJob?.cancel()
        releaseWakeLock()
        serviceScope.cancel()
    }
}

data class TrackingState(
    val isTracking: Boolean = false,
    val sessionId: Int? = null,
    val treatmentId: Int? = null,
    val startTime: Long = 0L,
    val elapsedSeconds: Long = 0L,
    val currentDb: Float = 0f,
    val snoreCount: Int = 0,
    val totalSnoreSeconds: Int = 0,
    val maxDb: Float = 0f,
    val recentDbHistory: List<Float> = emptyList()
)
