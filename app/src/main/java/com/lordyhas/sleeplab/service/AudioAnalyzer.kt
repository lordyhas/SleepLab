package com.lordyhas.sleeplab.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.lordyhas.sleeplab.service.detection.AdaptiveFloor
import com.lordyhas.sleeplab.service.detection.AudioFeatureExtractor
import com.lordyhas.sleeplab.service.detection.SnoreBurst
import com.lordyhas.sleeplab.service.detection.SnoreDetector
import com.lordyhas.sleeplab.service.detection.SoundClass
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
 * ratio, [NoiseFloorCalibrator] (optional) measures the room first, [SnoreDetector] finds rhythmic
 * low-pitched bursts, and [AdaptiveFloor] keeps the threshold following the room when calibrated.
 * Nothing is stored or sent: only the derived numbers leave this class.
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
    private val onThresholdAdapted: (thresholdDb: Float, noiseFloorDb: Float) -> Unit = { _, _ -> }
) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    @Volatile
    private var isRecording = false

    private val detector = SnoreDetector(thresholdDb)
    private var adaptiveFloor: AdaptiveFloor? = null

    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_MS = 100L
        private const val FRAME_SAMPLES = (SAMPLE_RATE * FRAME_MS / 1000).toInt()
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "AudioRecord minBufferSize error")
            return
        }

        // Internal buffer in bytes: room for several frames so reads never overrun.
        val bufferBytes = maxOf(minBufferSize * 2, FRAME_SAMPLES * 2 * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferBytes
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord could not be initialized")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.IO) {
                processAudioStream()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioRecord", e)
            stop()
        }
    }

    private suspend fun processAudioStream() {
        val frame = ShortArray(FRAME_SAMPLES)
        val extractor = AudioFeatureExtractor(SAMPLE_RATE)
        val streamStart = System.currentTimeMillis()
        var framesProcessed = 0L

        while (isRecording && scope.isActive) {
            val record = audioRecord ?: break
            // Blocking read until a whole frame is available
            var filled = 0
            while (filled < FRAME_SAMPLES && isRecording) {
                val read = record.read(frame, filled, FRAME_SAMPLES - filled)
                if (read > 0) filled += read else { delay(20); if (read < 0) break }
            }
            if (filled < FRAME_SAMPLES) continue

            val timestamp = streamStart + framesProcessed * FRAME_MS
            framesProcessed++
            val features = extractor.extract(frame, 0, FRAME_SAMPLES)

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

            onFrame(features.db, detector.classify(features))

            adaptiveFloor?.let { floor ->
                if (floor.onFrame(features.db, FRAME_MS)) {
                    detector.thresholdDb = floor.thresholdDb
                    onThresholdAdapted(floor.thresholdDb, floor.floorDb)
                }
            }

            detector.onFrame(timestamp, FRAME_MS, features).forEach { burst ->
                onSnoreDetected(burst, detector.thresholdDb)
            }
        }
    }

    fun stop() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

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
