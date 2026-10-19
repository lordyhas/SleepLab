package com.lordyhas.sleeplab.data.repository

import android.content.Context
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small persisted user preferences (SharedPreferences-backed) exposed as StateFlows.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _thresholdDb = MutableStateFlow(prefs.getFloat(KEY_THRESHOLD_DB, DEFAULT_THRESHOLD_DB))
    /** Sound level (dB) above which audio counts as snoring. */
    val thresholdDb: StateFlow<Float> = _thresholdDb.asStateFlow()

    private val _autoCalibrate = MutableStateFlow(prefs.getBoolean(KEY_AUTO_CALIBRATE, true))
    /** Measure the room's noise at the start of each night and derive the threshold from it. */
    val autoCalibrate: StateFlow<Boolean> = _autoCalibrate.asStateFlow()

    private val _clipsEnabled = MutableStateFlow(prefs.getBoolean(KEY_CLIPS_ENABLED, false))
    /** Save short audio clips of detected snores (opt-in). */
    val clipsEnabled: StateFlow<Boolean> = _clipsEnabled.asStateFlow()

    private val _clipRetentionDays = MutableStateFlow(prefs.getInt(KEY_CLIP_RETENTION_DAYS, DEFAULT_CLIP_RETENTION_DAYS))
    /** Clips older than this are deleted automatically; 0 = keep forever. */
    val clipRetentionDays: StateFlow<Int> = _clipRetentionDays.asStateFlow()

    private val _startDelayMinutes = MutableStateFlow(prefs.getInt(KEY_START_DELAY_MIN, 0))
    /** Minutes to wait after starting before calibration and detection begin (falling asleep). */
    val startDelayMinutes: StateFlow<Int> = _startDelayMinutes.asStateFlow()

    private val _autoStopEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_STOP_ENABLED, false))
    val autoStopEnabled: StateFlow<Boolean> = _autoStopEnabled.asStateFlow()

    private val _autoStopMinuteOfDay = MutableStateFlow(prefs.getInt(KEY_AUTO_STOP_MINUTE, DEFAULT_AUTO_STOP_MINUTE))
    /** Wake-up time (minutes after midnight) at which a night stops automatically. */
    val autoStopMinuteOfDay: StateFlow<Int> = _autoStopMinuteOfDay.asStateFlow()

    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDER_ENABLED, false))
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled.asStateFlow()

    private val _reminderMinuteOfDay = MutableStateFlow(prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE))
    /** Bedtime reminder time (minutes after midnight). */
    val reminderMinuteOfDay: StateFlow<Int> = _reminderMinuteOfDay.asStateFlow()

    private val _treatmentChoice = MutableStateFlow(prefs.getInt(KEY_TREATMENT_CHOICE, CHOICE_UNSET))
    /**
     * The treatment the user picked for tonight: a treatment id, [CHOICE_NONE] for an explicit
     * "no treatment", or [CHOICE_UNSET] when they never picked. Use [resolveTreatmentId].
     */
    val treatmentChoice: StateFlow<Int> = _treatmentChoice.asStateFlow()

    fun setThresholdDb(value: Float) {
        _thresholdDb.value = value
        prefs.edit().putFloat(KEY_THRESHOLD_DB, value).apply()
    }

    fun setAutoCalibrate(enabled: Boolean) {
        _autoCalibrate.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_CALIBRATE, enabled).apply()
    }

    fun setClipsEnabled(enabled: Boolean) {
        _clipsEnabled.value = enabled
        prefs.edit().putBoolean(KEY_CLIPS_ENABLED, enabled).apply()
    }

    fun setClipRetentionDays(days: Int) {
        _clipRetentionDays.value = days
        prefs.edit().putInt(KEY_CLIP_RETENTION_DAYS, days).apply()
    }

    fun setStartDelayMinutes(minutes: Int) {
        _startDelayMinutes.value = minutes
        prefs.edit().putInt(KEY_START_DELAY_MIN, minutes).apply()
    }

    fun setAutoStop(enabled: Boolean, minuteOfDay: Int = _autoStopMinuteOfDay.value) {
        _autoStopEnabled.value = enabled
        _autoStopMinuteOfDay.value = minuteOfDay
        prefs.edit().putBoolean(KEY_AUTO_STOP_ENABLED, enabled).putInt(KEY_AUTO_STOP_MINUTE, minuteOfDay).apply()
    }

    fun setReminder(enabled: Boolean, minuteOfDay: Int = _reminderMinuteOfDay.value) {
        _reminderEnabled.value = enabled
        _reminderMinuteOfDay.value = minuteOfDay
        prefs.edit().putBoolean(KEY_REMINDER_ENABLED, enabled).putInt(KEY_REMINDER_MINUTE, minuteOfDay).apply()
    }

    fun setTreatmentChoice(treatmentId: Int?) {
        val choice = treatmentId ?: CHOICE_NONE
        _treatmentChoice.value = choice
        prefs.edit().putInt(KEY_TREATMENT_CHOICE, choice).apply()
    }

    companion object {
        const val DEFAULT_THRESHOLD_DB = 50f
        const val DEFAULT_CLIP_RETENTION_DAYS = 14
        const val DEFAULT_AUTO_STOP_MINUTE = 7 * 60 + 30
        const val DEFAULT_REMINDER_MINUTE = 22 * 60 + 30
        const val CHOICE_UNSET = -2
        const val CHOICE_NONE = -1

        private const val PREFS_NAME = "sleeplab_settings"
        private const val KEY_THRESHOLD_DB = "threshold_db"
        private const val KEY_TREATMENT_CHOICE = "treatment_choice"
        private const val KEY_AUTO_CALIBRATE = "auto_calibrate"
        private const val KEY_CLIPS_ENABLED = "clips_enabled"
        private const val KEY_CLIP_RETENTION_DAYS = "clip_retention_days"
        private const val KEY_START_DELAY_MIN = "start_delay_minutes"
        private const val KEY_AUTO_STOP_ENABLED = "auto_stop_enabled"
        private const val KEY_AUTO_STOP_MINUTE = "auto_stop_minute"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"

        /**
         * Turns a stored choice into the treatment id to record with, falling back to the first
         * active treatment when nothing was picked or the picked one is no longer active.
         */
        fun resolveTreatmentId(choice: Int, activeTreatments: List<TreatmentEntity>): Int? = when (choice) {
            CHOICE_NONE -> null
            else -> activeTreatments.firstOrNull { it.id == choice }?.id
                ?: activeTreatments.firstOrNull()?.id
        }
    }
}
