package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.CollectionMovieCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionMovieCrossRefDao {
    @Query("SELECT * FROM collection_movie_cross_ref WHERE collectionId = :collectionId ORDER BY watchOrder ASC")
    fun getForCollection(collectionId: Long): Flow<List<CollectionMovieCrossRef>>

    @Query("SELECT * FROM collection_movie_cross_ref WHERE collectionId = :collectionId ORDER BY watchOrder ASC")
    suspend fun getForCollectionOnce(collectionId: Long): List<CollectionMovieCrossRef>

    @Query("SELECT * FROM collection_movie_cross_ref WHERE movieId = :movieId")
    fun getForMovie(movieId: Long): Flow<List<CollectionMovieCrossRef>>

    @Query("SELECT * FROM collection_movie_cross_ref WHERE movieId = :movieId")
    suspend fun getForMovieOnce(movieId: Long): List<CollectionMovieCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: CollectionMovieCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crossRefs: List<CollectionMovieCrossRef>)

    @Update
    suspend fun update(crossRef: CollectionMovieCrossRef)

    @Query("UPDATE collection_movie_cross_ref SET watchOrder = :watchOrder WHERE collectionId = :collectionId AND movieId = :movieId")
    suspend fun setWatchOrder(collectionId: Long, movieId: Long, watchOrder: Int)

    @Query("DELETE FROM collection_movie_cross_ref WHERE collectionId = :collectionId AND movieId = :movieId")
    suspend fun remove(collectionId: Long, movieId: Long)

    @Query("DELETE FROM collection_movie_cross_ref WHERE collectionId = :collectionId")
    suspend fun removeAllForCollection(collectionId: Long)
}
