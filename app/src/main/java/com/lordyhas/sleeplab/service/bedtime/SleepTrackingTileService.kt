package com.lordyhas.sleeplab.service.bedtime

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.lordyhas.sleeplab.LaunchAction
import com.lordyhas.sleeplab.MainActivity
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.service.SleepTrackingService
import com.lordyhas.sleeplab.ui.util.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Quick Settings tile. Stopping works directly; starting opens the app, because a microphone
 * foreground service must be started while the app is visible (and the permission / charger
 * checks live in the UI).
 */
class SleepTrackingTileService : TileService() {

    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        observeJob?.cancel()
        observeJob = CoroutineScope(Dispatchers.Main).launch {
            SleepTrackingService.trackingState.collect { updateTile() }
        }
    }

    override fun onStopListening() {
        observeJob?.cancel()
        observeJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (SleepTrackingService.trackingState.value.isTracking) {
            SleepTrackingService.stopService(this)
        } else {
            openApp()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = MainActivity.launchIntent(this, LaunchAction.START_TRACKING)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val state = SleepTrackingService.trackingState.value
        tile.state = if (state.isTracking) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (state.isTracking) formatClock(state.elapsedSeconds) else getString(R.string.tile_tap_to_start)
        }
        tile.updateTile()
    }

    companion object {
        /** Asks the system to refresh the tile (call when tracking starts or stops). */
        fun refresh(context: Context) {
            try {
                requestListeningState(context, ComponentName(context, SleepTrackingTileService::class.java))
            } catch (_: Exception) {
                // Tile not added by the user / not supported: nothing to refresh.
            }
        }
    }
}
