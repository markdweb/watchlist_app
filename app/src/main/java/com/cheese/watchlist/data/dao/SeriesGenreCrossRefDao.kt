package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.SeriesGenreCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesGenreCrossRefDao {
    @Query("SELECT * FROM series_genre_cross_ref WHERE seriesId = :seriesId")
    fun getForSeries(seriesId: Long): Flow<List<SeriesGenreCrossRef>>

    @Query(
        """
        SELECT genres.* FROM genres
        INNER JOIN series_genre_cross_ref ON genres.id = series_genre_cross_ref.genreId
        WHERE series_genre_cross_ref.seriesId = :seriesId
        ORDER BY genres.name ASC
        """
    )
    suspend fun genreNamesForSeries(seriesId: Long): List<com.cheese.watchlist.data.entities.GenreEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: SeriesGenreCrossRef)

    @Query("DELETE FROM series_genre_cross_ref WHERE genreId = :genreId AND seriesId = :seriesId")
    suspend fun remove(genreId: Long, seriesId: Long)

    @Query("DELETE FROM series_genre_cross_ref WHERE seriesId = :seriesId")
    suspend fun removeAllForSeries(seriesId: Long)
}
