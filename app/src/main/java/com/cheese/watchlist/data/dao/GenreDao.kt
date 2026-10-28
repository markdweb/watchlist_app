package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.GenreEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GenreDao {
    @Query("SELECT * FROM genres ORDER BY name ASC")
    fun getAll(): Flow<List<GenreEntity>>

    @Query("SELECT COUNT(*) FROM genres")
    suspend fun count(): Int

    @Query("SELECT * FROM genres WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): GenreEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(genre: GenreEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(genres: List<GenreEntity>)

    @Delete
    suspend fun delete(genre: GenreEntity)
}
