package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.WatchItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchItemDao {
    @Query("SELECT * FROM watch_items ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<WatchItem>>

    @Query("SELECT * FROM watch_items WHERE id = :id")
    suspend fun getById(id: Long): WatchItem?

    @Query("SELECT * FROM watch_items WHERE tmdbId = :tmdbId LIMIT 1")
    suspend fun getByTmdbId(tmdbId: Int): WatchItem?

    @Query("SELECT * FROM watch_items WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun getByTitle(title: String): WatchItem?

    @Query("SELECT * FROM watch_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<WatchItem>

    @Insert
    suspend fun insert(item: WatchItem): Long

    @Update
    suspend fun update(item: WatchItem)

    @Delete
    suspend fun delete(item: WatchItem)
}
