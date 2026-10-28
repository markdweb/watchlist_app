package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.MovieGenreCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieGenreCrossRefDao {
    @Query("SELECT * FROM movie_genre_cross_ref WHERE movieId = :movieId")
    fun getForMovie(movieId: Long): Flow<List<MovieGenreCrossRef>>

    @Query(
        """
        SELECT genres.* FROM genres
        INNER JOIN movie_genre_cross_ref ON genres.id = movie_genre_cross_ref.genreId
        WHERE movie_genre_cross_ref.movieId = :movieId
        ORDER BY genres.name ASC
        """
    )
    suspend fun genreNamesForMovie(movieId: Long): List<com.cheese.watchlist.data.entities.GenreEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: MovieGenreCrossRef)

    @Query("DELETE FROM movie_genre_cross_ref WHERE genreId = :genreId AND movieId = :movieId")
    suspend fun remove(genreId: Long, movieId: Long)

    @Query("DELETE FROM movie_genre_cross_ref WHERE movieId = :movieId")
    suspend fun removeAllForMovie(movieId: Long)
}
