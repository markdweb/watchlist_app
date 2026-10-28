package com.cheese.watchlist.data.repository

import androidx.room.withTransaction
import com.cheese.watchlist.data.dao.*
import com.cheese.watchlist.data.database.AppDatabase
import com.cheese.watchlist.data.entities.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf


data class SeasonProgress(
    val season: SeasonEntity,
    val watched: Int,
    val total: Int
) {
    val remaining: Int get() = (total - watched).coerceAtLeast(0)
    val progress: Float get() = if (total == 0) 0f else watched.toFloat() / total
    val completed: Boolean get() = total > 0 && watched == total
}

data class SeriesProgress(
    val series: SeriesEntity,
    val seasons: List<SeasonProgress>,
    val watched: Int,
    val total: Int,
    val nextEpisode: EpisodeEntity?
) {
    val remaining: Int get() = (total - watched).coerceAtLeast(0)
    val progress: Float get() = if (total == 0) 0f else watched.toFloat() / total
    val percentage: Int get() = (progress * 100).toInt()
}

data class SeriesMetadata(
    val genres: List<GenreEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList()
)

data class CollectionProgress(
    val collection: CollectionEntity,
    val movies: List<WatchItem>,
    val watched: Int
) {
    val total: Int get() = movies.size
    val remaining: Int get() = (total - watched).coerceAtLeast(0)
    val progress: Float get() = if (total == 0) 0f else watched.toFloat() / total
    val nextMovie: WatchItem? get() = movies.firstOrNull { it.status != WatchStatus.WATCHED }
}

data class LibraryStats(
    val movies: Int = 0,
    val series: Int = 0,
    val collections: Int = 0,
    val watchedMovies: Int = 0,
    val watchingMovies: Int = 0,
    val toWatchMovies: Int = 0,
    val watchedSeries: Int = 0,
    val watchingSeries: Int = 0,
    val toWatchSeries: Int = 0,
    val dropped: Int = 0,
    val totalEpisodes: Int = 0,
    val watchedEpisodes: Int = 0
) {
    val remainingEpisodes: Int get() = (totalEpisodes - watchedEpisodes).coerceAtLeast(0)
}

