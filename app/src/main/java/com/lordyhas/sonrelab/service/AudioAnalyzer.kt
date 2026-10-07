package com.lordyhas.sonrelab.service

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AudioAnalyzer(
    private val scope: CoroutineScope,
    private var thresholdDb: Float = 50f,
    private val onDecibelUpdate: (currentDb: Float) -> Unit,
    private val onSnoreDetected: (amplitudeDb: Float, durationSeconds: Int) -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    @Volatile
    private var isRecording = false

    companion object {
        private const val TAG = "AudioAnalyzer"
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    fun updateThreshold(newThresholdDb: Float) {
        this.thresholdDb = newThresholdDb
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

        val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
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
                processAudioStream(bufferSize)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting AudioRecord", e)
            stop()
        }
    }

    private suspend fun processAudioStream(bufferSize: Int) {
        val audioBuffer = ShortArray(bufferSize)
        
        var consecutiveSnoreWindows = 0
        var snoreMaxDb = 0f
        val windowDurationMs = (bufferSize.toFloat() / SAMPLE_RATE * 1000).toLong().coerceAtLeast(100)

        while (isRecording && scope.isActive) {
            val record = audioRecord ?: break
            val readCount = record.read(audioBuffer, 0, bufferSize)

            if (readCount > 0) {
                // Calculate RMS
                var sumSquare = 0.0
                for (i in 0 until readCount) {
                    val sample = audioBuffer[i]
                    sumSquare += sample * sample
                }
                val rms = sqrt(sumSquare / readCount)

                // Convert RMS to estimated dB SPL (0 to ~95 dB)
                // 32767 is max amplitude for 16-bit PCM
                val db = if (rms > 1.0) {
                    val rawDb = 20.0 * log10(rms / 1.0) // 0 to ~90.3 dB
                    rawDb.toFloat().coerceIn(0f, 100f)
                } else {
                    0f
                }

                onDecibelUpdate(db)

                // Snore detection algorithm:
                // If dB is above threshold, we count consecutive active windows
                if (db >= thresholdDb) {
                    consecutiveSnoreWindows++
                    if (db > snoreMaxDb) {
                        snoreMaxDb = db
                    }
                } else {
                    if (consecutiveSnoreWindows > 0) {
                        val durationSeconds = ((consecutiveSnoreWindows * windowDurationMs) / 1000).toInt()
                        // Snore event is recognized if it lasts between 1s and 8s
                        if (durationSeconds >= 1) {
                            onSnoreDetected(snoreMaxDb, durationSeconds.coerceIn(1, 10))
                        }
                        consecutiveSnoreWindows = 0
                        snoreMaxDb = 0f
                    }
                }
            } else {
                delay(50)
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
