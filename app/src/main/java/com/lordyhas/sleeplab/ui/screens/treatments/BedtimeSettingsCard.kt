package com.lordyhas.sleeplab.ui.screens.treatments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.IconBadge
import com.lordyhas.sleeplab.ui.theme.MoonGlow
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.util.formatMinuteOfDay

private val StartDelayOptions = listOf(0, 10, 15, 30)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BedtimeSettingsCard(
    startDelayMinutes: Int,
    autoStopEnabled: Boolean,
    autoStopMinuteOfDay: Int,
    reminderEnabled: Boolean,
    reminderMinuteOfDay: Int,
    onStartDelayChange: (Int) -> Unit,
    onAutoStopChange: (enabled: Boolean, minuteOfDay: Int) -> Unit,
    onReminderChange: (enabled: Boolean, minuteOfDay: Int) -> Unit
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<TimeTarget?>(null) }
    var notificationsDenied by remember { mutableStateOf(false) }
    // Reminder time waiting for the notification permission answer
    var pendingReminderMinute by remember { mutableStateOf(reminderMinuteOfDay) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsDenied = !granted
        if (granted) onReminderChange(true, pendingReminderMinute)
    }

    fun enableReminder(minuteOfDay: Int) {
        pendingReminderMinute = minuteOfDay
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminderChange(true, minuteOfDay)
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        // Start delay
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.HourglassBottom, tint = MoonGlow)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(stringResource(R.string.start_delay_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.start_delay_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StartDelayOptions.forEach { minutes ->
                FilterChip(
                    selected = startDelayMinutes == minutes,
                    onClick = { onStartDelayChange(minutes) },
                    label = {
                        Text(
                            if (minutes == 0) stringResource(R.string.start_delay_off)
                            else stringResource(R.string.minutes_value, minutes)
                        )
                    },
                    shape = CircleShape
                )
            }
        }

        SectionDivider()

        TimeSettingRow(
            icon = Icons.Default.Alarm,
            tint = SleepTealAccent,
            title = stringResource(R.string.auto_stop_title),
            description = stringResource(R.string.auto_stop_desc),
            enabled = autoStopEnabled,
            minuteOfDay = autoStopMinuteOfDay,
            onToggle = { onAutoStopChange(it, autoStopMinuteOfDay) },
            onEditTime = { editing = TimeTarget.AUTO_STOP }
        )

        SectionDivider()

        TimeSettingRow(
            icon = Icons.Default.NotificationsActive,
            tint = SleepIndigoLight,
            title = stringResource(R.string.reminder_setting_title),
            description = stringResource(R.string.reminder_desc),
            enabled = reminderEnabled,
            minuteOfDay = reminderMinuteOfDay,
            onToggle = { on -> if (on) enableReminder(reminderMinuteOfDay) else onReminderChange(false, reminderMinuteOfDay) },
            onEditTime = { editing = TimeTarget.REMINDER }
        )
        if (notificationsDenied) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.reminder_notifications_off),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    editing?.let { target ->
        TimePickerDialog(
            initialMinuteOfDay = if (target == TimeTarget.AUTO_STOP) autoStopMinuteOfDay else reminderMinuteOfDay,
            onDismiss = { editing = null },
            onConfirm = { minute ->
                editing = null
                when (target) {
                    TimeTarget.AUTO_STOP -> onAutoStopChange(true, minute)
                    TimeTarget.REMINDER -> enableReminder(minute)
                }
            }
        )
    }
}

private enum class TimeTarget { AUTO_STOP, REMINDER }

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun TimeSettingRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String,
    enabled: Boolean,
    minuteOfDay: Int,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit
) {
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon = icon, tint = tint)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
    Spacer(Modifier.height(8.dp))
    AssistChip(
        onClick = onEditTime,
        label = { Text(formatMinuteOfDay(context, minuteOfDay)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (enabled) tint else MaterialTheme.colorScheme.outline) },
        shape = CircleShape
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_time)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text(stringResource(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
