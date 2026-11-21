package com.lordyhas.sleeplab.service.detection

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Google's YAMNet sound classifier (AudioSet, 521 classes) running on the phone with LiteRT.
 *
 * Input: [WINDOW_SAMPLES] samples of 16 kHz mono audio scaled to [-1, 1] (0.975 s).
 * Output: one score per class, 0–1. The model ships in `assets/yamnet.tflite` (the MediaPipe
 * float32 build, labels embedded); class indices are those of `yamnet_class_map.csv`.
 *
 * Not thread-safe: call [classify] and [close] from one thread.
 */
class YamnetClassifier private constructor(private val interpreter: Interpreter) : Closeable {

    private val inputShape = interpreter.getInputTensor(0).shape()
    private val scores: Array<FloatArray>
    private val input: ByteBuffer = ByteBuffer.allocateDirect(WINDOW_SAMPLES * 4).order(ByteOrder.nativeOrder())

    init {
        // Some YAMNet builds take a flat [n] waveform of any length: pin it to one window.
        if (inputShape.size == 1 && inputShape[0] != WINDOW_SAMPLES) {
            interpreter.resizeInput(0, intArrayOf(WINDOW_SAMPLES))
        }
        interpreter.allocateTensors()
        val outShape = interpreter.getOutputTensor(0).shape()
        scores = Array(if (outShape.size == 2) outShape[0] else 1) { FloatArray(outShape.last()) }
    }

    /** Scores for one window of [WINDOW_SAMPLES] samples in [-1, 1]; the returned array is reused. */
    fun classify(window: FloatArray): FloatArray {
        require(window.size == WINDOW_SAMPLES)
        input.rewind()
        input.asFloatBuffer().put(window)
        input.rewind()
        val outShape = interpreter.getOutputTensor(0).shape()
        if (outShape.size == 2) {
            interpreter.run(input, scores)
            return scores[0]
        }
        val flat = scores[0]
        interpreter.run(input, flat)
        return flat
    }

    override fun close() = interpreter.close()

    companion object {
        private const val TAG = "YamnetClassifier"
        private const val MODEL_ASSET = "yamnet.tflite"

        const val SAMPLE_RATE = 16_000
        const val WINDOW_SAMPLES = 15_600 // 0.975 s

        const val CLASS_SPEECH = 0
        const val CLASS_BREATHING = 36
        const val CLASS_SNORING = 38
        const val CLASS_SNORT = 41
        const val CLASS_COUGH = 42

        /** Loads the model, or returns null (and logs why) so the caller can fall back to the rules. */
        fun create(context: Context): YamnetClassifier? = try {
            val bytes = context.assets.open(MODEL_ASSET).use { it.readBytes() }
            val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder()).put(bytes)
            model.rewind()
            YamnetClassifier(Interpreter(model, Interpreter.Options().setNumThreads(2)))
        } catch (e: Throwable) {
            Log.e(TAG, "Could not load YAMNet", e)
            null
        }
    }
}
