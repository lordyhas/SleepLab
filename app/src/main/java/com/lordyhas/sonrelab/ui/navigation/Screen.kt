package com.lordyhas.sonrelab.ui.navigation
import com.lordyhas.sonrelab.R

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector) {
    object Home : Screen("home", R.string.screen_home, Icons.Default.Home)
    object Tracking : Screen("tracking", R.string.screen_tracking, Icons.Default.Bedtime)
    object Analytics : Screen("analytics", R.string.screen_analytics, Icons.Default.BarChart)
    object Treatments : Screen("treatments", R.string.screen_treatments, Icons.Default.MedicalServices)
}
