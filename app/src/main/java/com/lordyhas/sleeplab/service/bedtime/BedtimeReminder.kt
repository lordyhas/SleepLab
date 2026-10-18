package com.lordyhas.sleeplab.service.bedtime

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lordyhas.sleeplab.LaunchAction
import com.lordyhas.sleeplab.MainActivity
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.service.SleepTrackingService

/**
 * Daily bedtime reminder. Uses an inexact alarm window (no exact-alarm permission needed, and
 * the system may batch it to save battery); each delivery schedules the next day.
 */
object BedtimeReminder {
    private const val CHANNEL_ID = "bedtime_reminder_channel"
    private const val NOTIFICATION_ID = 2001
    private const val REQUEST_CODE = 2001
    private const val WINDOW_MS = 15L * 60 * 1000

    /** Schedules (or cancels) the next reminder according to the current settings. */
    fun sync(context: Context) {
        val settings = SleepLabApp.instance.settingsRepository
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = alarmIntent(context)
        alarmManager.cancel(pending)
        if (!settings.reminderEnabled.value) return
        val next = BedtimeSchedule.nextOccurrence(System.currentTimeMillis(), settings.reminderMinuteOfDay.value)
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, next, WINDOW_MS, pending)
    }

    private fun alarmIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, BedtimeReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    internal fun show(context: Context) {
        // Already tracking: nothing to remind.
        if (SleepTrackingService.trackingState.value.isTracking) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)

        fun open(action: LaunchAction, requestCode: Int) = PendingIntent.getActivity(
            context,
            requestCode,
            MainActivity.launchIntent(context, action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_night_recording)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(context.getString(R.string.reminder_text))
            .setContentIntent(open(LaunchAction.OPEN_TRACKING, 1))
            .addAction(0, context.getString(R.string.start_night_tracking), open(LaunchAction.START_TRACKING, 2))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notification permission revoked between the check and the post.
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}

/** Fires at bedtime: shows the reminder, then schedules tomorrow's. */
class BedtimeReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BedtimeReminder.show(context)
        BedtimeReminder.sync(context)
    }
}

/** Alarms are cleared on reboot and drift on clock/time-zone changes: reschedule. */
class BedtimeRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> BedtimeReminder.sync(context)
        }
    }
}
