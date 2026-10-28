package com.cheese.watchlist.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.ui.components.AppLogo
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val sweepAngle = remember { Animatable(0f) }
    var showText by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        sweepAngle.animateTo(
            targetValue = 300f,
            animationSpec = tween(durationMillis = 900, easing = EaseOutCubic)
        )
        showText = 1f
        delay(600)
        onFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(132.dp)) {
                    drawArc(
                        color = androidx.compose.ui.graphics.Color(0xFFE7E5F0),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = androidx.compose.ui.graphics.Color(0xFF4F6FF5),
                        startAngle = -90f,
                        sweepAngle = sweepAngle.value,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                AppLogo(size = 96.dp)
            }

            androidx.compose.foundation.layout.Spacer(Modifier.size(28.dp))

            androidx.compose.animation.AnimatedVisibility(visible = showText > 0f) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Watchlist",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                    Text(
                        "Track your movies & series",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
