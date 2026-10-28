package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAll(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Delete
    suspend fun delete(tag: TagEntity)
}
