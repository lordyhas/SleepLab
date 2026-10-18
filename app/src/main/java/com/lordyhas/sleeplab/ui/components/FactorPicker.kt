package com.lordyhas.sleeplab.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Sick
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.data.local.entity.BuiltInFactors
import com.lordyhas.sleeplab.data.local.entity.FactorEntity

/** Display name: built-ins are translated, custom factors use what the user typed. */
@Composable
fun FactorEntity.label(): String = when (key) {
    BuiltInFactors.ALCOHOL -> stringResource(R.string.factor_alcohol)
    BuiltInFactors.LATE_MEAL -> stringResource(R.string.factor_late_meal)
    BuiltInFactors.CONGESTION -> stringResource(R.string.factor_congestion)
    BuiltInFactors.BACK_SLEEPING -> stringResource(R.string.factor_back_sleeping)
    BuiltInFactors.EXHAUSTED -> stringResource(R.string.factor_exhausted)
    BuiltInFactors.SEDATIVES -> stringResource(R.string.factor_sedatives)
    else -> name ?: "?"
}

fun FactorEntity.icon(): ImageVector = when (key) {
    BuiltInFactors.ALCOHOL -> Icons.Default.LocalBar
    BuiltInFactors.LATE_MEAL -> Icons.Default.Restaurant
    BuiltInFactors.CONGESTION -> Icons.Default.Sick
    BuiltInFactors.BACK_SLEEPING -> Icons.Default.AirlineSeatFlat
    BuiltInFactors.EXHAUSTED -> Icons.Default.BatteryAlert
    BuiltInFactors.SEDATIVES -> Icons.Default.Medication
    else -> Icons.AutoMirrored.Filled.Label
}

/**
 * Multi-select chips for the factors of a night, plus "Add" for a custom one.
 * Archived factors are only shown if they are already selected.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FactorPicker(
    factors: List<FactorEntity>,
    selectedIds: Set<Int>,
    onToggle: (factorId: Int) -> Unit,
    onAddCustom: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAdd by remember { mutableStateOf(false) }

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        factors.filter { !it.isArchived || it.id in selectedIds }.forEach { factor ->
            val selected = factor.id in selectedIds
            FilterChip(
                selected = selected,
                onClick = { onToggle(factor.id) },
                label = { Text(factor.label(), maxLines = 1) },
                leadingIcon = { Icon(factor.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.White.copy(alpha = 0.04f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = Color.White.copy(alpha = 0.1f),
                    selectedBorderColor = Color.Transparent
                )
            )
        }
        AssistChip(
            onClick = { showAdd = true },
            label = { Text(stringResource(R.string.factor_add)) },
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
            shape = CircleShape
        )
    }

    if (showAdd) {
        AddFactorDialog(
            onDismiss = { showAdd = false },
            onConfirm = { name ->
                showAdd = false
                onAddCustom(name)
            }
        )
    }
}

@Composable
private fun AddFactorDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.factor_add_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(40) },
                placeholder = { Text(stringResource(R.string.factor_add_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
