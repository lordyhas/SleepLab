package com.lordyhas.sleeplab.ui.util

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.data.local.entity.SleepSessionEntity
import com.lordyhas.sleeplab.service.detection.SnoreScore
import com.lordyhas.sleeplab.ui.theme.SnoreHigh
import com.lordyhas.sleeplab.ui.theme.SnoreLow
import com.lordyhas.sleeplab.ui.theme.SnoreMedium

/** How to present a night's score; depends on which formula produced it. */
data class NightScoreDisplay(
    val value: Float,
    @param:StringRes val labelRes: Int,
    @param:StringRes val levelRes: Int,
    val color: Color,
    /** True for nights scored with the old dB-average formula. */
    val isLegacy: Boolean
)

fun SleepSessionEntity.scoreDisplay(): NightScoreDisplay {
    val score = snoreIntensityScore
    return if (scoreVersion >= SnoreScore.VERSION) {
        // Time-weighted score: 0 = no snoring, 100 = a quarter of the night or more.
        val (level, color) = when {
            score < 20f -> R.string.night_quiet to SnoreLow
            score < 50f -> R.string.night_moderate to SnoreMedium
            else -> R.string.night_loud to SnoreHigh
        }
        NightScoreDisplay(score, R.string.snore_score_label, level, color, isLegacy = false)
    } else {
        // Legacy score was an average of dB values, so it uses the dB severity bands.
        val (level, color) = when {
            score < 45f -> R.string.night_quiet to SnoreLow
            score < 60f -> R.string.night_moderate to SnoreMedium
            else -> R.string.night_loud to SnoreHigh
        }
        NightScoreDisplay(score, R.string.intensity_label, level, color, isLegacy = true)
    }
}
