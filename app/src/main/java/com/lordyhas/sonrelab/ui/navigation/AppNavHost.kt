package com.lordyhas.sonrelab.ui.navigation

import androidx.compose.animation.AnimatedVisibility
 import androidx.compose.ui.res.stringResource
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lordyhas.sonrelab.service.SleepTrackingService
import com.lordyhas.sonrelab.ui.screens.analytics.AnalyticsScreen
import com.lordyhas.sonrelab.ui.screens.home.HomeScreen
import com.lordyhas.sonrelab.ui.screens.tracking.TrackingScreen
import com.lordyhas.sonrelab.ui.screens.treatments.TreatmentsScreen
import com.lordyhas.sonrelab.ui.theme.SleepTealAccent

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        Screen.Home,
        Screen.Tracking,
        Screen.Analytics,
        Screen.Treatments
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val trackingState by SleepTrackingService.trackingState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            if (screen == Screen.Tracking && trackingState.isTracking) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = SleepTealAccent)
                                    }
                                ) {
                                    Icon(
                                        screen.icon,
                                        contentDescription = stringResource(screen.titleResId),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    screen.icon,
                                    contentDescription = stringResource(screen.titleResId),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                stringResource(screen.titleResId),
                                fontSize = 11.sp,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToTracking = {
                        navController.navigate(Screen.Tracking.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToTreatments = {
                        navController.navigate(Screen.Treatments.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Screen.Tracking.route) {
                TrackingScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route)
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }

            composable(Screen.Treatments.route) {
                TreatmentsScreen()
            }
        }
    }
}
