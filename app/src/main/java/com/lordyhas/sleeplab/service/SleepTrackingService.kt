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
import com.lordyhas.sleeplab.service.bedtime.BedtimeSchedule
import com.lordyhas.sleeplab.service.bedtime.SleepTrackingTileService
import com.lordyhas.sleeplab.service.clips.AacClipEncoder
import com.lordyhas.sleeplab.service.clips.ClipBudget
import com.lordyhas.sleeplab.service.detection.SnoreBurst
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.ConcurrentHashMap
import com.lordyhas.sleeplab.service.detection.SnoreScore
import com.lordyhas.sleeplab.service.detection.SoundClass
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
    private var stopAtMillis: Long = Long.MAX_VALUE

    /** Clip saving runs one at a time, so evictions and saves never race. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val clipDispatcher = Dispatchers.IO.limitedParallelism(1)
    private var clipBudget = ClipBudget()
    /** Database id of each snore of the night, keyed by burst start (inserted asynchronously). */
    private val eventIds = ConcurrentHashMap<Long, CompletableDeferred<Int>>()
    /** Saved clip file per snore, keyed by burst start. */
    private val clipPaths = HashMap<Long, String>()

    /** Detected snores of the current night, for the score computed when it ends. */
    private val scoreEpisodes = mutableListOf<SnoreScore.Episode>()
    /** Averages frame levels between two stored noise samples. */
    private val sampleAccumulator = LevelAccumulator()

    companion object {
        const val CHANNEL_ID = "snore_tracking_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.lordyhas.sleeplab.action.START_TRACKING"
        const val ACTION_STOP = "com.lordyhas.sleeplab.action.STOP_TRACKING"
        const val EXTRA_TREATMENT_ID = "extra_treatment_id"
        const val EXTRA_THRESHOLD_DB = "extra_threshold_db"
        const val EXTRA_AUTO_CALIBRATE = "extra_auto_calibrate"

        private val _trackingState = MutableStateFlow(TrackingState())
        val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        /**
         * @param thresholdDb manual threshold; with [autoCalibrate] it is only used until the
         *   room's noise floor has been measured.
         */
        fun startService(
            context: Context,
            treatmentId: Int? = null,
            thresholdDb: Float = 50f,
            autoCalibrate: Boolean = false
        ) {
            val intent = Intent(context, SleepTrackingService::class.java).apply {
                action = ACTION_START
                if (treatmentId != null) {
                    putExtra(EXTRA_TREATMENT_ID, treatmentId)
                }
                putExtra(EXTRA_THRESHOLD_DB, thresholdDb)
                putExtra(EXTRA_AUTO_CALIBRATE, autoCalibrate)
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
                val autoCalibrate = intent.getBooleanExtra(EXTRA_AUTO_CALIBRATE, false)
                startTracking(treatmentId, thresholdDb, autoCalibrate)
            }
            ACTION_STOP -> {
                stopTracking()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTracking(treatmentId: Int?, thresholdDb: Float, autoCalibrate: Boolean) {
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
        synchronized(scoreEpisodes) { scoreEpisodes.clear() }
        clipBudget = ClipBudget()
        eventIds.clear()
        clipPaths.clear()
        val settings = SleepLabApp.instance.settingsRepository
        val captureClips = settings.clipsEnabled.value
        val startDelayMs = settings.startDelayMinutes.value * 60_000L
        stopAtMillis = BedtimeSchedule.stopAt(
            startMillis = startTimeMillis,
            autoStopEnabled = settings.autoStopEnabled.value,
            autoStopMinuteOfDay = settings.autoStopMinuteOfDay.value
        )
        sampleAccumulator.drainAverage()

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
            val sessionId = app.sleepSessionRepository.startNewSession(treatmentId, thresholdDb).toInt()
            currentSessionId = sessionId

            // Tonight's factors belong to this night; the picker starts empty next time.
            val factorIds = app.settingsRepository.tonightFactorIds.value
            if (factorIds.isNotEmpty()) {
                app.factorRepository.setSessionFactors(sessionId, factorIds)
                app.settingsRepository.setTonightFactorIds(emptySet())
            }

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
                    recentDbHistory = emptyList(),
                    thresholdDb = thresholdDb,
                    noiseFloorDb = null,
                    isCalibrating = autoCalibrate,
                    calibrationProgress = 0f,
                    soundClass = SoundClass.QUIET,
                    lastSnoreAt = 0L,
                    clipsEnabled = captureClips,
                    detectionStartsAt = startTimeMillis + startDelayMs,
                    stopAt = stopAtMillis
                )
            }

            // Start Audio Analyzer
            audioAnalyzer = AudioAnalyzer(
                scope = serviceScope,
                thresholdDb = thresholdDb,
                calibrator = if (autoCalibrate) NoiseFloorCalibrator() else null,
                onFrame = { db, soundClass ->
                    handleFrame(db, soundClass)
                },
                onSnoreDetected = { burst, thresholdAtDetection ->
                    handleSnoreDetected(sessionId, burst, thresholdAtDetection)
                },
                onThresholdAdapted = { newThreshold, newFloor ->
                    _trackingState.update { it.copy(thresholdDb = newThreshold, noiseFloorDb = newFloor) }
                },
                captureClips = captureClips,
                startDelayMs = startDelayMs,
                onClipAudio = { burst, samples, sampleRate ->
                    handleClipAudio(sessionId, burst, samples, sampleRate)
                },
                onCalibrationProgress = { progress ->
                    _trackingState.update { it.copy(calibrationProgress = progress) }
                },
                onCalibrated = { result ->
                    handleCalibrated(sessionId, result)
                }
            ).also { it.start() }

            startNoiseSampling(sessionId)
            SleepTrackingTileService.refresh(this@SleepTrackingService)

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
                val averageDb = sampleAccumulator.drainAverage() ?: _trackingState.value.currentDb
                app.noiseSampleRepository.recordSample(
                    sessionId = sessionId,
                    decibelLevel = averageDb,
                    thresholdDb = _trackingState.value.thresholdDb
                )
            }
        }
    }

    private fun startElapsedTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                val now = System.currentTimeMillis()
                if (now >= stopAtMillis) {
                    stopTracking() // auto-stop time or the 12 h cap
                    break
                }
                val elapsed = (now - startTimeMillis) / 1000
                _trackingState.update { it.copy(elapsedSeconds = elapsed) }
                updateNotification(elapsed, _trackingState.value.snoreCount)
            }
        }
    }

    private fun handleFrame(db: Float, soundClass: SoundClass) {
        sampleAccumulator.add(db)
        _trackingState.update { state ->
            val updatedHistory = (state.recentDbHistory + db).takeLast(TrackingState.HISTORY_FRAMES)
            val updatedMax = if (db > state.maxDb) db else state.maxDb
            state.copy(
                currentDb = db,
                maxDb = updatedMax,
                recentDbHistory = updatedHistory,
                soundClass = soundClass
            )
        }
    }

    private fun handleSnoreDetected(sessionId: Int, burst: SnoreBurst, thresholdAtDetection: Float) {
        val durationSec = burst.durationSeconds
        synchronized(scoreEpisodes) {
            scoreEpisodes += SnoreScore.Episode(durationSec, burst.peakDb - thresholdAtDetection)
        }
        _trackingState.update { state ->
            state.copy(
                snoreCount = state.snoreCount + 1,
                totalSnoreSeconds = state.totalSnoreSeconds + durationSec,
                lastSnoreAt = burst.startMs + burst.durationMs
            )
        }
        val id = CompletableDeferred<Int>()
        eventIds[burst.startMs] = id
        serviceScope.launch(Dispatchers.IO) {
            val eventId = SleepLabApp.instance.snoreEventRepository.recordSnoreEvent(
                sessionId = sessionId,
                amplitude = burst.peakDb,
                durationSeconds = durationSec,
                timestamp = burst.startMs
            )
            id.complete(eventId.toInt())
        }
    }

    private fun handleClipAudio(sessionId: Int, burst: SnoreBurst, samples: ShortArray, sampleRate: Int) {
        serviceScope.launch(clipDispatcher) {
            val decision = clipBudget.offer(burst.startMs, burst.peakDb)
            if (decision !is ClipBudget.Decision.Accept) return@launch
            val clips = SleepLabApp.instance.clipRepository

            decision.evictKeys.forEach { key ->
                val evictedId = eventIds[key]?.await() ?: return@forEach
                clips.detach(evictedId, clipPaths.remove(key))
            }

            val file = clips.newClipFile(sessionId, burst.startMs)
            if (AacClipEncoder.encode(samples, sampleRate, file)) {
                val eventId = eventIds[burst.startMs]?.await() ?: return@launch
                clips.attach(eventId, file)
                clipPaths[burst.startMs] = file.absolutePath
            }
        }
    }

    private fun handleCalibrated(sessionId: Int, result: CalibrationResult) {
        _trackingState.update {
            it.copy(
                isCalibrating = false,
                calibrationProgress = 1f,
                noiseFloorDb = result.noiseFloorDb,
                thresholdDb = result.thresholdDb
            )
        }
        serviceScope.launch(Dispatchers.IO) {
            SleepLabApp.instance.sleepSessionRepository.saveCalibration(
                sessionId = sessionId,
                noiseFloorDb = result.noiseFloorDb,
                thresholdDb = result.thresholdDb
            )
        }
    }

    private fun stopTracking() {
        // Auto-stop and a manual stop can race; the first one wins.
        if (timerJob == null && audioAnalyzer == null) return
        val sessionId = currentSessionId
        val totalSnoreSeconds = _trackingState.value.totalSnoreSeconds

        noiseSamplingJob?.cancel()
        noiseSamplingJob = null
        audioAnalyzer?.stop()
        audioAnalyzer = null
        timerJob?.cancel()
        timerJob = null

        serviceScope.launch(Dispatchers.IO) {
            if (sessionId != null) {
                val app = SleepLabApp.instance
                val endTime = System.currentTimeMillis()
                val episodes = synchronized(scoreEpisodes) { scoreEpisodes.toList() }
                val score = SnoreScore.compute(episodes, (endTime - startTimeMillis) / 1000)

                app.sleepSessionRepository.completeSession(
                    sessionId = sessionId,
                    endTime = endTime,
                    totalSnoreDurationSeconds = totalSnoreSeconds,
                    snoreIntensityScore = score,
                    scoreVersion = SnoreScore.VERSION
                )
            }

            _trackingState.update {
                it.copy(
                    isTracking = false,
                    isCalibrating = false,
                    currentDb = 0f
                )
            }

            SleepLabApp.instance.clipRepository.purgeExpired()

            SleepTrackingTileService.refresh(this@SleepTrackingService)
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
                // Safety timeout just past the longest possible night
                acquire(BedtimeSchedule.MAX_SESSION_MS + 10 * 60 * 1000L)
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
    val recentDbHistory: List<Float> = emptyList(),
    /** Threshold in use right now (manual, or the calibrated one once measured). */
    val thresholdDb: Float = 50f,
    val noiseFloorDb: Float? = null,
    val isCalibrating: Boolean = false,
    val calibrationProgress: Float = 0f,
    /** What the latest frame sounds like (quiet / other noise / snore-like). */
    val soundClass: SoundClass = SoundClass.QUIET,
    /** End time of the last confirmed snore, 0 if none yet. */
    val lastSnoreAt: Long = 0L,
    /** Whether snore clips are being saved tonight. */
    val clipsEnabled: Boolean = false,
    /** When calibration/detection begin (after the start delay). */
    val detectionStartsAt: Long = 0L,
    /** When this night will stop automatically (auto-stop time or the 12 h cap). */
    val stopAt: Long = 0L
) {
    companion object {
        /** Live waveform length: 60 frames of 100 ms. */
        const val HISTORY_FRAMES = 60
    }
}

/** Thread-safe running mean of frame levels. */
private class LevelAccumulator {
    private var sum = 0.0
    private var count = 0

    @Synchronized
    fun add(db: Float) {
        sum += db
        count++
    }

    /** Mean since the last call, or null if nothing was added; resets. */
    @Synchronized
    fun drainAverage(): Float? {
        val mean = if (count > 0) (sum / count).toFloat() else null
        sum = 0.0
        count = 0
        return mean
    }
}
