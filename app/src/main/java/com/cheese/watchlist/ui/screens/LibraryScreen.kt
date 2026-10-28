package com.cheese.watchlist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.*
import com.cheese.watchlist.data.repository.CollectionProgress
import com.cheese.watchlist.data.repository.LibraryStats
import com.cheese.watchlist.data.repository.SeriesProgress
import com.cheese.watchlist.viewmodel.*

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onAddMovie: () -> Unit,
    onAddSeries: () -> Unit,
    onAddCollection: () -> Unit,
    onMovie: (Long) -> Unit,
    onSeries: (Long) -> Unit,
    onCollection: (Long) -> Unit,
    onSettings: () -> Unit,
    onEditSeries: (Long) -> Unit,
    onEditCollection: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    var stats by remember { mutableStateOf(LibraryStats()) }
    var showAdd by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }

    LaunchedEffect(
        state.movies,
        state.series,
        state.collections
    ) {
        stats = viewModel.stats()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add"
                )
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = 20.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---------------------------------------------------------
            // HEADER
            // ---------------------------------------------------------

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "My Lists",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = "Everything you want to watch, all in one place.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onSettings
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }

            // ---------------------------------------------------------
            // WANT / WATCHED SWITCH
            // ---------------------------------------------------------

            item {
                WantWatchedTabs(
                    selected = state.statusFilter,
                    onSelect = viewModel::setStatus
                )
            }

            // ---------------------------------------------------------
            // TYPE FILTER
            // ---------------------------------------------------------

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryTypeFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = state.typeFilter == filter,
                            onClick = {
                                viewModel.setType(filter)
                            },
                            label = {
                                Text(
                                    when (filter) {
                                        LibraryTypeFilter.ALL -> "All"
                                        LibraryTypeFilter.MOVIES -> "Movies"
                                        LibraryTypeFilter.SERIES -> "Series"
                                        LibraryTypeFilter.COLLECTIONS -> "Collections"
                                    }
                                )
                            },
                            leadingIcon = {
                                when (filter) {
                                    LibraryTypeFilter.ALL -> {
                                        Icon(
                                            Icons.Outlined.Apps,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    LibraryTypeFilter.MOVIES -> {
                                        Icon(
                                            Icons.Outlined.Movie,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    LibraryTypeFilter.SERIES -> {
                                        Icon(
                                            Icons.Outlined.Tv,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    LibraryTypeFilter.COLLECTIONS -> {
                                        Icon(
                                            Icons.Outlined.CollectionsBookmark,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }

                    AssistChip(
                        onClick = { showSort = true },
                        label = {
                            Text("Sort")
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // ---------------------------------------------------------
            // SEARCH
            // ---------------------------------------------------------

            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("Search your lists...")
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.setQuery("")
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp)
                )
            }

            // ---------------------------------------------------------
            // COLLECTIONS
            // ---------------------------------------------------------

            if (
                state.typeFilter == LibraryTypeFilter.ALL ||
                state.typeFilter == LibraryTypeFilter.COLLECTIONS
            ) {
                item {
                    SectionHeader(
                        title = "Collections",
                        subtitle = if (state.filteredCollections.isEmpty()) {
                            "Create a collection to group movies together."
                        } else {
                            "${state.filteredCollections.size} collection" +
                                    if (state.filteredCollections.size == 1) "" else "s"
                        }
                    )
                }

                if (state.filteredCollections.isEmpty()) {
                    item {
                        EmptyCard(
                            icon = Icons.Outlined.CollectionsBookmark,
                            title = "No collections yet",
                            subtitle = "Use the + button to create your first collection."
                        )
                    }
                } else {
                    items(
                        items = state.filteredCollections,
                        key = { "collection_${it.id}" }
                    ) { collection ->

                        val progress = state.collectionProgress[collection.id]

                        SwipeDeleteRow(
                            onDelete = {
                                deleteTarget = DeleteTarget.Collection(collection)
                            }
                        ) {
                            CollectionCard(
                                collection = collection,
                                progress = progress,
                                onClick = {
                                    onCollection(collection.id)
                                },
                                onEdit = {
                                    onEditCollection(collection.id)
                                }
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // CONTINUE WATCHING
            // ---------------------------------------------------------

            val continueSeries = state.series
                .filter {
                    val progress = state.seriesProgress[it.id]
                    progress != null &&
                            progress.watched > 0 &&
                            progress.remaining > 0
                }
                .sortedByDescending { it.updatedAt }
                .take(5)

            if (
                continueSeries.isNotEmpty() &&
                state.typeFilter != LibraryTypeFilter.COLLECTIONS &&
                state.statusFilter != LibraryStatusFilter.WATCHED
            ) {
                item {
                    SectionHeader(
                        title = "Continue Watching",
                        subtitle = "Pick up where you left off."
                    )
                }

                items(
                    items = continueSeries,
                    key = { "continue_${it.id}" }
                ) { series ->

                    SeriesCard(
                        series = series,
                        progress = state.seriesProgress[series.id],
                        onClick = {
                            onSeries(series.id)
                        },
                        onFavorite = {
                            viewModel.toggleSeriesFavorite(series)
                        },
                        onEdit = {
                            onEditSeries(series.id)
                        }
                    )
                }
            }

            // ---------------------------------------------------------
            // MOVIES
            // ---------------------------------------------------------

            if (
                state.typeFilter == LibraryTypeFilter.ALL ||
                state.typeFilter == LibraryTypeFilter.MOVIES
            ) {
                item {
                    SectionHeader(
                        title = "Movies",
                        subtitle = "${state.filteredMovies.size} movie" +
                                if (state.filteredMovies.size == 1) "" else "s"
                    )
                }

                if (state.filteredMovies.isEmpty()) {
                    item {
                        EmptyCard(
                            icon = Icons.Outlined.Movie,
                            title = "No movies found",
                            subtitle = "Movies matching your current filters will appear here."
                        )
                    }
                } else {
                    items(
                        items = state.filteredMovies,
                        key = { "movie_${it.id}" }
                    ) { movie ->

                        SwipeDeleteRow(
                            onDelete = {
                                deleteTarget = DeleteTarget.Movie(movie)
                            }
                        ) {
                            MovieCard(
                                movie = movie,
                                onClick = {
                                    onMovie(movie.id)
                                },
                                onFavorite = {
                                    viewModel.toggleMovieFavorite(movie)
                                }
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // SERIES
            // ---------------------------------------------------------

            if (
                state.typeFilter == LibraryTypeFilter.ALL ||
                state.typeFilter == LibraryTypeFilter.SERIES
            ) {
                item {
                    SectionHeader(
                        title = "Series",
                        subtitle = "${state.filteredSeries.size} series" +
                                if (state.filteredSeries.size == 1) "" else ""
                    )
                }

                if (state.filteredSeries.isEmpty()) {
                    item {
                        EmptyCard(
                            icon = Icons.Outlined.Tv,
                            title = "No series found",
                            subtitle = "Series matching your current filters will appear here."
                        )
                    }
                } else {
                    items(
                        items = state.filteredSeries,
                        key = { "series_${it.id}" }
                    ) { series ->

                        SwipeDeleteRow(
                            onDelete = {
                                deleteTarget = DeleteTarget.Series(series)
                            }
                        ) {
                            SeriesCard(
                                series = series,
                                progress = state.seriesProgress[series.id],
                                onClick = {
                                    onSeries(series.id)
                                },
                                onFavorite = {
                                    viewModel.toggleSeriesFavorite(series)
                                },
                                onEdit = {
                                    onEditSeries(series.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // ADD DIALOG
    // -------------------------------------------------------------

    if (showAdd) {
        AlertDialog(
            onDismissRequest = {
                showAdd = false
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Add to My Lists",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AddChoice(
                        label = "Movie",
                        subtitle = "Add a movie to your watchlist",
                        icon = Icons.Outlined.Movie
                    ) {
                        showAdd = false
                        onAddMovie()
                    }

                    AddChoice(
                        label = "Series",
                        subtitle = "Track a TV series and episodes",
                        icon = Icons.Outlined.Tv
                    ) {
                        showAdd = false
                        onAddSeries()
                    }

                    AddChoice(
                        label = "Collection",
                        subtitle = "Group movies into a watch order",
                        icon = Icons.Outlined.CollectionsBookmark
                    ) {
                        showAdd = false
                        onAddCollection()
                    }
                }
            },
            confirmButton = {}
        )
    }

    // -------------------------------------------------------------
    // SORT DIALOG
    // -------------------------------------------------------------

    if (showSort) {
        AlertDialog(
            onDismissRequest = {
                showSort = false
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Sort My Lists",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    LibrarySort.entries.forEach { option ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.setSort(option)
                                    showSort = false
                                }
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = option == state.sort,
                                onClick = {
                                    viewModel.setSort(option)
                                    showSort = false
                                }
                            )

                            Spacer(Modifier.width(8.dp))

                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // -------------------------------------------------------------
    // DELETE DIALOG
    // -------------------------------------------------------------

    deleteTarget?.let { target ->

        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
            },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete ${target.title}?",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (target is DeleteTarget.Collection) {
                        "The collection will be deleted, but the movies inside it will remain in your library."
                    } else {
                        "This item will be removed from your watchlist."
                    }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                    }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (target) {
                            is DeleteTarget.Movie ->
                                viewModel.deleteMovie(target.movie.id)

                            is DeleteTarget.Series ->
                                viewModel.deleteSeries(target.series.id)

                            is DeleteTarget.Collection ->
                                viewModel.deleteCollection(target.collection.id)
                        }

                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        )
    }
}

// =====================================================================
// WANT / WATCHED TABS
// =====================================================================

@Composable
private fun WantWatchedTabs(
    selected: LibraryStatusFilter,
    onSelect: (LibraryStatusFilter) -> Unit
) {
    val wantSelected =
        selected == LibraryStatusFilter.ALL ||
                selected == LibraryStatusFilter.TO_WATCH

    val watchedSelected =
        selected == LibraryStatusFilter.WATCHED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (wantSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .clickable {
                        onSelect(LibraryStatusFilter.TO_WATCH)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = if (wantSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = "Want to Watch",
                        fontWeight = if (wantSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Medium
                        },
                        color = if (wantSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        if (watchedSelected) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .clickable {
                        onSelect(LibraryStatusFilter.WATCHED)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = if (watchedSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    Text(
                        text = "Watched",
                        fontWeight = if (watchedSelected) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Medium
                        },
                        color = if (watchedSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

// =====================================================================
// SECTION HEADER
// =====================================================================

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =====================================================================
// MOVIE CARD
// =====================================================================

@Composable
private fun MovieCard(
    movie: WatchItem,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Movie,
                    contentDescription = null,
                    modifier = Modifier.size(29.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = buildString {
                        append(movie.status.label)

                        if (movie.yearDate.isNotBlank()) {
                            append(" • ")
                            append(movie.yearDate)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (movie.genre.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = movie.genre,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onFavorite
            ) {
                Icon(
                    imageVector = if (movie.favorite) {
                        Icons.Filled.Star
                    } else {
                        Icons.Outlined.StarOutline
                    },
                    contentDescription = "Favorite",
                    tint = if (movie.favorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// =====================================================================
// SERIES CARD
// =====================================================================

@Composable
private fun SeriesCard(
    series: SeriesEntity,
    progress: SeriesProgress?,
    onClick: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(29.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = series.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = buildString {
                            append(series.status.label)

                            if (series.yearDate.isNotBlank()) {
                                append(" • ")
                                append(series.yearDate)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                onEdit?.let {
                    IconButton(
                        onClick = it
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit"
                        )
                    }
                }

                onFavorite?.let {
                    IconButton(
                        onClick = it
                    ) {
                        Icon(
                            imageVector = if (series.favorite) {
                                Icons.Filled.Star
                            } else {
                                Icons.Outlined.StarOutline
                            },
                            contentDescription = "Favorite",
                            tint = if (series.favorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            if (progress != null) {
                Spacer(Modifier.height(13.dp))

                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${progress.watched}/${progress.total} episodes • ${progress.percentage}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =====================================================================
// COLLECTION CARD
// =====================================================================

@Composable
private fun CollectionCard(
    collection: CollectionEntity,
    progress: CollectionProgress?,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(15.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CollectionsBookmark,
                        contentDescription = null,
                        modifier = Modifier.size(29.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.width(13.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = collection.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    if (progress != null) {
                        Text(
                            text = "${progress.watched}/${progress.total} watched • ${progress.remaining} remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No movies added yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit"
                    )
                }
            }

            if (progress != null && progress.total > 0) {
                Spacer(Modifier.height(13.dp))

                LinearProgressIndicator(
                    progress = { progress.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${(progress.progress * 100).toInt()}% complete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =====================================================================
// EMPTY CARD
// =====================================================================

@Composable
private fun EmptyCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================================
// ADD CHOICE
// =====================================================================

@Composable
private fun AddChoice(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =====================================================================
// SWIPE DELETE
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteRow(
    onDelete: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit
) {
    var triggered by remember {
        mutableStateOf(false)
    }

    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (
                value == SwipeToDismissBoxValue.EndToStart &&
                !triggered
            ) {
                triggered = true
                onDelete()
            }

            false
        }
    )

    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissBoxValue.Settled) {
            triggered = false
        }
    }

    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        MaterialTheme.colorScheme.errorContainer
                    )
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = content
    )
}

// =====================================================================
// DELETE TARGET
// =====================================================================

private sealed class DeleteTarget(
    val title: String
) {
    class Movie(
        val movie: WatchItem
    ) : DeleteTarget(movie.title)

    class Series(
        val series: SeriesEntity
    ) : DeleteTarget(series.title)

    class Collection(
        val collection: CollectionEntity
    ) : DeleteTarget(collection.title)
}