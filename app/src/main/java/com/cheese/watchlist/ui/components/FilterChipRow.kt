package com.cheese.watchlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A single pill-shaped filter chip, used for genres and statuses. */
@Composable
fun WatchlistFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(if (selected) accentColor else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) accentColor else MaterialTheme.colorScheme.outline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun <T> FilterChipRow(
    items: List<T>,
    labelFor: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(items) { item ->
            WatchlistFilterChip(
                label = labelFor(item),
                selected = isSelected(item),
                onClick = { onSelect(item) }
            )
        }
    }
}
