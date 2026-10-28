package com.cheese.watchlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cheese.watchlist.data.entities.*
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.data.repository.CollectionProgress
import com.cheese.watchlist.data.repository.SeriesProgress
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class LibraryTypeFilter { ALL, MOVIES, SERIES, COLLECTIONS }
enum class LibraryStatusFilter { ALL, TO_WATCH, WATCHING, WATCHED, FAVORITES }

data class LibraryUiState(
    val movies: List<WatchItem> = emptyList(),
    val series: List<SeriesEntity> = emptyList(),
    val collections: List<CollectionEntity> = emptyList(),
    val genres: List<GenreEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val sources: List<SourceEntity> = emptyList(),
    val seriesProgress: Map<Long, SeriesProgress> = emptyMap(),
    val collectionProgress: Map<Long, CollectionProgress> = emptyMap(),
    val query: String = "",
    val typeFilter: LibraryTypeFilter = LibraryTypeFilter.ALL,
    val statusFilter: LibraryStatusFilter = LibraryStatusFilter.ALL,
    val genreFilter: String? = null,
    val sort: LibrarySort = LibrarySort.RECENTLY_UPDATED
) {
    val filteredMovies: List<WatchItem>
        get() = movies.filter { movie ->
            (query.isBlank() || movie.title.contains(query, true) || movie.genre.contains(query, true) ||
                movie.whereToWatch.contains(query, true) || movie.comments.contains(query, true)) &&
            (statusFilter == LibraryStatusFilter.ALL ||
                (statusFilter == LibraryStatusFilter.FAVORITES && movie.favorite) ||
                statusFilter.toWatchStatus()?.let { movie.status == it } == true) &&
            (genreFilter == null || movie.genre.split(",").any { it.trim().equals(genreFilter, true) })
        }.let { it.sortedWith(LibrarySort.comparator(sort)) }

    val filteredSeries: List<SeriesEntity>
        get() = series.filter { s ->
            (query.isBlank() || s.title.contains(query, true) || s.whereToWatch.contains(query, true) || s.comments.contains(query, true)) &&
            (statusFilter == LibraryStatusFilter.ALL ||
                (statusFilter == LibraryStatusFilter.FAVORITES && s.favorite) ||
                statusFilter.toWatchStatus()?.let { s.status == it } == true)
        }.let { it.sortedWith(LibrarySort.comparatorSeries(sort)) }

    val filteredCollections: List<CollectionEntity>
        get() = collections.filter { query.isBlank() || it.title.contains(query, true) || it.comments.contains(query, true) }
            .sortedWith(LibrarySort.comparatorCollections(sort))

    private fun LibraryStatusFilter.toWatchStatus() = when (this) {
        LibraryStatusFilter.TO_WATCH -> WatchStatus.TO_WATCH
        LibraryStatusFilter.WATCHING -> WatchStatus.WATCHING
        LibraryStatusFilter.WATCHED -> WatchStatus.WATCHED
        else -> null
    }
}

enum class LibrarySort(val label: String) {
    RECENTLY_UPDATED("Recently Updated"),
    RECENTLY_ADDED("Recently Added"),
    AZ("A–Z"),
    ZA("Z–A"),
    HIGHEST_RATED("Highest Rated"),
    LOWEST_RATED("Lowest Rated"),
    FAVORITES_FIRST("Favorites First"),
    RELEASE_YEAR_NEWEST("Release Year — Newest"),
    RELEASE_YEAR_OLDEST("Release Year — Oldest");

    companion object {
        fun comparator(sort: LibrarySort): Comparator<WatchItem> = when (sort) {
            RECENTLY_UPDATED -> compareByDescending { it.updatedAt }
            RECENTLY_ADDED -> compareByDescending { it.createdAt }
            AZ -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            ZA -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
            HIGHEST_RATED -> compareByDescending<WatchItem> { it.rating }.thenBy { it.title.lowercase() }
            LOWEST_RATED -> compareBy<WatchItem> { it.rating }.thenBy { it.title.lowercase() }
            FAVORITES_FIRST -> compareByDescending<WatchItem> { it.favorite }.thenByDescending { it.updatedAt }
            RELEASE_YEAR_NEWEST -> compareByDescending<WatchItem> { it.yearDate.toIntOrNull() ?: -1 }.thenBy { it.title.lowercase() }
            RELEASE_YEAR_OLDEST -> compareBy<WatchItem> { it.yearDate.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }
        }
        fun comparatorSeries(sort: LibrarySort): Comparator<SeriesEntity> = when (sort) {
            RECENTLY_UPDATED -> compareByDescending { it.updatedAt }
            RECENTLY_ADDED -> compareByDescending { it.createdAt }
            AZ -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            ZA -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
            HIGHEST_RATED -> compareByDescending<SeriesEntity> { it.rating }.thenBy { it.title.lowercase() }
            LOWEST_RATED -> compareBy<SeriesEntity> { it.rating }.thenBy { it.title.lowercase() }
            FAVORITES_FIRST -> compareByDescending<SeriesEntity> { it.favorite }.thenByDescending { it.updatedAt }
            RELEASE_YEAR_NEWEST -> compareByDescending<SeriesEntity> { it.yearDate.toIntOrNull() ?: -1 }.thenBy { it.title.lowercase() }
            RELEASE_YEAR_OLDEST -> compareBy<SeriesEntity> { it.yearDate.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() }
        }
        fun comparatorCollections(sort: LibrarySort): Comparator<CollectionEntity> = when (sort) {
            RECENTLY_UPDATED -> compareByDescending { it.updatedAt }
            RECENTLY_ADDED -> compareByDescending { it.createdAt }
            AZ, HIGHEST_RATED, LOWEST_RATED, FAVORITES_FIRST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            ZA -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.title }
            RELEASE_YEAR_NEWEST, RELEASE_YEAR_OLDEST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        }
    }
}

