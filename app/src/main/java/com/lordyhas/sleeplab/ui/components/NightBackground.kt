package com.lordyhas.sleeplab.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.lordyhas.sleeplab.ui.theme.NightSkyBottom
import com.lordyhas.sleeplab.ui.theme.NightSkyTop
import com.lordyhas.sleeplab.ui.theme.SleepIndigoPrimary
import kotlin.random.Random

private class Star(val x: Float, val y: Float, val radius: Float, val alpha: Float)

/**
 * App-wide backdrop: a deep night gradient with a soft indigo glow and a static star field.
 * Stars are drawn once per frame in the draw phase only (no recomposition, no animation)
 * so the background stays battery-friendly during overnight use.
 */
@Composable
fun NightBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val stars = remember {
        val random = Random(seed = 42)
        List(80) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.5f + random.nextFloat() * 1.1f,
                alpha = 0.15f + random.nextFloat() * 0.5f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(NightSkyTop, NightSkyBottom)))

                val glowCenter = Offset(size.width * 0.9f, size.height * 0.02f)
                val glowRadius = size.width * 0.9f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(SleepIndigoPrimary.copy(alpha = 0.22f), Color.Transparent),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    radius = glowRadius,
                    center = glowCenter
                )

                // Stars fade out toward the bottom of the sky.
                val skyHeight = size.height * 0.55f
                stars.forEach { star ->
                    drawCircle(
                        color = Color.White.copy(alpha = star.alpha * (1f - star.y)),
                        radius = star.radius * density,
                        center = Offset(star.x * size.width, star.y * skyHeight)
                    )
                }
            }
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            content()
        }
    }
}
