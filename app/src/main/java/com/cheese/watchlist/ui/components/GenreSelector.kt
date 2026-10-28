package com.cheese.watchlist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.GenreEntity

/** Multi-select genre/tag chips — tap any number of genres to toggle them on or off. */
@Composable
fun GenreSelector(
    genres: List<GenreEntity>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { genre ->
            WatchlistFilterChip(
                label = genre.name,
                selected = selected.any { it.equals(genre.name, ignoreCase = true) },
                onClick = { onToggle(genre.name) }
            )
        }
    }
}
