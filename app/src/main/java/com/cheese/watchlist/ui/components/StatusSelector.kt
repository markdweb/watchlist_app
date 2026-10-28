package com.cheese.watchlist.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.ui.theme.StatusDropped
import com.cheese.watchlist.ui.theme.StatusToWatch
import com.cheese.watchlist.ui.theme.StatusWatched
import com.cheese.watchlist.ui.theme.StatusWatching

fun statusColor(status: WatchStatus) = when (status) {
    WatchStatus.TO_WATCH -> StatusToWatch
    WatchStatus.WATCHING -> StatusWatching
    WatchStatus.WATCHED -> StatusWatched
    WatchStatus.DROPPED -> StatusDropped
}

/** Selector used inside the Add/Edit form. */
@Composable
fun StatusSelector(
    selected: WatchStatus,
    onSelect: (WatchStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(WatchStatus.entries) { status ->
            WatchlistFilterChip(
                label = status.label,
                selected = selected == status,
                onClick = { onSelect(status) },
                accentColor = statusColor(status)
            )
        }
    }
}
