package com.cheese.watchlist.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.SeriesEntity
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.ui.components.RatingStars
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SeriesEditorScreen(catalog: CatalogRepository, seriesId: Long? = null, onDone: () -> Unit) {
    val genres by catalog.allGenres.collectAsState(initial = emptyList())
    val tags by catalog.allTags.collectAsState(initial = emptyList())
    val sources by catalog.allSources.collectAsState(initial = emptyList())
    var loaded by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf<SeriesEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0) }
    var favorite by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(WatchStatus.TO_WATCH) }
    var selectedGenres by remember { mutableStateOf<Set<String>>(emptySet()) }
    var tagText by remember { mutableStateOf("") }
    var seasonCounts by remember { mutableStateOf("6,13,16") }
    var notes by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(seriesId) {
        if (seriesId != null) {
            val s = catalog.getSeries(seriesId)
            existing = s
            if (s != null) {
                title = s.title; year = s.yearDate; source = s.whereToWatch; url = s.url; rating = s.rating
                favorite = s.favorite; status = s.status; notes = s.comments
                selectedGenres = catalog.seriesGenres(seriesId).map { it.name }.toSet()
                tagText = catalog.itemTags(seriesId, com.cheese.watchlist.data.entities.TaggableType.SERIES).joinToString(", ") { it.name }
                val seasonList = catalog.observeSeasons(seriesId).first()
                seasonCounts = seasonList.map { catalog.getEpisodes(it.id).size }.joinToString(",")
            }
        }
        loaded = true
    }

    if (!loaded) return

    val isEdit = seriesId != null
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (isEdit) "Edit Series" else "Add Series") }, navigationIcon = {
            IconButton(onClick = onDone) { Icon(Icons.Outlined.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            Text("Type: Series", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(year, { year = it.filter { c -> c.isDigit() } }, Modifier.fillMaxWidth(), label = { Text("Year") }, singleLine = true)

            Text("Genre", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                genres.forEach { g -> FilterChip(g.name in selectedGenres, { selectedGenres = if (g.name in selectedGenres) selectedGenres - g.name else selectedGenres + g.name }, label = { Text(g.name) }) }
            }

            OutlinedTextField(tagText, { tagText = it }, Modifier.fillMaxWidth(), label = { Text("Tags") }, placeholder = { Text("anime, favorite, rewatch") }, singleLine = true)

            Text("Where to watch / Source", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sources.forEach { s -> FilterChip(source.equals(s.name, true), { source = s.name }, label = { Text(s.name) }) }
            }
            OutlinedTextField(source, { source = it }, Modifier.fillMaxWidth(), label = { Text("Source name") }, singleLine = true)
            OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("URL") }, singleLine = true)

            Text("Rating", style = MaterialTheme.typography.titleMedium)
            RatingStars(rating = rating, onRatingChange = { rating = it }, starSize = 26.dp)

            Text("Favorite", style = MaterialTheme.typography.titleMedium)
            FilterChip(favorite, { favorite = !favorite }, label = { Text(if (favorite) "★ Favorite" else "☆ Favorite") })

            Text("Status", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WatchStatus.entries.forEach { st -> FilterChip(status == st, { status = st }, label = { Text(st.label) }) }
            }

            Text("Episodes per season", style = MaterialTheme.typography.titleMedium)
            Text("For a new series: 6,13,16 creates 3 seasons. When editing, increasing a count adds missing episodes without resetting watched episodes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(seasonCounts, { seasonCounts = it.filter { c -> c.isDigit() || c == ',' } }, Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 3)

            Button(
                enabled = title.isNotBlank() && !saving,
                onClick = {
                    scope.launch {
                        saving = true
                        val entity = (existing ?: SeriesEntity(title = title.trim())).copy(
                            title = title.trim(), status = status, whereToWatch = source.trim(), url = url.trim(), rating = rating,
                            favorite = favorite, comments = notes.trim(), yearDate = year.trim()
                        )
                        val id = catalog.saveSeries(entity, selectedGenres, tagText.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet(), source)
                        val desired = seasonCounts.split(",").mapNotNull { it.toIntOrNull() }.filter { it > 0 }
                        val currentSeasons = catalog.observeSeasons(id).first()
                        desired.forEachIndexed { index, count ->
                            val seasonNumber = index + 1
                            val season = currentSeasons.firstOrNull { it.seasonNumber == seasonNumber }
                            val seasonId = season?.id ?: catalog.addSeason(id, seasonNumber)
                            val existingCount = catalog.getEpisodes(seasonId).size
                            if (count > existingCount) catalog.addEpisodes(seasonId, count - existingCount, existingCount + 1)
                        }
                        saving = false
                        onDone()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (saving) "Saving…" else if (isEdit) "Update Series" else "Create Series") }
            Spacer(Modifier.height(20.dp))
        }
    }
}
