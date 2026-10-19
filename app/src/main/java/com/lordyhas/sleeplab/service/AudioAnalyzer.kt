package com.lordyhas.sleeplab.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.lordyhas.sleeplab.service.clips.PcmRingBuffer
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
    private val startDelayMs: Long = 0L
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

    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_MS = 100L
        private const val FRAME_SAMPLES = (SAMPLE_RATE * FRAME_MS / 1000).toInt()

        /** Long enough to reach back to the first of the 3 bursts that confirm a rhythm. */
        const val CLIP_BUFFER_SECONDS = 25
        private const val CLIP_PRE_ROLL_MS = 1_000L
        private const val CLIP_POST_ROLL_MS = 1_500L
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
        val detectionStartsAt = streamStart + startDelayMs
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
            ring?.let { buffer ->
                buffer.write(frame, 0, FRAME_SAMPLES)
                deliverReadyClips(buffer)
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

            onFrame(features.db, detector.classify(features))

            adaptiveFloor?.let { floor ->
                if (floor.onFrame(features.db, FRAME_MS)) {
                    detector.thresholdDb = floor.thresholdDb
                    onThresholdAdapted(floor.thresholdDb, floor.floorDb)
                }
            }

            detector.onFrame(timestamp, FRAME_MS, features).forEach { burst ->
                onSnoreDetected(burst, detector.thresholdDb)
                if (ring != null) {
                    fun sampleAt(timeMs: Long) = (timeMs - streamStart) * SAMPLE_RATE / 1000
                    clipRequests += ClipRequest(
                        burst = burst,
                        fromSample = sampleAt(burst.startMs - CLIP_PRE_ROLL_MS).coerceAtLeast(0),
                        toSample = sampleAt(burst.startMs + burst.durationMs + CLIP_POST_ROLL_MS)
                    )
                }
            }
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
