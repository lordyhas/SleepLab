package com.lordyhas.sleeplab.service.detection

import android.util.Log
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.util.Locale

/**
 * One CSV per night in `filesDir/detection_logs/`, to check the detection afterwards (and later
 * to build a dataset). No audio, only numbers. Lines, by first field:
 *
 * - `#` header: detector, threshold
 * - `W,start,peakDb,snoring,snort,breathing,cough,speech,topClass,topScore,inferMs` — one YAMNet window
 * - `Y,start,durationMs,peakDb` — a burst found by YAMNet
 * - `R,start,durationMs,peakDb` — a burst the rules detector would have reported
 *
 * Times are epoch milliseconds. Excluded from backup like the clips.
 */
class DetectionLog(file: File) : Closeable {
    private val writer: BufferedWriter? = try {
        file.parentFile?.mkdirs()
        file.bufferedWriter()
    } catch (e: Exception) {
        Log.e(TAG, "Could not open the detection log", e)
        null
    }
    private var lines = 0

    @Synchronized
    fun line(text: String) {
        try {
            writer?.apply {
                write(text)
                newLine()
                // About once a minute, so a night cut short loses little
                if (++lines % FLUSH_EVERY_LINES == 0) flush()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not write the detection log", e)
        }
    }

    fun header(detector: String, thresholdDb: Float) =
        line("# detector=$detector threshold=${fmt(thresholdDb)}")

    fun window(start: Long, peakDb: Float, scores: FloatArray, inferMs: Long) {
        var top = 0
        for (i in scores.indices) if (scores[i] > scores[top]) top = i
        line(
            "W,$start,${fmt(peakDb)}," +
                listOf(
                    YamnetClassifier.CLASS_SNORING, YamnetClassifier.CLASS_SNORT, YamnetClassifier.CLASS_BREATHING,
                    YamnetClassifier.CLASS_COUGH, YamnetClassifier.CLASS_SPEECH
                ).joinToString(",") { fmt(scores[it]) } +
                ",$top,${fmt(scores[top])},$inferMs"
        )
    }

    fun burst(kind: Char, burst: SnoreBurst) =
        line("$kind,${burst.startMs},${burst.durationMs},${fmt(burst.peakDb)}")

    @Synchronized
    override fun close() {
        try {
            writer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Could not close the detection log", e)
        }
    }

    private fun fmt(v: Float) = String.format(Locale.US, "%.3f", v)

    companion object {
        private const val TAG = "DetectionLog"
        const val DIR = "detection_logs"
        private const val FLUSH_EVERY_LINES = 120
    }
}
