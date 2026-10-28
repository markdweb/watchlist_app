package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.CollectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY title ASC")
    fun getAll(): Flow<List<CollectionEntity>>

    @Query("SELECT * FROM collections WHERE id = :id")
    suspend fun getById(id: Long): CollectionEntity?

    @Query("SELECT * FROM collections WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun findByTitleIgnoreCase(title: String): CollectionEntity?

    @Insert
    suspend fun insert(collection: CollectionEntity): Long

    @Update
    suspend fun update(collection: CollectionEntity)

    @Delete
    suspend fun delete(collection: CollectionEntity)
}
