package com.cheese.watchlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cheese.watchlist.data.entities.GenreEntity
import com.cheese.watchlist.data.entities.SourceEntity
import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.entities.WatchType
import com.cheese.watchlist.data.backup.BackupSerializer
import com.cheese.watchlist.data.repository.WatchlistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TypeFilter { ALL, MOVIE, SERIES }

data class WatchlistUiState(
    val items: List<WatchItem> = emptyList(),
    val genres: List<GenreEntity> = emptyList(),
    val sources: List<SourceEntity> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: WatchStatus? = null,
    val genreFilter: String? = null,
    val typeFilter: TypeFilter = TypeFilter.ALL,
    val favoritesOnly: Boolean = false,
    val toastMessage: String? = null,
    val selectedIds: Set<Long> = emptySet()
) {
    val isSelectionMode: Boolean get() = selectedIds.isNotEmpty()

    val filteredItems: List<WatchItem>
        get() = items.filter { item ->
            val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.genre.contains(searchQuery, ignoreCase = true) ||
                item.whereToWatch.contains(searchQuery, ignoreCase = true) ||
                item.type.name.contains(searchQuery, ignoreCase = true)
            val matchesStatus = statusFilter == null || item.status == statusFilter
            val matchesGenre = genreFilter == null || genreFilter == "All" ||
                item.genreList().any { it.equals(genreFilter, ignoreCase = true) }
            val matchesType = when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.MOVIE -> item.type == WatchType.MOVIE
                TypeFilter.SERIES -> item.type == WatchType.SERIES
            }
            matchesSearch && matchesStatus && matchesGenre && matchesType
        }

    val favoriteItems: List<WatchItem>
        get() = items.filter { it.favorite }
}

/** Splits the comma-separated genre string back into individual genre names. */
fun WatchItem.genreList(): List<String> =
    genre.split(",").map { it.trim() }.filter { it.isNotEmpty() }

class WatchlistViewModel(private val repository: WatchlistRepository) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<WatchStatus?>(null)
    private val genreFilter = MutableStateFlow<String?>(null)
    private val typeFilter = MutableStateFlow(TypeFilter.ALL)
    private val toastMessage = MutableStateFlow<String?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    private val filtersState = combine(
        searchQuery, statusFilter, genreFilter, typeFilter, toastMessage, selectedIds
    ) { values ->
        FiltersSnapshot(
            query = values[0] as String,
            status = values[1] as WatchStatus?,
            genre = values[2] as String?,
            type = values[3] as TypeFilter,
            toast = values[4] as String?,
            selected = values[5] as Set<Long>
        )
    }

    private data class FiltersSnapshot(
        val query: String,
        val status: WatchStatus?,
        val genre: String?,
        val type: TypeFilter,
        val toast: String?,
        val selected: Set<Long>
    )

    val uiState: StateFlow<WatchlistUiState> = combine(
        repository.allItems,
        repository.allGenres,
        repository.allSources,
        filtersState
    ) { items, genres, sources, filters ->
        WatchlistUiState(
            items = items,
            genres = genres,
            sources = sources,
            searchQuery = filters.query,
            statusFilter = filters.status,
            genreFilter = filters.genre,
            typeFilter = filters.type,
            toastMessage = filters.toast,
            selectedIds = filters.selected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WatchlistUiState())

    fun setSearchQuery(query: String) { searchQuery.value = query }
    fun setStatusFilter(status: WatchStatus?) { statusFilter.value = status }
    fun setGenreFilter(genre: String?) { genreFilter.value = genre }
    fun setTypeFilter(type: TypeFilter) { typeFilter.value = type }

    fun consumeToast() { toastMessage.value = null }

    suspend fun getItem(id: Long): WatchItem? = repository.getItem(id)

    fun saveItem(item: WatchItem, isNew: Boolean) {
        viewModelScope.launch {
            val toSave = item.copy(updatedAt = System.currentTimeMillis())
            repository.saveItem(toSave)
            toastMessage.value = if (isNew) "${item.title} saved" else "${item.title} updated"
        }
    }

    fun deleteItem(item: WatchItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            toastMessage.value = "${item.title} deleted"
        }
    }

    fun toggleFavorite(item: WatchItem) {
        viewModelScope.launch {
            repository.saveItem(item.copy(favorite = !item.favorite, updatedAt = System.currentTimeMillis()))
        }
    }

    /** Adjusts current episode by [delta], clamps to valid range, and auto-updates status. */
    fun adjustEpisode(item: WatchItem, delta: Int) {
        viewModelScope.launch {
            val newEpisode = (item.currentEpisode + delta).coerceIn(0, item.totalEpisodes)
            if (newEpisode == item.currentEpisode) return@launch

            var newStatus = item.status
            if (newEpisode == item.totalEpisodes && item.totalEpisodes > 0) {
                newStatus = WatchStatus.WATCHED
            } else if (newEpisode > 0 && item.status == WatchStatus.TO_WATCH) {
                newStatus = WatchStatus.WATCHING
            }

            val updated = item.copy(
                currentEpisode = newEpisode,
                status = newStatus,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveItem(updated)
            toastMessage.value = "Episode $newEpisode saved"
        }
    }

    fun addGenre(name: String) {
        viewModelScope.launch {
            val added = repository.addGenre(name)
            toastMessage.value = if (added) "Genre added" else "Genre already exists"
        }
    }

    fun deleteGenre(genre: GenreEntity) {
        viewModelScope.launch {
            repository.deleteGenre(genre)
            toastMessage.value = "Genre deleted"
        }
    }

    fun addSource(name: String) {
        viewModelScope.launch {
            val added = repository.addSource(name)
            toastMessage.value = if (added) "Source added" else "Source already exists"
        }
    }

    fun deleteSource(source: SourceEntity) {
        viewModelScope.launch {
            repository.deleteSource(source)
            toastMessage.value = "Source deleted"
        }
    }

    // ---------- multi-select ----------

    fun toggleSelection(id: Long) {
        val current = selectedIds.value
        selectedIds.value = if (id in current) current - id else current + id
    }

    fun selectAll() {
        selectedIds.value = uiState.value.filteredItems.map { it.id }.toSet()
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = selectedIds.value
            val toDelete = uiState.value.items.filter { it.id in ids }
            toDelete.forEach { repository.deleteItem(it) }
            toastMessage.value = "${toDelete.size} deleted"
            selectedIds.value = emptySet()
        }
    }

    // ---------- backup: export / import ----------

    suspend fun exportBackupJson(): String {
        val items = uiState.value.items
        val genres = uiState.value.genres
        val sources = uiState.value.sources
        return BackupSerializer.export(items, genres, sources)
    }

    suspend fun exportSelectedJson(): String {
        val ids = selectedIds.value
        val items = uiState.value.items.filter { it.id in ids }
        return BackupSerializer.export(items, uiState.value.genres, uiState.value.sources)
    }

    fun importBackupJson(json: String) {
        viewModelScope.launch {
            try {
                val parsed = BackupSerializer.import(json)
                parsed.genres.forEach { repository.addGenre(it) }
                parsed.sources.forEach { repository.addSource(it) }
                parsed.items.forEach { repository.saveItem(it.copy(id = 0L)) }
                toastMessage.value = "Imported ${parsed.items.size} titles"
            } catch (e: Exception) {
                toastMessage.value = "Import failed — invalid file"
            }
        }
    }
}

class WatchlistViewModelFactory(private val repository: WatchlistRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WatchlistViewModel::class.java)) {
            return WatchlistViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
