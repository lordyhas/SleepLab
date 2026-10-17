package com.lordyhas.sleeplab.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A button that only fires after being pressed and held for [holdMillis].
 * Prevents ending a whole night's recording with a sleepy accidental tap.
 * A quick tap calls [onTapTooShort] so the UI can explain the gesture.
 * Accessibility services get a regular click action.
 */
@Composable
fun HoldToConfirmButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdMillis: Int = 1200,
    onTapTooShort: () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val currentOnTapTooShort by rememberUpdatedState(onTapTooShort)
    val shape = MaterialTheme.shapes.large

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), shape)
            .semantics {
                role = Role.Button
                onClick(label = label) {
                    currentOnConfirm()
                    true
                }
            }
            .pointerInput(holdMillis) {
                detectTapGestures(
                    onPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val job = scope.launch {
                            progress.animateTo(
                                1f,
                                tween(
                                    durationMillis = (holdMillis * (1f - progress.value)).toInt(),
                                    easing = LinearEasing
                                )
                            )
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            currentOnConfirm()
                        }
                        val released = tryAwaitRelease()
                        if (job.isActive) {
                            job.cancel()
                            if (released) currentOnTapTooShort()
                            scope.launch { progress.animateTo(0f, tween(250)) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Fill that grows while holding
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(progress.value)
                .background(color.copy(alpha = 0.35f))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}
