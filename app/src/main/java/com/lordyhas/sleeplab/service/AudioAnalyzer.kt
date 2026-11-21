package com.lordyhas.sleeplab.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.lordyhas.sleeplab.service.clips.PcmRingBuffer
import com.lordyhas.sleeplab.service.detection.AdaptiveFloor
import com.lordyhas.sleeplab.service.detection.AudioFeatureExtractor
import com.lordyhas.sleeplab.service.detection.SnoreBurst
import com.lordyhas.sleeplab.service.detection.SnoreDetector
import com.lordyhas.sleeplab.service.detection.SoundClass
import com.lordyhas.sleeplab.service.detection.DetectionLog
import com.lordyhas.sleeplab.service.detection.YamnetClassifier
import com.lordyhas.sleeplab.service.detection.YamnetSnoreTracker
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Reads the microphone and turns it into snore events.
 *
 * Audio is analysed in fixed 100 ms frames: [AudioFeatureExtractor] measures level and low-band
 * ratio, [NoiseFloorCalibrator] (optional) measures the room first, and [AdaptiveFloor] keeps the
 * threshold following the room when calibrated.
 *
 * Snores come from one of two detectors:
 * - with [yamnet], every 0.5 s the last second of audio goes through the YAMNet classifier (on its
 *   own thread) and [YamnetSnoreTracker] turns its "Snoring" score into bursts. There is no level
 *   gate: the model hears everything. The rules detector still runs, but only for [detectionLog].
 * - without it, [SnoreDetector] finds rhythmic low-pitched bursts (the original rules).
 * Raw audio never leaves this class except as short clips around confirmed snores, and only when
 * [captureClips] is on (opt-in); nothing is sent anywhere.
 */