class LibraryViewModel(private val catalog: CatalogRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val type = MutableStateFlow(LibraryTypeFilter.ALL)
    private val status = MutableStateFlow(LibraryStatusFilter.ALL)
    private val genre = MutableStateFlow<String?>(null)
    private val sort = MutableStateFlow(LibrarySort.RECENTLY_UPDATED)
    private val progress = MutableStateFlow<Map<Long, SeriesProgress>>(emptyMap())
    private val collectionProgress = MutableStateFlow<Map<Long, CollectionProgress>>(emptyMap())

    val uiState: StateFlow<LibraryUiState> = combine(
        catalog.allMovies, catalog.allSeries, catalog.allCollections,
        catalog.allGenres, catalog.allTags, catalog.allSources,
        query, type, status, genre, sort, progress, collectionProgress
    ) { values ->
        LibraryUiState(
            movies = values[0] as List<WatchItem>,
            series = values[1] as List<SeriesEntity>,
            collections = values[2] as List<CollectionEntity>,
            genres = values[3] as List<GenreEntity>,
            tags = values[4] as List<TagEntity>,
            sources = values[5] as List<SourceEntity>,
            query = values[6] as String,
            typeFilter = values[7] as LibraryTypeFilter,
            statusFilter = values[8] as LibraryStatusFilter,
            genreFilter = values[9] as String?,
            sort = values[10] as LibrarySort,
            seriesProgress = values[11] as Map<Long, SeriesProgress>,
            collectionProgress = values[12] as Map<Long, CollectionProgress>
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LibraryUiState())

    init {
        viewModelScope.launch {
            combine(catalog.allSeries, catalog.allMovies, catalog.allCollections) { s, _, c -> s to c }
                .collectLatest { (series, collections) ->
                    val p = series.mapNotNull { catalog.seriesProgress(it.id)?.let { x -> it.id to x } }.toMap()
                    val cp = collections.mapNotNull { catalog.collectionProgress(it.id)?.let { x -> it.id to x } }.toMap()
                    progress.value = p
                    collectionProgress.value = cp
                }
        }
    }

    fun setQuery(v: String) { query.value = v }
    fun setType(v: LibraryTypeFilter) { type.value = v }
    fun setStatus(v: LibraryStatusFilter) { status.value = v }
    fun setGenre(v: String?) { genre.value = v }
    fun setSort(v: LibrarySort) { sort.value = v }
    fun clearFilters() { query.value = ""; type.value = LibraryTypeFilter.ALL; status.value = LibraryStatusFilter.ALL; genre.value = null }
    fun toggleMovieFavorite(movie: WatchItem) = viewModelScope.launch {
        catalog.saveMovie(movie.copy(favorite = !movie.favorite))
    }
    fun toggleSeriesFavorite(series: SeriesEntity) = viewModelScope.launch {
        catalog.saveSeries(series.copy(favorite = !series.favorite))
    }
    fun deleteMovie(id: Long) = viewModelScope.launch { catalog.deleteMovie(id) }
    fun deleteSeries(id: Long) = viewModelScope.launch { catalog.deleteSeries(id) }
    fun toggleEpisode(id: Long, watched: Boolean) = viewModelScope.launch { catalog.setEpisodeWatched(id, watched) }
    fun resumeFrom(seriesId: Long, episodeId: Long?) = viewModelScope.launch { catalog.resumeFrom(seriesId, episodeId) }
    fun deleteCollection(id: Long) = viewModelScope.launch { catalog.deleteCollection(id) }
    suspend fun collection(id: Long) = catalog.getCollection(id)
    suspend fun collectionMovieIds(id: Long) = catalog.collectionMovieIds(id)
    suspend fun updateCollection(collection: CollectionEntity, movieIds: List<Long>) {
        catalog.updateCollection(collection)
        catalog.setCollectionMovies(collection.id, movieIds)
    }

    suspend fun stats() = catalog.stats()
    suspend fun series(id: Long) = catalog.seriesProgress(id)
    fun observeSeries(id: Long) = catalog.observeSeriesProgress(id)
    suspend fun seriesMetadata(id: Long) = catalog.seriesMetadata(id)
    suspend fun episodes(seasonId: Long) = catalog.getEpisodes(seasonId)
    fun seasons(seriesId: Long) = catalog.observeSeasons(seriesId)
    fun observeEpisodes(seasonId: Long) = catalog.observeEpisodes(seasonId)

    class Factory(private val catalog: CatalogRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) LibraryViewModel(catalog) as T
            else error("Unknown ViewModel: $modelClass")
    }
}