class CatalogRepository(
    private val db: AppDatabase,
    private val movies: WatchItemDao,
    private val series: SeriesDao,
    private val seasons: SeasonDao,
    private val episodes: EpisodeDao,
    private val collections: CollectionDao,
    private val collectionMovies: CollectionMovieCrossRefDao,
    private val genres: GenreDao,
    private val movieGenres: MovieGenreCrossRefDao,
    private val seriesGenres: SeriesGenreCrossRefDao,
    private val tags: TagDao,
    private val itemTags: ItemTagCrossRefDao,
    private val sources: SourceDao
) {
    val allMovies: Flow<List<WatchItem>> = movies.getAll()
    val allSeries: Flow<List<SeriesEntity>> = series.getAll()
    val allCollections: Flow<List<CollectionEntity>> = collections.getAll()
    val allGenres: Flow<List<GenreEntity>> = genres.getAll()
    val allTags: Flow<List<TagEntity>> = tags.getAll()
    val allSources: Flow<List<SourceEntity>> = sources.getAll()

    suspend fun getMovie(id: Long) = movies.getById(id)
    suspend fun getSeries(id: Long) = series.getById(id)

    suspend fun getMovieByTmdbId(tmdbId: Int) = movies.getByTmdbId(tmdbId)
    suspend fun getMovieByTitle(title: String) = movies.getByTitle(title)
    suspend fun getSeriesByUrl(url: String) = series.getByUrl(url)
    suspend fun getSeriesByTitle(title: String) = series.getByTitle(title)

    /**
     * Save or update the custom website/app URL for a movie.
     *
     * The existing `url` field is reserved for the TMDB URL.
     * `watchUrl` stores the exact user-provided watch link.
     */
    suspend fun setMovieWatchUrl(
        movieId: Long,
        watchUrl: String
    ) = db.withTransaction {
        val movie = movies.getById(movieId) ?: return@withTransaction

        movies.update(
            movie.copy(
                watchUrl = watchUrl.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Save or update the custom website/app URL for a series.
     *
     * The existing `url` field is reserved for the TMDB URL.
     * `watchUrl` stores the exact user-provided watch link.
     */
    suspend fun setSeriesWatchUrl(
        seriesId: Long,
        watchUrl: String
    ) = db.withTransaction {
        val seriesEntity = series.getById(seriesId) ?: return@withTransaction

        series.update(
            seriesEntity.copy(
                watchUrl = watchUrl.trim(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Update the status of an existing movie without deleting/re-adding it.
    suspend fun setMovieStatus(
        movieId: Long,
        status: WatchStatus,
        clearRating: Boolean = false
    ) = db.withTransaction {
        val movie = movies.getById(movieId) ?: return@withTransaction
        movies.update(
            movie.copy(
                status = status,
                rating = if (clearRating) 0 else movie.rating,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // Update the status of an existing series without recalculating it from episodes.
    suspend fun setSeriesStatus(
        seriesId: Long,
        status: WatchStatus,
        clearRating: Boolean = false
    ) = db.withTransaction {
        val seriesEntity = series.getById(seriesId) ?: return@withTransaction
        series.update(
            seriesEntity.copy(
                status = status,
                rating = if (clearRating) 0 else seriesEntity.rating,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveMovie(
        movie: WatchItem,
        genreNames: Set<String> = emptySet(),
        tagNames: Set<String> = emptySet(),
        sourceName: String? = null
    ): Long = db.withTransaction {
        val clean = movie.copy(
            type = WatchType.MOVIE,
            season = 1,
            totalEpisodes = 0,
            currentEpisode = 0,
            updatedAt = System.currentTimeMillis()
        )
        val id = if (clean.id == 0L) movies.insert(clean) else { movies.update(clean); clean.id }
        setMovieGenres(id, genreNames)
        setItemTags(id, TaggableType.MOVIE, tagNames)
        if (sourceName != null) assignMovieSource(id, sourceName)
        id
    }

    suspend fun deleteMovie(movieId: Long) = db.withTransaction {
        itemTags.removeAllForItem(movieId, TaggableType.MOVIE)
        movieGenres.removeAllForMovie(movieId)
        // Membership rows are intentionally removed first; the movie itself is then deleted.
        movies.getById(movieId)?.let { movies.delete(it) }
    }

    suspend fun saveSeries(
        seriesEntity: SeriesEntity,
        genreNames: Set<String> = emptySet(),
        tagNames: Set<String> = emptySet(),
        sourceName: String? = null
    ): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val clean = seriesEntity.copy(updatedAt = now)
        val id = if (clean.id == 0L) series.insert(clean) else { series.update(clean); clean.id }
        setSeriesGenres(id, genreNames)
        setItemTags(id, TaggableType.SERIES, tagNames)
        if (sourceName != null) assignSeriesSource(id, sourceName)
        recalculateSeriesStatus(id, preserveDropped = true)
        id
    }

    suspend fun deleteSeries(seriesId: Long) = db.withTransaction {
        itemTags.removeAllForItem(seriesId, TaggableType.SERIES)
        seriesGenres.removeAllForSeries(seriesId)
        series.getById(seriesId)?.let { series.delete(it) }
    }

    suspend fun addSeason(seriesId: Long, seasonNumber: Int, title: String? = null): Long {
        require(seasonNumber > 0)
        return db.withTransaction {
            require(seasons.countBySeasonNumber(seriesId, seasonNumber) == 0) { "Season already exists" }
            seasons.insert(SeasonEntity(seriesId = seriesId, seasonNumber = seasonNumber, title = title?.trim()?.ifBlank { null }))
        }
    }

    suspend fun deleteSeason(season: SeasonEntity) = seasons.delete(season)

    suspend fun addEpisodes(seasonId: Long, count: Int, startAt: Int = 1): List<Long> {
        require(count >= 0)
        require(startAt > 0)
        if (count == 0) return emptyList()
        return db.withTransaction {
            val existing = episodes.getForSeasonOnce(seasonId).map { it.episodeNumber }.toSet()
            val list = (startAt until startAt + count).filterNot { it in existing }
                .map { EpisodeEntity(seasonId = seasonId, episodeNumber = it) }
            val ids = episodes.insertAll(list)
            seasons.getById(seasonId)?.let { recalculateSeriesStatus(it.seriesId, preserveDropped = true) }
            ids
        }
    }

    suspend fun addEpisode(seasonId: Long, number: Int, title: String? = null): Long {
        require(number > 0)
        return db.withTransaction {
            val season = seasons.getById(seasonId) ?: error("Season not found")
            require(episodes.getForSeasonOnce(seasonId).none { it.episodeNumber == number }) { "Episode already exists" }
            val id = episodes.insert(EpisodeEntity(seasonId = seasonId, episodeNumber = number, title = title?.trim()?.ifBlank { null }))
            recalculateSeriesStatus(season.seriesId, preserveDropped = true)
            id
        }
    }

    suspend fun setEpisodeWatched(episodeId: Long, watched: Boolean) = db.withTransaction {
        val episode = episodes.getById(episodeId) ?: return@withTransaction
        val season = seasons.getById(episode.seasonId) ?: return@withTransaction
        val seriesEntity = series.getById(season.seriesId) ?: return@withTransaction
        episodes.update(episode.copy(watched = watched, watchedAt = if (watched) System.currentTimeMillis() else null))
        // Always touch the parent so every Room Flow depending on the series refreshes immediately.
        series.update(seriesEntity.copy(updatedAt = System.currentTimeMillis()))
        recalculateSeriesStatus(season.seriesId, preserveDropped = seriesEntity.status == WatchStatus.DROPPED && !watched)
    }

    suspend fun resumeFrom(seriesId: Long, episodeId: Long?) = db.withTransaction {
        val s = series.getById(seriesId) ?: return@withTransaction
        series.update(s.copy(resumeEpisodeId = episodeId, updatedAt = System.currentTimeMillis()))
    }

    suspend fun recalculateSeriesStatus(seriesId: Long, preserveDropped: Boolean = true) {
        val s = series.getById(seriesId) ?: return
        val total = episodes.countForSeries(seriesId)
        val watched = episodes.watchedCountForSeries(seriesId)
        val newStatus = when {
            total > 0 && watched == total -> WatchStatus.WATCHED
            preserveDropped && s.status == WatchStatus.DROPPED && watched == 0 -> WatchStatus.DROPPED
            watched > 0 -> WatchStatus.WATCHING
            else -> WatchStatus.TO_WATCH
        }
        if (newStatus != s.status) series.update(s.copy(status = newStatus, updatedAt = System.currentTimeMillis()))
    }

    private fun buildSeriesProgress(s: SeriesEntity, seasonsList: List<SeasonEntity>, episodeLists: List<List<EpisodeEntity>>): SeriesProgress {
        val seasonProgress = seasonsList.mapIndexed { index, season ->
            val all = episodeLists.getOrElse(index) { emptyList() }
            SeasonProgress(season, all.count { it.watched }, all.size)
        }
        val allEpisodes = episodeLists.flatten().sortedWith(compareBy<EpisodeEntity> { episode ->
            seasonsList.indexOfFirst { it.id == episode.seasonId }
        }.thenBy { it.episodeNumber })
        val next = allEpisodes.firstOrNull { !it.watched }
        return SeriesProgress(s, seasonProgress, allEpisodes.count { it.watched }, allEpisodes.size, next)
    }

    suspend fun seriesProgress(seriesId: Long): SeriesProgress? {
        val s = series.getById(seriesId) ?: return null
        val seasonsList = seasons.getForSeriesOnce(seriesId)
        val lists = seasonsList.map { episodes.getForSeasonOnce(it.id) }
        return buildSeriesProgress(s, seasonsList, lists)
    }

    fun observeSeriesProgress(seriesId: Long): Flow<SeriesProgress?> =
        combine(series.observeById(seriesId), seasons.getForSeries(seriesId)) { s, seasonList -> s to seasonList }
            .flatMapLatest { (s, seasonList) ->
                if (s == null) flowOf(null)
                else if (seasonList.isEmpty()) flowOf(buildSeriesProgress(s, emptyList(), emptyList()))
                else combine(seasonList.map { episodes.getForSeason(it.id) }) { lists ->
                    buildSeriesProgress(s, seasonList, lists.toList())
                }
            }

    suspend fun getEpisodes(seasonId: Long) = episodes.getForSeasonOnce(seasonId)
    fun observeEpisodes(seasonId: Long): Flow<List<EpisodeEntity>> = episodes.getForSeason(seasonId)
    fun observeSeasons(seriesId: Long) = seasons.getForSeries(seriesId)

    suspend fun seriesMetadata(seriesId: Long): SeriesMetadata = SeriesMetadata(
        genres = seriesGenres.genreNamesForSeries(seriesId),
        tags = itemTags(itemId = seriesId, type = TaggableType.SERIES)
    )

    fun observeSeriesGenres(seriesId: Long) = seriesGenres.getForSeries(seriesId)
    fun observeSeriesTags(seriesId: Long) = itemTags.getForItem(seriesId, TaggableType.SERIES)

    suspend fun createCollection(title: String, comments: String = ""): Long =
        collections.insert(CollectionEntity(title = title.trim(), comments = comments.trim()))

    suspend fun updateCollection(collection: CollectionEntity) =
        collections.update(collection.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteCollection(collectionId: Long) = db.withTransaction {
        collectionMovies.removeAllForCollection(collectionId)
        collections.getById(collectionId)?.let { collections.delete(it) }
    }

    suspend fun getCollection(id: Long) = collections.getById(id)

    suspend fun setCollectionMovies(collectionId: Long, movieIdsInOrder: List<Long>) = db.withTransaction {
        collectionMovies.removeAllForCollection(collectionId)
        collectionMovies.insertAll(movieIdsInOrder.distinct().mapIndexed { index, movieId ->
            CollectionMovieCrossRef(collectionId, movieId, index + 1)
        })
        collections.getById(collectionId)?.let { collections.update(it.copy(updatedAt = System.currentTimeMillis())) }
    }

    suspend fun removeMovieFromCollection(collectionId: Long, movieId: Long) = collectionMovies.remove(collectionId, movieId)

    suspend fun collectionMovieIds(collectionId: Long): List<Long> =
        collectionMovies.getForCollectionOnce(collectionId).sortedBy { it.watchOrder }.map { it.movieId }

    suspend fun collectionProgress(collectionId: Long): CollectionProgress? {
        val c = collections.getById(collectionId) ?: return null
        val refs = collectionMovies.getForCollectionOnce(collectionId)
        val movieMap: Map<Long, WatchItem> =
            if (refs.isEmpty()) emptyMap() else movies.getByIds(refs.map { it.movieId }).associateBy { it.id }
        val ordered = refs.sortedBy { it.watchOrder }.mapNotNull { movieMap[it.movieId] }
        return CollectionProgress(c, ordered, ordered.count { it.status == WatchStatus.WATCHED })
    }

    fun observeCollectionMovies(collectionId: Long): Flow<List<WatchItem>> =
        combine(collectionMovies.getForCollection(collectionId), allMovies) { refs, items ->
            val map = items.associateBy { it.id }
            refs.sortedBy { it.watchOrder }.mapNotNull { map[it.movieId] }
        }

    suspend fun setMovieGenres(movieId: Long, names: Set<String>) = db.withTransaction {
        movieGenres.removeAllForMovie(movieId)
        names.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.forEach { name ->
            movieGenres.insert(MovieGenreCrossRef(ensureGenre(name), movieId))
        }
    }

    suspend fun setSeriesGenres(seriesId: Long, names: Set<String>) = db.withTransaction {
        seriesGenres.removeAllForSeries(seriesId)
        names.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.forEach { name ->
            seriesGenres.insert(SeriesGenreCrossRef(ensureGenre(name), seriesId))
        }
    }

    suspend fun movieGenres(movieId: Long): List<GenreEntity> = movieGenres.genreNamesForMovie(movieId)
    suspend fun seriesGenres(seriesId: Long): List<GenreEntity> = seriesGenres.genreNamesForSeries(seriesId)

    suspend fun ensureGenre(name: String): Long {
        val clean = name.trim()
        require(clean.isNotBlank())
        genres.findByNameIgnoreCase(clean)?.let { return it.id }
        val inserted = genres.insert(GenreEntity(name = clean))
        return if (inserted != -1L) inserted else genres.findByNameIgnoreCase(clean)!!.id
    }

    suspend fun ensureTag(name: String): Long {
        val clean = name.trim()
        require(clean.isNotBlank())
        tags.findByNameIgnoreCase(clean)?.let { return it.id }
        val inserted = tags.insert(TagEntity(name = clean))
        return if (inserted != -1L) inserted else tags.findByNameIgnoreCase(clean)!!.id
    }

    suspend fun setItemTags(itemId: Long, type: TaggableType, names: Set<String>) = db.withTransaction {
        itemTags.removeAllForItem(itemId, type)
        names.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.forEach {
            itemTags.insert(ItemTagCrossRef(ensureTag(it), itemId, type))
        }
    }

    suspend fun itemTags(itemId: Long, type: TaggableType): List<TagEntity> {
        val refs = itemTags.getForItem(itemId, type).first()
        val all = tags.getAll().first().associateBy { it.id }
        return refs.mapNotNull { all[it.tagId] }
    }

    suspend fun deleteTag(tag: TagEntity) = tags.delete(tag)

    suspend fun assignMovieSource(movieId: Long, sourceName: String?) = db.withTransaction {
        val m = movies.getById(movieId) ?: return@withTransaction
        val sourceId = sourceName?.trim()?.takeIf { it.isNotBlank() }?.let { ensureSource(it) }
        movies.update(m.copy(sourceId = sourceId, whereToWatch = sourceName.orEmpty(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun assignSeriesSource(seriesId: Long, sourceName: String?) = db.withTransaction {
        val s = series.getById(seriesId) ?: return@withTransaction
        val sourceId = sourceName?.trim()?.takeIf { it.isNotBlank() }?.let { ensureSource(it) }
        series.update(s.copy(sourceId = sourceId, whereToWatch = sourceName.orEmpty(), updatedAt = System.currentTimeMillis()))
    }

    suspend fun ensureSource(name: String): Long {
        val clean = name.trim()
        require(clean.isNotBlank())
        sources.findByNameIgnoreCase(clean)?.let { return it.id }
        val inserted = sources.insert(SourceEntity(name = clean))
        return if (inserted != -1L) inserted else sources.findByNameIgnoreCase(clean)!!.id
    }

    suspend fun stats(): LibraryStats {
        val ms = allMovies.first()
        val ss = allSeries.first()
        val totalEpisodes = ss.sumOf { episodes.countForSeries(it.id) }
        val watchedEpisodes = ss.sumOf { episodes.watchedCountForSeries(it.id) }
        return LibraryStats(
            movies = ms.size,
            series = ss.size,
            collections = allCollections.first().size,
            watchedMovies = ms.count { it.status == WatchStatus.WATCHED },
            watchingMovies = ms.count { it.status == WatchStatus.WATCHING },
            toWatchMovies = ms.count { it.status == WatchStatus.TO_WATCH },
            watchedSeries = ss.count { it.status == WatchStatus.WATCHED },
            watchingSeries = ss.count { it.status == WatchStatus.WATCHING },
            toWatchSeries = ss.count { it.status == WatchStatus.TO_WATCH },
            dropped = ms.count { it.status == WatchStatus.DROPPED } + ss.count { it.status == WatchStatus.DROPPED },
            totalEpisodes = totalEpisodes,
            watchedEpisodes = watchedEpisodes
        )
    }
}
