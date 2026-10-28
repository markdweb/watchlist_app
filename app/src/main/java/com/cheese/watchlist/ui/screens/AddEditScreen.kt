package com.cheese.watchlist.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.entities.WatchType
import com.cheese.watchlist.ui.components.GenreSelector
import com.cheese.watchlist.ui.components.RatingStars
import com.cheese.watchlist.ui.components.SourceSelector
import com.cheese.watchlist.ui.components.StatusSelector
import com.cheese.watchlist.ui.theme.StarYellow
import com.cheese.watchlist.viewmodel.WatchlistViewModel
import com.cheese.watchlist.viewmodel.genreList

@Composable
fun AddEditScreen(
    viewModel: WatchlistViewModel,
    itemId: Long?,
    initialType: WatchType,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isNew = itemId == null || itemId <= 0L

    var title by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(WatchStatus.TO_WATCH) }
    var type by remember { mutableStateOf(initialType) }
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    var whereToWatch by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var favorite by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf("") }
    var yearDate by remember { mutableStateOf("") }
    var totalEpisodes by remember { mutableStateOf("") }
    var currentEpisode by remember { mutableStateOf("") }
    var season by remember { mutableStateOf("1") }
    var loaded by remember { mutableStateOf(isNew) }

    LaunchedEffect(itemId) {
        if (!isNew && itemId != null) {
            val existing = viewModel.getItem(itemId)
            if (existing != null) {
                title = existing.title
                status = existing.status
                type = existing.type
                selectedGenres = existing.genreList().toSet()
                whereToWatch = existing.whereToWatch
                url = existing.url
                rating = existing.rating
                favorite = existing.favorite
                comments = existing.comments
                yearDate = existing.yearDate
                totalEpisodes = if (existing.totalEpisodes > 0) existing.totalEpisodes.toString() else ""
                currentEpisode = if (existing.currentEpisode > 0) existing.currentEpisode.toString() else ""
                season = if (existing.season > 0) existing.season.toString() else "1"
            }
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add Title" else "Edit Title") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (!loaded) return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            FormField(label = "Title") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
            }

            FormField(label = "Type") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WatchType.entries.forEach { t ->
                        com.cheese.watchlist.ui.components.WatchlistFilterChip(
                            label = if (t == WatchType.MOVIE) "Movie" else "Series",
                            selected = type == t,
                            onClick = { type = t }
                        )
                    }
                }
            }

            FormField(label = "Status") {
                StatusSelector(selected = status, onSelect = { status = it })
            }

            FormField(label = "Genre / Tags") {
                GenreSelector(
                    genres = uiState.genres,
                    selected = selectedGenres,
                    onToggle = { name ->
                        selectedGenres = if (name in selectedGenres) selectedGenres - name else selectedGenres + name
                    }
                )
            }

            FormField(label = "Where to Watch") {
                SourceSelector(
                    sources = uiState.sources,
                    selected = whereToWatch,
                    onSelect = { whereToWatch = it }
                )
            }

            AnimatedVisibility(
                visible = type == WatchType.SERIES,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    FormField(label = "Season") {
                        OutlinedTextField(
                            value = season,
                            onValueChange = { season = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 2") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )
                    }
                    FormField(label = "Total Episodes") {
                        OutlinedTextField(
                            value = totalEpisodes,
                            onValueChange = { totalEpisodes = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 30") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )
                    }
                    FormField(label = "Current Episode") {
                        OutlinedTextField(
                            value = currentEpisode,
                            onValueChange = { currentEpisode = it.filter { c -> c.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 12") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors()
                        )
                    }
                    val total = totalEpisodes.toIntOrNull() ?: 0
                    val current = (currentEpisode.toIntOrNull() ?: 0).coerceIn(0, if (total > 0) total else 0)
                    if (total > 0) {
                        com.cheese.watchlist.ui.components.EpisodeProgress(
                            currentEpisode = current,
                            totalEpisodes = total
                        )
                    }
                }
            }

            FormField(label = "URL") {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/watch") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
            }

            FormField(label = "Year / Date") {
                OutlinedTextField(
                    value = yearDate,
                    onValueChange = { yearDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. 2014") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
            }

            FormField(label = "Rating") {
                RatingStars(rating = rating, starSize = 26.dp, onRatingChange = { rating = it })
            }

            FormField(label = "Favorite") {
                Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    modifier = Modifier.clickable { favorite = !favorite }
                ) {
                    Icon(
                        imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Favorite",
                        tint = if (favorite) StarYellow else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    Text(if (favorite) "Marked as favorite" else "Mark as favorite", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            FormField(label = "Comments") {
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Notes, thoughts, reminders...") },
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors()
                )
            }

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val total = totalEpisodes.toIntOrNull() ?: 0
                    val current = (currentEpisode.toIntOrNull() ?: 0).coerceIn(0, if (total > 0) total else 0)
                    val seasonValue = (season.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val item = WatchItem(
                        id = itemId?.takeIf { it > 0 } ?: 0L,
                        title = title.trim(),
                        status = status,
                        type = type,
                        genre = selectedGenres.joinToString(", "),
                        whereToWatch = whereToWatch,
                        url = url.trim(),
                        rating = rating,
                        favorite = favorite,
                        comments = comments,
                        yearDate = yearDate,
                        season = if (type == WatchType.SERIES) seasonValue else 1,
                        totalEpisodes = if (type == WatchType.SERIES) total else 0,
                        currentEpisode = if (type == WatchType.SERIES) current else 0
                    )
                    viewModel.saveItem(item, isNew)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = title.isNotBlank()
            ) {
                Text(if (isNew) "Save" else "Update")
            }

            androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FormField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary
)