class AudioAnalyzer(
    private val scope: CoroutineScope,
    thresholdDb: Float = 50f,
    /** When set, detection waits until the room's noise floor is measured, then uses its threshold. */
    private val calibrator: NoiseFloorCalibrator? = null,
    private val onFrame: (db: Float, soundClass: SoundClass) -> Unit,
    /** A confirmed snore, with the threshold that was in force when it was detected. */
    private val onSnoreDetected: (burst: SnoreBurst, thresholdDb: Float) -> Unit,
    private val onCalibrationProgress: (progress: Float) -> Unit = {},
    private val onCalibrated: (CalibrationResult) -> Unit = {},
    private val onThresholdAdapted: (thresholdDb: Float, noiseFloorDb: Float) -> Unit = { _, _ -> },
    /** When true, the last [CLIP_BUFFER_SECONDS] of audio stay in memory to cut snore clips from. */
    private val captureClips: Boolean = false,
    /** Audio around a confirmed snore (pre-roll + snore + post-roll), mono PCM-16. */
    private val onClipAudio: (burst: SnoreBurst, samples: ShortArray, sampleRate: Int) -> Unit = { _, _, _ -> },
    /** Time to let the user fall asleep: calibration and detection only begin after it. */
    private val startDelayMs: Long = 0L,
    /** The microphone stopped delivering audio mid-night and could not be reopened; analysis has ended. */
    private val onMicrophoneLost: () -> Unit = {},
    /** When set, YAMNet decides what is snoring; the analyzer closes it on [stop]. */
    private val yamnet: YamnetClassifier? = null,
    /** Per-night numbers for checking the detection afterwards; closed on [stop]. */
    private val detectionLog: DetectionLog? = null
) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    @Volatile
    private var isRecording = false

    private val detector = SnoreDetector(thresholdDb)
    private var adaptiveFloor: AdaptiveFloor? = null

    private class ClipRequest(val burst: SnoreBurst, val fromSample: Long, val toSample: Long)
    private val ring = if (captureClips) PcmRingBuffer(SAMPLE_RATE * CLIP_BUFFER_SECONDS) else null
    private val clipRequests = mutableListOf<ClipRequest>()

    // YAMNet: the last second of audio as floats, classified on a single background thread so a
    // slow inference never makes the microphone overrun. Results come back in order through
    // [yamnetResults] and are handled on the audio thread.
    private class WindowResult(val hopStartMs: Long, val snoringScore: Float, val peakDb: Float)
    private val yamnetWindow = FloatArray(YamnetClassifier.WINDOW_SAMPLES)
    private var yamnetWritePos = 0
    private val yamnetResults = ConcurrentLinkedQueue<WindowResult>()
    private val yamnetTracker = YamnetSnoreTracker(hopMs = YAMNET_HOP_FRAMES * FRAME_MS)
    private val recentFrameDb = ArrayDeque<Float>()
    private val worker = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(8),
        ThreadPoolExecutor.DiscardOldestPolicy()
    )

    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_MS = 100L
        private const val FRAME_SAMPLES = (SAMPLE_RATE * FRAME_MS / 1000).toInt()

        /** Room for a [CLIP_LENGTH_MS] clip centred on a snore confirmed a few seconds late. */
        const val CLIP_BUFFER_SECONDS = 45
        /** Long enough to hear what was going on around the snore, not just one breath. */
        private const val CLIP_LENGTH_MS = 30_000L

        /** YAMNet runs every 5 frames (0.5 s) on the last 0.975 s of audio. */
        private const val YAMNET_HOP_FRAMES = 5
        private const val YAMNET_WINDOW_MS = 975L

        /** No audio at all for this long means the stream is dead, not just slow. */
        private const val STALL_TIMEOUT_MS = 5_000L
        private const val REOPEN_ATTEMPTS = 5
        /** Wait before each reopen attempt, growing linearly (2 s, 4 s … ≈ 30 s in total). */
        private const val REOPEN_DELAY_MS = 2_000L
    }

    /**
     * Opens the microphone and starts analysing.
     *
     * @return false when the microphone could not be opened; nothing is running then.
     */
    fun start(): Boolean {
        if (isRecording) return true

        val record = openRecord() ?: return false
        audioRecord = record
        isRecording = true
        recordingJob = scope.launch(Dispatchers.IO) {
            processAudioStream()
        }
        return true
    }

    /** A recording [AudioRecord], or null if the microphone is unavailable. */
    @SuppressLint("MissingPermission")
    private fun openRecord(): AudioRecord? {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "AudioRecord minBufferSize error")
            return null
        }

        // Internal buffer in bytes: room for several frames so reads never overrun.
        val bufferBytes = maxOf(minBufferSize * 2, FRAME_SAMPLES * 2 * 4)

        var record: AudioRecord? = null
        return try {
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferBytes
            )
            check(record.state == AudioRecord.STATE_INITIALIZED) { "AudioRecord could not be initialized" }
            record.startRecording()
            check(record.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "AudioRecord did not start" }
            record
        } catch (e: Exception) {
            Log.e(TAG, "Could not open the microphone", e)
            releaseQuietly(record)
            null
        }
    }

    private fun releaseQuietly(record: AudioRecord?) {
        try {
            record?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing AudioRecord", e)
        }
    }

    /**
     * The stream broke mid-night (read error, or no audio for [STALL_TIMEOUT_MS]): drop the dead
     * recorder and try to open a new one a few times before giving up.
     */
    private suspend fun reopenRecord(): AudioRecord? {
        releaseQuietly(audioRecord)
        audioRecord = null
        repeat(REOPEN_ATTEMPTS) { attempt ->
            delay(REOPEN_DELAY_MS * (attempt + 1))
            if (!isRecording) return null
            openRecord()?.let { record ->
                if (isRecording) {
                    audioRecord = record
                    return record
                }
                releaseQuietly(record) // stopped while reopening
                return null
            }
        }
        return null
    }

    private suspend fun processAudioStream() {
        val frame = ShortArray(FRAME_SAMPLES)
        val extractor = AudioFeatureExtractor(SAMPLE_RATE)
        val streamStart = System.currentTimeMillis()
        val detectionStartsAt = streamStart + startDelayMs
        var framesProcessed = 0L
        // Time lost while the microphone was being reopened: audio frames stay contiguous, the clock doesn't.
        var gapMs = 0L

        while (isRecording && scope.isActive) {
            val record = audioRecord ?: break
            // Blocking read until a whole frame is available
            var filled = 0
            var broken = false
            val stallDeadline = SystemClock.elapsedRealtime() + STALL_TIMEOUT_MS
            while (filled < FRAME_SAMPLES && isRecording) {
                val read = record.read(frame, filled, FRAME_SAMPLES - filled)
                when {
                    read > 0 -> filled += read
                    read < 0 || SystemClock.elapsedRealtime() > stallDeadline -> { broken = true; break }
                    else -> delay(20)
                }
            }
            if (!isRecording) break
            if (broken) {
                Log.w(TAG, "Microphone stream broke, reopening")
                if (reopenRecord() == null) {
                    if (isRecording) {
                        isRecording = false
                        onMicrophoneLost()
                    }
                    return
                }
                gapMs = (System.currentTimeMillis() - streamStart - framesProcessed * FRAME_MS).coerceAtLeast(gapMs)
                continue
            }

            val timestamp = streamStart + gapMs + framesProcessed * FRAME_MS
            framesProcessed++
            val features = extractor.extract(frame, 0, FRAME_SAMPLES)
            ring?.let { buffer ->
                buffer.write(frame, 0, FRAME_SAMPLES)
                deliverReadyClips(buffer)
            }
            if (yamnet != null) {
                for (i in 0 until FRAME_SAMPLES) {
                    yamnetWindow[yamnetWritePos] = frame[i] / 32768f
                    yamnetWritePos = (yamnetWritePos + 1) % yamnetWindow.size
                }
                recentFrameDb.addLast(features.db)
                while (recentFrameDb.size > 10) recentFrameDb.removeFirst()
            }

            if (timestamp < detectionStartsAt) {
                onFrame(features.db, SoundClass.QUIET) // live level only, nothing detected yet
                continue
            }

            if (calibrator != null && !calibrator.isComplete) {
                onFrame(features.db, SoundClass.QUIET)
                calibrator.add(features.db, FRAME_MS)
                onCalibrationProgress(calibrator.progress)
                if (calibrator.isComplete) {
                    calibrator.result()?.let { result ->
                        detector.thresholdDb = result.thresholdDb
                        adaptiveFloor = AdaptiveFloor(initialFloorDb = result.noiseFloorDb)
                        onCalibrated(result)
                    }
                }
                continue // no snore detection while measuring the room
            }

            if (yamnet != null && framesProcessed >= 10 && framesProcessed % YAMNET_HOP_FRAMES == 0L) {
                submitWindow(timestamp + FRAME_MS)
            }
            var result = yamnetResults.poll()
            while (result != null) {
                yamnetTracker.onWindow(result.hopStartMs, result.snoringScore, result.peakDb).forEach { burst ->
                    detectionLog?.burst('Y', burst)
                    reportSnore(burst, streamStart + gapMs)
                }
                result = yamnetResults.poll()
            }

            val rulesClass = detector.classify(features)
            onFrame(
                features.db,
                when {
                    yamnet == null -> rulesClass
                    yamnetTracker.isSnoring -> SoundClass.SNORE_LIKE
                    rulesClass == SoundClass.QUIET -> SoundClass.QUIET
                    else -> SoundClass.NOISE
                }
            )

            adaptiveFloor?.let { floor ->
                if (floor.onFrame(features.db, FRAME_MS)) {
                    detector.thresholdDb = floor.thresholdDb
                    onThresholdAdapted(floor.thresholdDb, floor.floorDb)
                }
            }

            detector.onFrame(timestamp, FRAME_MS, features).forEach { burst ->
                detectionLog?.burst('R', burst)
                if (yamnet == null) reportSnore(burst, streamStart + gapMs)
            }
        }
    }

    private fun reportSnore(burst: SnoreBurst, audioStart: Long) {
        onSnoreDetected(burst, detector.thresholdDb)
        if (ring != null) {
            fun sampleAt(timeMs: Long) = (timeMs - audioStart) * SAMPLE_RATE / 1000
            val centre = burst.startMs + burst.durationMs / 2
            clipRequests += ClipRequest(
                burst = burst,
                fromSample = sampleAt(centre - CLIP_LENGTH_MS / 2).coerceAtLeast(0),
                toSample = sampleAt(centre + CLIP_LENGTH_MS / 2)
            )
        }
    }

    /** Sends the last 0.975 s (ending at [windowEndMs]) to YAMNet on the worker thread. */
    private fun submitWindow(windowEndMs: Long) {
        val model = yamnet ?: return
        // Unroll the circular buffer so the window is in time order.
        val window = FloatArray(yamnetWindow.size) { i -> yamnetWindow[(yamnetWritePos + i) % yamnetWindow.size] }
        val peakDb = recentFrameDb.maxOrNull() ?: 0f
        val windowStart = windowEndMs - YAMNET_WINDOW_MS
        val hopMs = YAMNET_HOP_FRAMES * FRAME_MS
        // Each window accounts for the hop at its centre, so overlapping windows don't double count.
        val hopStart = windowStart + YAMNET_WINDOW_MS / 2 - hopMs / 2
        try {
            worker.execute {
                if (!isRecording) return@execute
                val began = SystemClock.elapsedRealtime()
                val scores = try {
                    model.classify(window)
                } catch (e: Exception) {
                    Log.e(TAG, "YAMNet inference failed", e)
                    return@execute
                }
                detectionLog?.window(windowStart, peakDb, scores, SystemClock.elapsedRealtime() - began)
                yamnetResults.add(WindowResult(hopStart, scores[YamnetClassifier.CLASS_SNORING], peakDb))
            }
        } catch (_: RejectedExecutionException) {
            // Stopping
        }
    }

    /** Hands out clips whose post-roll has now been recorded. */
    private fun deliverReadyClips(buffer: PcmRingBuffer) {
        val iterator = clipRequests.iterator()
        while (iterator.hasNext()) {
            val request = iterator.next()
            if (buffer.writtenSamples < request.toSample) continue
            iterator.remove()
            buffer.read(request.fromSample, request.toSample)?.let { samples ->
                onClipAudio(request.burst, samples, SAMPLE_RATE)
            }
        }
    }

    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        // Close the model and the log on the worker, after any inference still running.
        try {
            worker.execute {
                yamnet?.close()
                detectionLog?.close()
            }
        } catch (_: RejectedExecutionException) {
            // Already stopped
        }
        worker.shutdown()

        try {
            audioRecord?.apply {
                if (state == AudioRecord.STATE_INITIALIZED && recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }
}
