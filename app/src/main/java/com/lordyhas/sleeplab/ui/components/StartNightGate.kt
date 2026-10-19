package com.lordyhas.sleeplab.ui.components

import android.content.Context
import android.os.BatteryManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.lordyhas.sleeplab.R

/** Below this charge, starting unplugged triggers a warning. */
private const val LOW_BATTERY_PERCENT = 50

/**
 * Everything that must happen before a night starts, in order: a charger check (warns when
 * unplugged and below [LOW_BATTERY_PERCENT] %, with "start anyway"), then the microphone
 * permission ([rememberMicPermissionGate]), then [onStart].
 */
@Composable
fun rememberStartNightGate(onStart: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val micGate = rememberMicPermissionGate(onStart)
    var batteryPercent by remember { mutableIntStateOf(-1) }
    var showBatteryWarning by remember { mutableStateOf(false) }

    if (showBatteryWarning) {
        AlertDialog(
            onDismissRequest = { showBatteryWarning = false },
            icon = { Icon(Icons.Default.BatteryAlert, contentDescription = null) },
            title = { Text(stringResource(R.string.battery_title)) },
            text = { Text(stringResource(R.string.battery_message, batteryPercent)) },
            confirmButton = {
                TextButton(onClick = {
                    showBatteryWarning = false
                    micGate()
                }) { Text(stringResource(R.string.battery_start_anyway)) }
            },
            dismissButton = {
                Button(onClick = { showBatteryWarning = false }) { Text(stringResource(R.string.battery_plug_in)) }
            }
        )
    }

    return remember(micGate) {
        {
            val (charging, percent) = batteryStatus(context)
            if (!charging && percent in 0 until LOW_BATTERY_PERCENT) {
                batteryPercent = percent
                showBatteryWarning = true
            } else {
                micGate()
            }
        }
    }
}

/** (isCharging, percent 0–100 or -1 if unknown). */
private fun batteryStatus(context: Context): Pair<Boolean, Int> {
    val manager = context.getSystemService(BatteryManager::class.java) ?: return true to -1
    val percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return manager.isCharging to (if (percent in 0..100) percent else -1)
}
