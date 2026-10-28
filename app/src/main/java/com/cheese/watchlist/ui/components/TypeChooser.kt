package com.cheese.watchlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.WatchType

/** "What are you adding?" chooser shown after tapping the FAB. */
@Composable
fun TypeChooser(
    onSelect: (WatchType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            "What are you adding?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TypeChoiceCard(
                icon = Icons.Outlined.Movie,
                title = "Movie",
                subtitle = "A single film",
                onClick = { onSelect(WatchType.MOVIE) },
                modifier = Modifier.weight(1f)
            )
            TypeChoiceCard(
                icon = Icons.Outlined.Tv,
                title = "Series",
                subtitle = "Track episodes and progress",
                onClick = { onSelect(WatchType.SERIES) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TypeChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
