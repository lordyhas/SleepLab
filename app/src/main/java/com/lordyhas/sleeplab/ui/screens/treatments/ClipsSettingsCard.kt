package com.lordyhas.sleeplab.ui.screens.treatments

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.IconBadge
import com.lordyhas.sleeplab.ui.theme.SnoreMedium

private val RetentionOptions = listOf(7, 14, 30, 0) // 0 = forever

/** Opt-in snore recordings: consent, retention, storage used, delete all. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClipsSettingsCard(
    enabled: Boolean,
    retentionDays: Int,
    storageBytes: Long,
    onEnabledChange: (Boolean) -> Unit,
    onRetentionChange: (Int) -> Unit,
    onDeleteAll: () -> Unit
) {
    val context = LocalContext.current
    var showConsent by remember { mutableStateOf(false) }
    var showDeleteAll by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.GraphicEq, tint = SnoreMedium)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.clips_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.clips_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = { on -> if (on) showConsent = true else onEnabledChange(false) }
            )
        }

        AnimatedVisibility(visible = enabled || storageBytes > 0) {
            Column {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.clips_keep_for),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RetentionOptions.forEach { days ->
                        FilterChip(
                            selected = retentionDays == days,
                            onClick = { onRetentionChange(days) },
                            label = {
                                Text(
                                    if (days == 0) stringResource(R.string.clips_forever)
                                    else stringResource(R.string.clips_days, days)
                                )
                            },
                            shape = CircleShape
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.height(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.clips_storage, Formatter.formatShortFileSize(context, storageBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.weight(1f)
                    )
                    if (storageBytes > 0) {
                        TextButton(onClick = { showDeleteAll = true }) {
                            Text(stringResource(R.string.clips_delete_all), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showConsent) {
        AlertDialog(
            onDismissRequest = { showConsent = false },
            icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
            title = { Text(stringResource(R.string.clips_consent_title)) },
            text = { Text(stringResource(R.string.clips_consent_message)) },
            confirmButton = {
                Button(onClick = {
                    showConsent = false
                    onEnabledChange(true)
                }) { Text(stringResource(R.string.clips_consent_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showConsent = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showDeleteAll) {
        AlertDialog(
            onDismissRequest = { showDeleteAll = false },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.clips_delete_all_title)) },
            text = { Text(stringResource(R.string.clips_delete_all_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAll = false
                        onDeleteAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAll = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
