package com.cheese.watchlist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.SourceEntity

@Composable
fun SourceSelector(
    sources: List<SourceEntity>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sources) { source ->
            WatchlistFilterChip(
                label = source.name,
                selected = selected.equals(source.name, ignoreCase = true),
                onClick = { onSelect(source.name) }
            )
        }
    }
}
