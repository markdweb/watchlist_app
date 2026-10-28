package com.cheese.watchlist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cheese.watchlist.data.entities.SeriesEntity
import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.data.tmdb.TmdbApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class DiscoveryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val trending: List<TmdbApi.Item> = emptyList(),
    val newReleases: List<TmdbApi.Item> = emptyList(),
    val upcoming: List<TmdbApi.Item> = emptyList(),

    val searchResults: List<TmdbApi.Item> = emptyList(),
    val searchQuery: String = "",
    val addingKey: String? = null,
    val addedKeys: Set<String> = emptySet(),
)

class DiscoveryViewModel(
    private val api: TmdbApi,
    private val catalog: CatalogRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoveryUiState())
    val state: StateFlow<DiscoveryUiState> = _state.asStateFlow()

    private var searchJob: Job? = null


    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)

            runCatching {
                val trending = api.trending()
                val now = api.nowPlaying()
                val upcoming = api.upcoming()

                _state.value = _state.value.copy(
                    loading = false,
                    trending = trending,
                    newReleases = now,
                    upcoming = upcoming,

                    error = null
                )

            }.onFailure {
                _state.value = _state.value.copy(
                    loading = false,
                    error = it.message ?: "Unable to load TMDB"
                )
            }
        }
    }


    fun search(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        searchJob?.cancel()
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(350)
            runCatching { api.search(cleanQuery) }
                .onSuccess { results ->
                    _state.value = _state.value.copy(
                        searchResults = results.distinctBy { "${it.mediaType}_${it.id}" },
                        error = null
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        searchResults = emptyList(),
                        error = error.message ?: "Search failed"
                    )
                }
        }
    }

    fun addToWatchlist(item: TmdbApi.Item) {
        val key = "${item.mediaType}:${item.id}"
        if (_state.value.addedKeys.contains(key)) return

        viewModelScope.launch {
            _state.value = _state.value.copy(addingKey = key)
            runCatching {
                val details = api.details(item)
                if (item.mediaType == "movie") {
                    catalog.saveMovie(
                        WatchItem(
                            title = details.item.title,
                            status = WatchStatus.TO_WATCH,
                            url = details.item.tmdbUrl,
                            rating = details.item.rating.toInt(),
                            yearDate = details.item.year,
                            tmdbId = details.item.id,
                            posterPath = details.item.posterPath,
                            backdropPath = details.item.backdropPath
                        ),
                        genreNames = details.item.genres.toSet()
                    )
                } else {
                    val id = catalog.saveSeries(
                        SeriesEntity(
                            title = details.item.title,
                            status = WatchStatus.TO_WATCH,
                            url = details.item.tmdbUrl,
                            rating = details.item.rating.toInt(),
                            yearDate = details.item.year
                        ),
                        genreNames = details.item.genres.toSet()
                    )
                    for (season in details.seasons) {
                        val seasonId = catalog.addSeason(id, season.number, season.name)
                        catalog.addEpisodes(seasonId, season.episodeCount)
                    }
                }
            }.onSuccess {
                _state.value = _state.value.copy(
                    addingKey = null,
                    addedKeys = _state.value.addedKeys + key
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    addingKey = null,
                    error = it.message ?: "Could not add item"
                )
            }
        }
    }

    class Factory(
        private val api: TmdbApi,
        private val catalog: CatalogRepository
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DiscoveryViewModel(api, catalog) as T
    }
}
