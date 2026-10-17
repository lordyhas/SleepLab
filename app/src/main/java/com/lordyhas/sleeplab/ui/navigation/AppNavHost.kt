package com.lordyhas.sleeplab.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lordyhas.sleeplab.service.SleepTrackingService
import com.lordyhas.sleeplab.ui.components.NightBackground
import com.lordyhas.sleeplab.ui.screens.analytics.AnalyticsScreen
import com.lordyhas.sleeplab.ui.screens.home.HomeScreen
import com.lordyhas.sleeplab.ui.screens.sessiondetail.SessionDetailScreen
import com.lordyhas.sleeplab.ui.screens.tracking.TrackingScreen
import com.lordyhas.sleeplab.ui.screens.treatments.TreatmentsScreen
import com.lordyhas.sleeplab.ui.theme.CardBorder
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent

private const val TRANSITION_MILLIS = 260

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

    fun navigateToTab(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NightBackground {
        Scaffold(
            containerColor = Color.Transparent,
            // Only the bottom bar reserves space; each screen pads for the status bar itself
            // so content can scroll under it over the night-sky background.
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.drawBehind {
                        drawLine(CardBorder, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
                    }
                ) {
                    items.forEach { screen ->
                        val selected = currentRoute == screen.route ||
                                (screen == Screen.Analytics && currentRoute?.startsWith("session_detail") == true)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { if (currentRoute != screen.route) navigateToTab(screen) },
                            icon = {
                                if (screen == Screen.Tracking && trackingState.isTracking) {
                                    BadgedBox(badge = { LiveBadge() }) {
                                        Icon(screen.icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(screen.titleResId), maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = { fadeIn(tween(TRANSITION_MILLIS)) + scaleIn(tween(TRANSITION_MILLIS), initialScale = 0.98f) },
                exitTransition = { fadeOut(tween(TRANSITION_MILLIS / 2)) },
                popEnterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
                popExitTransition = { fadeOut(tween(TRANSITION_MILLIS / 2)) }
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToTracking = { navigateToTab(Screen.Tracking) },
                        onNavigateToTreatments = { navigateToTab(Screen.Treatments) },
                        onNavigateToSessionDetail = { sessionId ->
                            navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                        }
                    )
                }

                composable(Screen.Tracking.route) {
                    TrackingScreen(
                        onNavigateToTreatments = { navigateToTab(Screen.Treatments) }
                    )
                }

                composable(Screen.Analytics.route) {
                    AnalyticsScreen(
                        onNavigateToSessionDetail = { sessionId ->
                            navController.navigate(Screen.SessionDetail.createRoute(sessionId))
                        }
                    )
                }

                composable(Screen.Treatments.route) {
                    TreatmentsScreen()
                }

                composable(
                    route = Screen.SessionDetail.route,
                    arguments = listOf(navArgument("sessionId") { type = NavType.IntType }),
                    enterTransition = {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(TRANSITION_MILLIS)) +
                                fadeIn(tween(TRANSITION_MILLIS))
                    },
                    popExitTransition = {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(TRANSITION_MILLIS)) +
                                fadeOut(tween(TRANSITION_MILLIS))
                    }
                ) { backStackEntry ->
                    val sessionId = backStackEntry.arguments?.getInt("sessionId") ?: return@composable
                    SessionDetailScreen(
                        sessionId = sessionId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/** Pulsing dot shown on the Tracking tab while a night is being recorded. */
@Composable
private fun LiveBadge() {
    val transition = rememberInfiniteTransition(label = "liveBadge")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "liveBadgeAlpha"
    )
    Badge(containerColor = SleepTealAccent, modifier = Modifier.alpha(alpha))
}
