package com.lordyhas.sonrelab.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Accueil", Icons.Default.Home)
    object Tracking : Screen("tracking", "Enregistrement", Icons.Default.Bedtime)
    object Analytics : Screen("analytics", "Statistiques", Icons.Default.BarChart)
    object Treatments : Screen("treatments", "Traitements", Icons.Default.MedicalServices)
}
