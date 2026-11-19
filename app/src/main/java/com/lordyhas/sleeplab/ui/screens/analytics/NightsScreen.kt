package com.lordyhas.sleeplab.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.ui.components.EmptyState
import com.lordyhas.sleeplab.ui.components.ScreenHeader
import com.lordyhas.sleeplab.ui.components.SectionHeader
import com.lordyhas.sleeplab.ui.theme.SleepIndigoLight

/** Every recorded night: calendar on top, then the list, each opening its report. */
@Composable
fun NightsScreen(
    viewModel: AnalyticsViewModel = viewModel(),
    onNavigateToSessionDetail: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = statusBarTop + 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { ScreenHeader(title = stringResource(R.string.screen_nights)) }

        if (!uiState.isLoaded) return@LazyColumn

        if (uiState.historySessions.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.NightsStay,
                    title = stringResource(R.string.no_nights_title),
                    message = stringResource(R.string.no_night_recorded),
                    tint = SleepIndigoLight
                )
            }
            return@LazyColumn
        }

        item {
            CalendarCard(
                minutesByDay = uiState.minutesByDay,
                sessionIdByDay = uiState.sessionIdByDay,
                onNightClick = onNavigateToSessionDetail
            )
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(stringResource(R.string.nights_history))
        }

        items(uiState.historySessions, key = { it.session.id }) { item ->
            SessionHistoryRow(
                item = item,
                onClick = { onNavigateToSessionDetail(item.session.id) }
            )
        }
    }
}
