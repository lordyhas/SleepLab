package com.lordyhas.sleeplab.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.SleepLabApp
import com.lordyhas.sleeplab.ui.theme.SnoreMedium

/** True when the system may restrict this app in the background to save battery. */
fun isBatteryOptimized(context: Context): Boolean {
    val power = context.getSystemService(PowerManager::class.java) ?: return false
    return !power.isIgnoringBatteryOptimizations(context.packageName)
}

/**
 * Battery optimisation is the usual reason a night stops being recorded half-way through,
 * especially on phones with aggressive power management. While it applies, this card explains
 * the risk and opens the app's system page, where the user can set battery use to
 * "Unrestricted". It goes away once that is done, or for good with "Not now".
 */
@Composable
fun BatteryOptimizationCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = SleepLabApp.instance.settingsRepository
    val dismissed by settings.batteryHintDismissed.collectAsState()
    var optimized by remember { mutableStateOf(isBatteryOptimized(context)) }
    // Re-check when coming back from the system settings.
    LifecycleResumeEffect(Unit) {
        optimized = isBatteryOptimized(context)
        onPauseOrDispose { }
    }
    if (!optimized || dismissed) return

    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.BatterySaver, tint = SnoreMedium, size = 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.battery_opt_title), style = MaterialTheme.typography.titleSmall)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.battery_opt_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { settings.setBatteryHintDismissed(true) }) {
                Text(stringResource(R.string.not_now))
            }
            TextButton(onClick = { openAppSystemSettings(context) }) {
                Text(stringResource(R.string.open_settings))
            }
        }
    }
}

private fun openAppSystemSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No such settings page on this device: nothing to open.
    }
}
