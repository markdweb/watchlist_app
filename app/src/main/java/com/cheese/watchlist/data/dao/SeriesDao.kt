package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.SeriesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series ORDER BY updatedAt DESC")
    fun getAll(): Flow<List<SeriesEntity>>

    @Query("SELECT * FROM series WHERE id = :id")
    suspend fun getById(id: Long): SeriesEntity?

    @Query("SELECT * FROM series WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): SeriesEntity?

    @Query("SELECT * FROM series WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun getByTitle(title: String): SeriesEntity?

    @Query("SELECT * FROM series WHERE id = :id")
    fun observeById(id: Long): Flow<SeriesEntity?>

    @Query("SELECT * FROM series WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<SeriesEntity>

    @Insert
    suspend fun insert(series: SeriesEntity): Long

    @Update
    suspend fun update(series: SeriesEntity)

    @Delete
    suspend fun delete(series: SeriesEntity)
}
