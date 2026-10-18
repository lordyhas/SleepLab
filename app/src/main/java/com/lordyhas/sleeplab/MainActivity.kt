package com.lordyhas.sleeplab

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.lordyhas.sleeplab.ui.navigation.AppNavHost
import com.lordyhas.sleeplab.ui.theme.SleepLabTheme

/** What an external entry point (reminder, Quick Settings tile) asks the app to do. */
enum class LaunchAction { OPEN_TRACKING, START_TRACKING }

class MainActivity : ComponentActivity() {

    /** Pending request from the launching intent; cleared once the UI has handled it. */
    private val launchAction = mutableStateOf<LaunchAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only on a fresh launch: after a rotation the request was already handled.
        if (savedInstanceState == null) launchAction.value = intent.launchAction()
        setContent {
            SleepLabTheme(darkTheme = true) {
                AppNavHost(
                    launchAction = launchAction.value,
                    onLaunchActionHandled = { launchAction.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.launchAction()?.let { launchAction.value = it }
    }

    private fun Intent.launchAction(): LaunchAction? =
        getStringExtra(EXTRA_LAUNCH_ACTION)?.let { name -> LaunchAction.entries.firstOrNull { it.name == name } }

    companion object {
        private const val EXTRA_LAUNCH_ACTION = "com.lordyhas.sleeplab.extra.LAUNCH_ACTION"

        fun launchIntent(context: Context, action: LaunchAction): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_LAUNCH_ACTION, action.name)
            }
    }
}
