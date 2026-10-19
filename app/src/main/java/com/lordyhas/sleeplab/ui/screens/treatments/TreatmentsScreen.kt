package com.lordyhas.sleeplab.ui.screens.treatments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.data.local.entity.TreatmentEntity
import com.lordyhas.sleeplab.ui.components.EmptyState
import com.lordyhas.sleeplab.ui.components.GlassCard
import com.lordyhas.sleeplab.ui.components.IconBadge
import com.lordyhas.sleeplab.ui.components.ScreenHeader
import com.lordyhas.sleeplab.ui.components.SectionHeader
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.theme.snoreLevelColor
import java.text.DateFormat
import java.util.Date

@Composable
fun TreatmentsScreen(
    viewModel: TreatmentsViewModel = viewModel()
) {
    val treatments by viewModel.treatments.collectAsState()
    val thresholdDb by viewModel.thresholdDb.collectAsState()
    val autoCalibrate by viewModel.autoCalibrate.collectAsState()
    val clipsEnabled by viewModel.clipsEnabled.collectAsState()
    val clipRetentionDays by viewModel.clipRetentionDays.collectAsState()
    val clipStorageBytes by viewModel.clipStorageBytes.collectAsState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TreatmentEntity?>(null) }
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarTop + 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ScreenHeader(title = stringResource(R.string.treatments_and_settings)) }

            item {
                SectionHeader(stringResource(R.string.detection_section))
                Spacer(Modifier.height(4.dp))
                CalibrationCard(
                    autoCalibrate = autoCalibrate,
                    onAutoCalibrateChange = viewModel::setAutoCalibrate,
                    thresholdDb = thresholdDb,
                    onThresholdChange = viewModel::updateThreshold
                )
                Spacer(Modifier.height(14.dp))
                ClipsSettingsCard(
                    enabled = clipsEnabled,
                    retentionDays = clipRetentionDays,
                    storageBytes = clipStorageBytes,
                    onEnabledChange = viewModel::setClipsEnabled,
                    onRetentionChange = viewModel::setClipRetentionDays,
                    onDeleteAll = viewModel::deleteAllClips
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionHeader(
                        stringResource(R.string.your_medical_treatments),
                        modifier = Modifier.weight(1f)
                    )
                    if (treatments.isNotEmpty()) {
                        Text(
                            stringResource(R.string.active_count, treatments.count { it.isActive }),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (treatments.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.MedicalServices,
                        title = stringResource(R.string.no_saved_treatment),
                        message = stringResource(R.string.add_treatment_instruction),
                        action = {
                            OutlinedButton(onClick = { showAddDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_first_treatment))
                            }
                        }
                    )
                }
            } else {
                items(treatments, key = { it.id }) { treatment ->
                    TreatmentItemCard(
                        treatment = treatment,
                        onToggle = { viewModel.toggleTreatmentActive(treatment) },
                        onDelete = { pendingDelete = treatment },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        if (treatments.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_treatment_desc)) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
            )
        }
    }

    if (showAddDialog) {
        AddTreatmentDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                viewModel.addTreatment(name)
                showAddDialog = false
            }
        )
    }

    pendingDelete?.let { treatment ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_treatment_title, treatment.name)) },
            text = { Text(stringResource(R.string.delete_treatment_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTreatment(treatment)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun CalibrationCard(
    autoCalibrate: Boolean,
    onAutoCalibrateChange: (Boolean) -> Unit,
    thresholdDb: Float,
    onThresholdChange: (Float) -> Unit
) {
    // Local slider state so dragging is smooth; persisted when the drag ends.
    var sliderValue by remember { mutableFloatStateOf(thresholdDb) }
    LaunchedEffect(thresholdDb) { sliderValue = thresholdDb }
    val levelColor = snoreLevelColor(sliderValue)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Default.Tune, tint = SleepTealAccent)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.auto_calibrate_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.auto_calibrate_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = autoCalibrate, onCheckedChange = onAutoCalibrateChange)
        }

        AnimatedVisibility(visible = !autoCalibrate) {
            Column {
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.mic_sensitivity), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.manual_threshold_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${sliderValue.toInt()}",
                        style = MaterialTheme.typography.displaySmall,
                        color = levelColor
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "dB",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        stringResource(
                            when {
                                sliderValue < 45f -> R.string.very_sensitive
                                sliderValue < 60f -> R.string.recommended_normal
                                else -> R.string.loud_snoring_only
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { onThresholdChange(sliderValue) },
                    valueRange = 35f..75f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = levelColor,
                        activeTrackColor = levelColor,
                        activeTickColor = MaterialTheme.colorScheme.surface,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.more_sensitive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        stringResource(R.string.loud_only_short),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.sensitivity_scale_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun TreatmentItemCard(
    treatment: TreatmentEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(treatment.startDate) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(treatment.startDate))
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 4.dp, top = 14.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (treatment.isActive) 1f else 0.55f)
            ) {
                IconBadge(
                    icon = Icons.Default.MedicalServices,
                    tint = if (treatment.isActive) SleepIndigoLight else MaterialTheme.colorScheme.onSurfaceVariant,
                    size = 44.dp
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        treatment.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2
                    )
                    Text(
                        stringResource(R.string.since_date, formattedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = treatment.isActive,
                onCheckedChange = { onToggle() },
                thumbContent = if (treatment.isActive) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize)
                        )
                    }
                } else null
            )

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddTreatmentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val presets = listOf(
        stringResource(R.string.treatment_orthosis),
        stringResource(R.string.treatment_drops),
        stringResource(R.string.treatment_strips),
        stringResource(R.string.treatment_pillow),
        stringResource(R.string.treatment_cpap)
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
        title = { Text(stringResource(R.string.new_treatment)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.treatment_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(R.string.common_suggestions),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { preset ->
                        SuggestionChip(
                            onClick = { text = preset },
                            label = { Text(preset, maxLines = 1) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
