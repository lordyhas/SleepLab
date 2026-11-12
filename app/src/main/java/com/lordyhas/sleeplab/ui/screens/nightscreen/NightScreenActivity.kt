package com.lordyhas.sleeplab.ui.screens.nightscreen

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.service.SleepTrackingService
import com.lordyhas.sleeplab.ui.components.HoldToConfirmButton
import com.lordyhas.sleeplab.ui.theme.SleepLabTheme
import com.lordyhas.sleeplab.ui.util.formatDuration
import kotlinx.coroutines.delay
import java.util.Date

/**
 * The screen left showing while a night is recorded: black, as dim as the display goes, with
 * only the time and a "recording" line.
 *
 * It is allowed over the lock screen, so waking the phone during the night shows the time
 * without unlocking. That is the reason it is a separate activity: only what is drawn here is
 * reachable while the phone is locked. Going back to the app (history, notes, clips) goes
 * through the keyguard. The display still times out normally; nothing here keeps it on.
 */
class NightScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SleepTrackingService.trackingState.value.isTracking) {
            finish()
            return
        }
        enableEdgeToEdge()
        showOverLockScreen()
        dimDisplay()
        hideSystemBars()

        setContent {
            SleepLabTheme(darkTheme = true) {
                val state by SleepTrackingService.trackingState.collectAsState()
                // The night ended (stopped here, auto-stop, notification, microphone failure).
                LaunchedEffect(state.isTracking) {
                    if (!state.isTracking) finish()
                }
                NightScreen(
                    elapsedSeconds = state.elapsedSeconds,
                    onStop = { SleepTrackingService.stopService(this) },
                    onOpenApp = ::openApp
                )
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }

    /** Lowest brightness for this window only; the system setting is untouched. */
    private fun dimDisplay() {
        window.attributes = window.attributes.apply { screenBrightness = MIN_BRIGHTNESS }
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    /** Back to the full app, which sits behind the keyguard when the phone is locked. */
    private fun openApp() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard?.isKeyguardLocked == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = finish()
            })
        } else {
            finish()
        }
    }

    companion object {
        private const val MIN_BRIGHTNESS = 0.01f

        fun start(context: Context) {
            context.startActivity(
                Intent(context, NightScreenActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
        }
    }
}

/** How long the stop / open buttons stay after a tap. */
private const val CONTROLS_VISIBLE_MS = 6_000L

private val DimText = Color(0xFF8A8FA3)
private val DimmerText = Color(0xFF565B6E)

@Composable
private fun NightScreen(
    elapsedSeconds: Long,
    onStop: () -> Unit,
    onOpenApp: () -> Unit
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    // Moves the text a little every minute so a static clock doesn't mark OLED panels.
    var shiftStep by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            val time = System.currentTimeMillis()
            if (time / 60_000L != now / 60_000L) shiftStep++
            now = time
        }
    }

    // Each tap (re)opens the controls for a few seconds.
    var controlsShownAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(controlsShownAt) {
        if (controlsShownAt != 0L) {
            delay(CONTROLS_VISIBLE_MS)
            controlsShownAt = 0L
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { controlsShownAt = System.currentTimeMillis() })
            }
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = (((shiftStep % 5) - 2) * 6).dp.roundToPx(),
                        y = (((shiftStep % 7) - 3) * 10).dp.roundToPx()
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                DateFormat.getTimeFormat(context).format(Date(now)),
                style = MaterialTheme.typography.displayLarge,
                color = DimText
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.night_screen_recording, formatDuration(elapsedSeconds - elapsedSeconds % 60)),
                style = MaterialTheme.typography.bodyMedium,
                color = DimmerText
            )
        }

        AnimatedVisibility(
            visible = controlsShownAt != 0L,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onOpenApp) {
                    Text(stringResource(R.string.night_screen_open_app), color = DimText)
                }
                Spacer(Modifier.height(8.dp))
                HoldToConfirmButton(
                    label = stringResource(R.string.hold_to_stop),
                    icon = Icons.Default.Stop,
                    color = MaterialTheme.colorScheme.error,
                    onConfirm = onStop,
                    // A short tap keeps the controls up instead of letting them fade mid-gesture.
                    onTapTooShort = { controlsShownAt = System.currentTimeMillis() }
                )
            }
        }
    }
}
