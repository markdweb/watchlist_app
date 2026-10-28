package com.cheese.watchlist.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.ui.components.AppLogo
import kotlinx.coroutines.delay

/** A one-time "Welcome" moment shown right after onboarding finishes — never shown again. */
@Composable
fun FirstGreetingScreen(name: String, onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(500))
        delay(1400)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppLogo(size = 72.dp)
            androidx.compose.foundation.layout.Spacer(Modifier.size(24.dp))
            Text(
                text = if (name.isNotBlank()) "Welcome, $name!" else "Welcome!",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            Text(
                "Let's build your watchlist.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
