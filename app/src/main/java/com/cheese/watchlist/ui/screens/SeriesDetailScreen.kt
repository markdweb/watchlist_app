package com.cheese.watchlist.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.*
import com.cheese.watchlist.data.repository.SeriesProgress
import com.cheese.watchlist.viewmodel.LibraryViewModel

@Composable
fun SeriesDetailScreen(
    seriesId: Long,
    viewModel: LibraryViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit
) {
    val progress by viewModel.observeSeries(seriesId).collectAsState(initial = null)
    var expanded by remember { mutableStateOf<Long?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var metadata by remember { mutableStateOf<com.cheese.watchlist.data.repository.SeriesMetadata?>(null) }
    val context = LocalContext.current

    LaunchedEffect(seriesId) { metadata = viewModel.seriesMetadata(seriesId) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(progress?.series?.title ?: "Series") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
            actions = {
                progress?.series?.let { s ->
                    IconButton(onClick = { onEdit(s.id) }) { Icon(Icons.Outlined.Edit, "Edit") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") }
                    IconButton(onClick = { viewModel.toggleSeriesFavorite(s) }) { Icon(if (s.favorite) Icons.Filled.Star else Icons.Outlined.StarOutline, "Favorite") }
                }
            }
        )
    }) { padding ->
        if (progress == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 1.dp) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Series", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(5.dp))
                            Text(progress!!.series.title, style = MaterialTheme.typography.headlineSmall)
                            Text("Status: ${progress!!.series.status.label}${if (progress!!.series.yearDate.isNotBlank()) " • ${progress!!.series.yearDate}" else ""}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            metadata?.genres?.takeIf { it.isNotEmpty() }?.let { Text("Genres: ${it.joinToString { g -> g.name }}") }
                            metadata?.tags?.takeIf { it.isNotEmpty() }?.let { Text("Tags: ${it.joinToString { t -> t.name }}") }
                            if (progress!!.series.whereToWatch.isNotBlank()) {
                                Text("Source: ${progress!!.series.whereToWatch}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (progress!!.series.rating > 0) {
                                Spacer(Modifier.height(4.dp))
                                Text("Rating: ${progress!!.series.rating}/5", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (progress!!.series.url.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val raw = progress!!.series.url.trim()
                                            val url = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Couldn't open link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Outlined.OpenInNew, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open Watch Link")
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("${progress!!.watched}/${progress!!.total} episodes", style = MaterialTheme.typography.headlineSmall)
                            Text("${progress!!.percentage}% complete • ${progress!!.remaining} remaining", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { progress!!.progress }, Modifier.fillMaxWidth())
                            progress!!.nextEpisode?.let { ep -> Spacer(Modifier.height(10.dp)); Text("Next: Episode ${ep.episodeNumber}${ep.title?.let { " • $it" } ?: ""}") }
                        }
                    }
                }
                items(progress!!.seasons, key = { it.season.id }) { season ->
                    SeasonExpandable(season, expanded == season.season.id, { expanded = if (expanded == season.season.id) null else season.season.id }, viewModel, seriesId)
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false }, shape = RoundedCornerShape(28.dp), icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete this series?") }, text = { Text("\"${progress?.series?.title.orEmpty()}\" will be removed. Its seasons and episodes will also be deleted.") },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
            confirmButton = { Button(onClick = { showDelete = false; viewModel.deleteSeries(seriesId); onDelete() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } }
        )
    }
}

@Composable
private fun SeasonExpandable(season: com.cheese.watchlist.data.repository.SeasonProgress, expanded: Boolean, onToggle: () -> Unit, viewModel: LibraryViewModel, seriesId: Long) {
    val episodeList by viewModel.observeEpisodes(season.season.id).collectAsState(initial = emptyList())
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp) {
        Column {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(season.season.title ?: "Season ${season.season.seasonNumber}", style = MaterialTheme.typography.titleMedium)
                    Text("${season.watched}/${season.total} watched", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(season.progress * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
            }
            if (expanded) {
                HorizontalDivider()
                episodeList.forEach { ep ->
                    Row(Modifier.fillMaxWidth().clickable { viewModel.toggleEpisode(ep.id, !ep.watched) }.padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (ep.watched) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked, null, tint = if (ep.watched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) {
                            Text("Episode ${ep.episodeNumber}"); ep.title?.let { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }
    }
}
