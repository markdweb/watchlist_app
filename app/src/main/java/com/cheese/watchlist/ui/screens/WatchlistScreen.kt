package com.cheese.watchlist.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.entities.WatchType
import com.cheese.watchlist.data.preferences.OnboardingPreferences
import com.cheese.watchlist.ui.components.EmptyState
import com.cheese.watchlist.ui.components.FilterChipRow
import com.cheese.watchlist.ui.components.TypeChooser
import com.cheese.watchlist.ui.components.WatchlistCard
import com.cheese.watchlist.ui.components.WatchlistSearchBar
import com.cheese.watchlist.util.GreetingUtils
import com.cheese.watchlist.viewmodel.TypeFilter
import com.cheese.watchlist.viewmodel.WatchlistViewModel
import kotlinx.coroutines.launch

/** A single entry in the consolidated Status+Type filter row. */
private data class QuickFilter(
    val label: String,
    val isActive: (TypeFilter, WatchStatus?) -> Boolean,
    val onSelect: (WatchlistViewModel) -> Unit
)

private val quickFilters = listOf(
    QuickFilter("All", { type, status -> type == TypeFilter.ALL && status == null },
        { vm -> vm.setTypeFilter(TypeFilter.ALL); vm.setStatusFilter(null) }),
    QuickFilter("Movies", { type, _ -> type == TypeFilter.MOVIE },
        { vm -> vm.setTypeFilter(TypeFilter.MOVIE); vm.setStatusFilter(null) }),
    QuickFilter("Series", { type, _ -> type == TypeFilter.SERIES },
        { vm -> vm.setTypeFilter(TypeFilter.SERIES); vm.setStatusFilter(null) }),
    QuickFilter("To Watch", { _, status -> status == WatchStatus.TO_WATCH },
        { vm -> vm.setStatusFilter(WatchStatus.TO_WATCH); vm.setTypeFilter(TypeFilter.ALL) }),
    QuickFilter("Watching", { _, status -> status == WatchStatus.WATCHING },
        { vm -> vm.setStatusFilter(WatchStatus.WATCHING); vm.setTypeFilter(TypeFilter.ALL) }),
    QuickFilter("Watched", { _, status -> status == WatchStatus.WATCHED },
        { vm -> vm.setStatusFilter(WatchStatus.WATCHED); vm.setTypeFilter(TypeFilter.ALL) }),
    QuickFilter("Dropped", { _, status -> status == WatchStatus.DROPPED },
        { vm -> vm.setStatusFilter(WatchStatus.DROPPED); vm.setTypeFilter(TypeFilter.ALL) })
)

@Composable
fun WatchlistScreen(
    viewModel: WatchlistViewModel,
    onAddType: (WatchType) -> Unit,
    onOpenItem: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showTypeChooser by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        userName = OnboardingPreferences.getUserName(context)
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeToast()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportSelectedJson()
                try {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    Toast.makeText(context, "Exported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
                viewModel.clearSelection()
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isSelectionMode) {
                TopAppBar(
                    title = { Text("${uiState.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Outlined.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(onClick = { exportLauncher.launch("watchlist_selected.json") }) {
                            Icon(Icons.Outlined.SaveAlt, contentDescription = "Export selected")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(onClick = { showTypeChooser = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add title")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = if (userName.isNotBlank()) "Welcome, $userName!" else "Welcome!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${GreetingUtils.timeOfDayGreeting()} — ${GreetingUtils.motivationalLine()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                WatchlistSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::setSearchQuery
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Filter",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChipRow(
                        items = quickFilters,
                        labelFor = { it.label },
                        isSelected = { it.isActive(uiState.typeFilter, uiState.statusFilter) },
                        onSelect = { it.onSelect(viewModel) }
                    )
                }
            }

            item {
                val genreOptions = listOf("All") + uiState.genres.map { it.name }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Genre",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChipRow(
                        items = genreOptions,
                        labelFor = { it },
                        isSelected = { it == (uiState.genreFilter ?: "All") },
                        onSelect = { viewModel.setGenreFilter(if (it == "All") null else it) }
                    )
                }
            }

            if (uiState.favoriteItems.isNotEmpty()) {
                item {
                    Text("Favorites", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                items(uiState.favoriteItems, key = { "fav_${it.id}" }) { item ->
                    WatchlistCard(
                        item = item,
                        onClick = { onOpenItem(item.id) },
                        onDelete = { viewModel.deleteItem(item) },
                        onOpenUrl = { openUrl(context, item.url) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onIncrementEpisode = { viewModel.adjustEpisode(item, 1) },
                        onDecrementEpisode = { viewModel.adjustEpisode(item, -1) },
                        selectionMode = uiState.isSelectionMode,
                        isSelected = item.id in uiState.selectedIds,
                        onToggleSelect = { viewModel.toggleSelection(item.id) },
                        onLongPress = { viewModel.toggleSelection(item.id) }
                    )
                }
            }

            item {
                Text("All Titles", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
            }

            if (uiState.filteredItems.isEmpty()) {
                item {
                    EmptyState(onAddClick = { showTypeChooser = true })
                }
            } else {
                items(uiState.filteredItems, key = { it.id }) { item ->
                    WatchlistCard(
                        item = item,
                        onClick = { onOpenItem(item.id) },
                        onDelete = { viewModel.deleteItem(item) },
                        onOpenUrl = { openUrl(context, item.url) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onIncrementEpisode = { viewModel.adjustEpisode(item, 1) },
                        onDecrementEpisode = { viewModel.adjustEpisode(item, -1) },
                        selectionMode = uiState.isSelectionMode,
                        isSelected = item.id in uiState.selectedIds,
                        onToggleSelect = { viewModel.toggleSelection(item.id) },
                        onLongPress = { viewModel.toggleSelection(item.id) }
                    )
                }
            }
        }
    }

    if (showTypeChooser) {
        Dialog(onDismissRequest = { showTypeChooser = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            ) {
                TypeChooser(onSelect = {
                    showTypeChooser = false
                    onAddType(it)
                })
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    if (url.isBlank()) return
    val fixedUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl)))
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't open link", Toast.LENGTH_SHORT).show()
    }
}
