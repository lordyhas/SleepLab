package com.lordyhas.sleeplab.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.lordyhas.sleeplab.R

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    /** Set up and start tonight; the live view while recording; last night's report in the morning. */
    object Tonight : Screen("tonight", R.string.screen_tonight, Icons.Default.Bedtime)
    object Nights : Screen("nights", R.string.screen_nights, Icons.Default.CalendarMonth)
    object Trends : Screen("trends", R.string.screen_trends, Icons.Default.Insights)

    // Pushed on top of the tabs
    object Settings : Screen("settings", R.string.settings_title, Icons.Default.Settings)
    object Treatments : Screen("treatments", R.string.screen_treatments, Icons.Default.MedicalServices)
    object SessionDetail : Screen("session_detail/{sessionId}", R.string.screen_session_detail, Icons.Default.CalendarMonth) {
        fun createRoute(sessionId: Int) = "session_detail/$sessionId"
    }
}
