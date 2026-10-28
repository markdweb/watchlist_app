package com.cheese.watchlist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.data.entities.CollectionEntity
import com.cheese.watchlist.data.repository.CatalogRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionEditorScreen(
    catalog: CatalogRepository,
    collectionId: Long? = null,
    onDone: () -> Unit
) {
    val movies by catalog.allMovies.collectAsState(initial = emptyList())

    var loaded by remember { mutableStateOf(false) }
    var existing by remember { mutableStateOf<CollectionEntity?>(null) }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<List<Long>>(emptyList()) }
    var saving by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val isEdit = collectionId != null

    LaunchedEffect(collectionId) {
        if (collectionId != null) {
            val collection = catalog.getCollection(collectionId)
            existing = collection

            if (collection != null) {
                title = collection.title
                notes = collection.comments
                selected = catalog.collectionMovieIds(collection.id)
            }
        }

        loaded = true
    }

    if (!loaded) return

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEdit) "Edit Collection" else "New Collection",
                            fontWeight = FontWeight.SemiBold
                        )

                        if (title.isNotBlank()) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = 8.dp,
                    bottom = 18.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.12f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CollectionsBookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(25.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = if (isEdit) {
                                            "Update your collection"
                                        } else {
                                            "Create a collection"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = "Group movies together in your own order.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Collection name") },
                                placeholder = { Text("e.g. Wrong Turn") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notes") },
                                placeholder = { Text("Optional notes about this collection") },
                                minLines = 3,
                                maxLines = 5,
                                shape = RoundedCornerShape(16.dp)
                            )
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 14.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Movies in this collection",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = if (selected.isEmpty()) {
                                        "No movies selected yet"
                                    } else {
                                        "${selected.size} movie${if (selected.size == 1) "" else "s"} selected"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (selected.isNotEmpty()) {
                                Text(
                                    text = "${selected.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Choose movies",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = "Tap a movie to add or remove it. Selected movies follow the order you add them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (movies.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Movie,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    text = "No movies available",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = "Add some movies to your watchlist first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(
                        items = movies,
                        key = { it.id }
                    ) { movie ->

                        val index = selected.indexOf(movie.id)
                        val checked = index >= 0

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    selected =
                                        if (checked) {
                                            selected - movie.id
                                        } else {
                                            selected + movie.id
                                        }
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = if (checked) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = if (checked) 1.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 13.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                if (checked) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(
                                                MaterialTheme.colorScheme.primary
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Movie,
                                            contentDescription = null,
                                            modifier = Modifier.size(21.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = movie.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = if (checked) {
                                            FontWeight.SemiBold
                                        } else {
                                            FontWeight.Medium
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = if (checked) {
                                            "Watch order #${index + 1} • ${movie.status.label}"
                                        } else {
                                            movie.status.label
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (checked) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = {
                                            selected = selected + movie.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 18.dp,
                        end = 18.dp,
                        top = 12.dp,
                        bottom = 18.dp
                    )
                ) {

                    if (selected.isNotEmpty()) {
                        Text(
                            text = "${selected.size} movie${if (selected.size == 1) "" else "s"} will be added",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        enabled = title.isNotBlank() && !saving,
                        onClick = {
                            scope.launch {
                                saving = true

                                val id = existing?.id
                                    ?: catalog.createCollection(
                                        title,
                                        notes
                                    )

                                val entity =
                                    (existing
                                        ?: CollectionEntity(
                                            id = id,
                                            title = title
                                        )
                                            ).copy(
                                            title = title.trim(),
                                            comments = notes.trim()
                                        )

                                catalog.updateCollection(entity)
                                catalog.setCollectionMovies(id, selected)

                                saving = false
                                onDone()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(17.dp),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = when {
                                saving -> "Saving…"
                                isEdit -> "Update Collection"
                                else -> "Create Collection"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}