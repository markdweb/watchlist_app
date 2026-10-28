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
import kotlinx.coroutines.launch

class TmdbDetailViewModel(private val api: TmdbApi) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val item: TmdbApi.Item? = null,
        val error: String? = null,
        val trailerUrl: String? = null,
        val stills: List<String> = emptyList(),
        val similar: List<TmdbApi.Item> = emptyList(),
        val providers: List<String> = emptyList(),
        val country: String = "",
        val language: String = "",
        val director: String = "",
        val writers: List<String> = emptyList(),
        val cast: List<String> = emptyList()
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(id: Int, mediaType: String) {
        viewModelScope.launch {
            _state.value = UiState(loading = true)

            runCatching {
                api.details(id, mediaType)
            }
                .onSuccess { d ->
                    _state.value = UiState(
                        item = d.item,
                        trailerUrl = d.trailerUrl,
                        stills = d.stills,
                        similar = d.similar,
                        providers = d.providers,
                        country = d.country,
                        language = d.language,
                        director = d.director,
                        writers = d.writers,
                        cast = d.cast
                    )
                }
                .onFailure {
                    _state.value = UiState(
                        error = it.message ?: "Unable to load title"
                    )
                }
        }
    }

    fun addToLibrary(
        catalog: CatalogRepository,
        status: WatchStatus,
        onDone: () -> Unit,
        onDuplicate: () -> Unit = {}
    ) {
        val item = _state.value.item ?: return

        viewModelScope.launch {
            runCatching {
                val details = api.details(item)

                val title = details.item.title.trim()
                val tmdbUrl = details.item.tmdbUrl

                if (item.mediaType == "movie") {

                    val existingByTmdbId =
                        catalog.getMovieByTmdbId(details.item.id)

                    val existingByTitle =
                        catalog.getMovieByTitle(title)

                    if (existingByTmdbId != null || existingByTitle != null) {
                        return@runCatching false
                    }

                    catalog.saveMovie(
                        WatchItem(
                            title = title,
                            status = status,
                            url = tmdbUrl,
                            rating = details.item.rating.toInt(),
                            yearDate = details.item.year,
                            tmdbId = details.item.id,
                            posterPath = details.item.posterPath,
                            backdropPath = details.item.backdropPath
                        ),
                        genreNames = details.item.genres.toSet()
                    )

                } else {

                    val existingByUrl =
                        catalog.getSeriesByUrl(tmdbUrl)

                    val existingByTitle =
                        catalog.getSeriesByTitle(title)

                    if (existingByUrl != null || existingByTitle != null) {
                        return@runCatching false
                    }

                    val seriesId = catalog.saveSeries(
                        SeriesEntity(
                            title = title,
                            status = status,
                            url = tmdbUrl,
                            rating = details.item.rating.toInt(),
                            yearDate = details.item.year
                        ),
                        genreNames = details.item.genres.toSet()
                    )

                    for (season in details.seasons) {
                        val seasonId = catalog.addSeason(
                            seriesId,
                            season.number,
                            season.name
                        )

                        catalog.addEpisodes(
                            seasonId,
                            season.episodeCount
                        )
                    }
                }

                true

            }.onSuccess { added ->
                if (added) {
                    onDone()
                } else {
                    onDuplicate()
                }
            }
        }
    }

    /**
     * Save or update the custom website/watch URL for an existing
     * movie or series.
     *
     * The full URL is stored exactly as entered.
     */
    fun saveWatchUrl(
        catalog: CatalogRepository,
        url: String,
        onDone: () -> Unit,
        onNotFound: () -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val item = _state.value.item ?: return

        val cleanUrl = url.trim()

        if (cleanUrl.isBlank()) {
            onError(IllegalArgumentException("Website address cannot be empty."))
            return
        }

        viewModelScope.launch {
            runCatching {
                if (item.mediaType == "movie") {

                    val existing =
                        catalog.getMovieByTmdbId(item.id)
                            ?: catalog.getMovieByTitle(item.title.trim())

                    if (existing == null) {
                        false
                    } else {
                        catalog.setMovieWatchUrl(
                            movieId = existing.id,
                            watchUrl = cleanUrl
                        )
                        true
                    }

                } else {

                    val existing =
                        catalog.getSeriesByUrl(item.tmdbUrl)
                            ?: catalog.getSeriesByTitle(item.title.trim())

                    if (existing == null) {
                        false
                    } else {
                        catalog.setSeriesWatchUrl(
                            seriesId = existing.id,
                            watchUrl = cleanUrl
                        )
                        true
                    }
                }
            }
                .onSuccess { saved ->
                    if (saved) {
                        onDone()
                    } else {
                        onNotFound()
                    }
                }
                .onFailure(onError)
        }
    }

    /**
     * Mark an existing movie/series as watched.
     * This changes the existing database row instead of deleting/re-adding it.
     */
    fun markAsWatched(
        catalog: CatalogRepository,
        onDone: () -> Unit,
        onNotFound: () -> Unit = {}
    ) {
        val item = _state.value.item ?: return

        viewModelScope.launch {
            runCatching {
                if (item.mediaType == "movie") {
                    val existing =
                        catalog.getMovieByTmdbId(item.id)
                            ?: catalog.getMovieByTitle(item.title.trim())

                    if (existing == null) {
                        false
                    } else {
                        catalog.setMovieStatus(
                            movieId = existing.id,
                            status = WatchStatus.WATCHED
                        )
                        true
                    }
                } else {
                    val existing =
                        catalog.getSeriesByUrl(item.tmdbUrl)
                            ?: catalog.getSeriesByTitle(item.title.trim())

                    if (existing == null) {
                        false
                    } else {
                        catalog.setSeriesStatus(
                            seriesId = existing.id,
                            status = WatchStatus.WATCHED
                        )
                        true
                    }
                }
            }.onSuccess { updated ->
                if (updated) onDone() else onNotFound()
            }
        }
    }

    /**
     * Move an existing watched movie/series back to Want to watch.
     * The existing row is kept and its rating is cleared.
     */
    fun moveBackToWantToWatch(
        catalog: CatalogRepository,
        onDone: () -> Unit,
        onNotFound: () -> Unit = {}
    ) {
        val item = _state.value.item ?: return

        viewModelScope.launch {
            runCatching {
                if (item.mediaType == "movie") {
                    val existing =
                        catalog.getMovieByTmdbId(item.id)
                            ?: catalog.getMovieByTitle(item.title.trim())

                    if (existing == null) {
                        false
                    } else {
                        catalog.setMovieStatus(
                            movieId = existing.id,
                            status = WatchStatus.TO_WATCH,
                            clearRating = true
                        )
                        true
                    }
                } else {
                    val existing =
                        catalog.getSeriesByUrl(item.tmdbUrl)
                            ?: catalog.getSeriesByTitle(item.title.trim())

                    if (existing == null) {
                        false
                    } else {
                        catalog.setSeriesStatus(
                            seriesId = existing.id,
                            status = WatchStatus.TO_WATCH,
                            clearRating = true
                        )
                        true
                    }
                }
            }.onSuccess { updated ->
                if (updated) onDone() else onNotFound()
            }
        }
    }

    class Factory(
        private val api: TmdbApi
    ) : androidx.lifecycle.ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T = TmdbDetailViewModel(api) as T
    }
}