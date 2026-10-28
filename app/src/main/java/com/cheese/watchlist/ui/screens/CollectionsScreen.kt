package com.cheese.watchlist.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.CollectionEntity
import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.repository.CatalogRepository
import kotlinx.coroutines.launch

@Composable
fun CollectionsScreen(
    catalog: CatalogRepository,
    collectionId: Long? = null,
    onBack: () -> Unit,
    onMovie: (Long) -> Unit,
    onCollection: (Long) -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    if (collectionId == null) CollectionsList(catalog, onBack, onCollection, onEdit)
    else CollectionDetail(catalog, collectionId, onBack, onMovie, onEdit, onDelete)
}

@Composable
private fun CollectionsList(catalog: CatalogRepository, onBack: () -> Unit, onCollection: (Long) -> Unit, onEdit: (Long) -> Unit) {
    val collections by catalog.allCollections.collectAsState(initial = emptyList())
    Scaffold(topBar = { TopAppBar(title = { Text("Collections") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(collections, key = { _, it -> it.id }) { _, c ->
                val movies by catalog.observeCollectionMovies(c.id).collectAsState(initial = emptyList())
                CollectionRow(c, movies, { onCollection(c.id) }, { onEdit(c.id) })
            }
        }
    }
}

@Composable
private fun CollectionRow(c: CollectionEntity, movies: List<WatchItem>, onClick: () -> Unit, onEdit: () -> Unit) {
    val watched = movies.count { it.status == WatchStatus.WATCHED }
    Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CollectionsBookmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)); Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(c.title, style = MaterialTheme.typography.titleMedium); Text("$watched/${movies.size} watched • ${(if (movies.isEmpty()) 0 else watched * 100 / movies.size)}%", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Edit") }
        }
    }
}

@Composable
private fun CollectionDetail(catalog: CatalogRepository, id: Long, onBack: () -> Unit, onMovie: (Long) -> Unit, onEdit: (Long) -> Unit, onDelete: () -> Unit) {
    val collections by catalog.allCollections.collectAsState(initial = emptyList())
    val collection = collections.firstOrNull { it.id == id }
    val movies by catalog.observeCollectionMovies(id).collectAsState(initial = emptyList())
    val watched = movies.count { it.status == WatchStatus.WATCHED }
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text(collection?.title ?: "Collection") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }, actions = {
            IconButton(onClick = { onEdit(id) }) { Icon(Icons.Outlined.Edit, "Edit") }
            IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, "Delete") }
        })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
                    Column(Modifier.padding(18.dp)) {
                        Text("${watched}/${movies.size} watched", style = MaterialTheme.typography.headlineSmall)
                        Text("${if (movies.isEmpty()) 0 else watched * 100 / movies.size}% complete", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        collection?.comments?.takeIf { it.isNotBlank() }?.let { Text(it) }
                        Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { if (movies.isEmpty()) 0f else watched.toFloat() / movies.size }, Modifier.fillMaxWidth())
                        movies.firstOrNull { it.status != WatchStatus.WATCHED }?.let { Text("Next: ${it.title}", modifier = Modifier.padding(top = 10.dp)) }
                    }
                }
            }
            itemsIndexed(movies, key = { _, it -> it.id }) { index, movie ->
                Row(Modifier.fillMaxWidth().clickable { onMovie(movie.id) }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", Modifier.width(36.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Column(Modifier.weight(1f)) { Text(movie.title, style = MaterialTheme.typography.titleMedium); Text(movie.status.label, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (movie.status == WatchStatus.WATCHED) Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    val scope = rememberCoroutineScope()

    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false }, shape = RoundedCornerShape(28.dp), icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Delete collection?") }, text = { Text("\"${collection?.title.orEmpty()}\" will be deleted, but the movies inside it will remain in your library.") },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        confirmButton = { Button(onClick = {
                showDelete = false
                scope.launch {
                    catalog.deleteCollection(id)
                    onDelete()
                }
            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } }
    )
}

