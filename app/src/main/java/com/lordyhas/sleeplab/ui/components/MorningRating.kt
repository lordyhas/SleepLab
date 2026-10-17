package com.lordyhas.sleeplab.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.lordyhas.sleeplab.R
import com.lordyhas.sleeplab.ui.theme.SleepTealAccent
import com.lordyhas.sleeplab.ui.theme.SnoreHigh
import com.lordyhas.sleeplab.ui.theme.SnoreLow
import com.lordyhas.sleeplab.ui.theme.SnoreMedium

private data class RatingOption(val value: Int, val icon: ImageVector, val labelRes: Int, val color: Color)

private val RatingOptions = listOf(
    RatingOption(1, Icons.Default.SentimentVeryDissatisfied, R.string.rating_1, SnoreHigh),
    RatingOption(2, Icons.Default.SentimentDissatisfied, R.string.rating_2, Color(0xFFFB923C)),
    RatingOption(3, Icons.Default.SentimentNeutral, R.string.rating_3, SnoreMedium),
    RatingOption(4, Icons.Default.SentimentSatisfied, R.string.rating_4, SnoreLow),
    RatingOption(5, Icons.Default.SentimentVerySatisfied, R.string.rating_5, SleepTealAccent)
)

/** Five-face morning rating (1 = awful … 5 = great). Tapping the selected face clears it. */
@Composable
fun MorningRatingSelector(
    rating: Int?,
    onRatingChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        RatingOptions.forEach { option ->
            val selected = rating == option.value
            val background by animateColorAsState(
                if (selected) option.color.copy(alpha = 0.18f) else Color.Transparent,
                label = "ratingBackground"
            )
            val tint by animateColorAsState(
                if (selected) option.color else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "ratingTint"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(background)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onRatingChange(if (selected) null else option.value) }
                    )
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                Icon(option.icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(option.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1
                )
            }
        }
    }
}
