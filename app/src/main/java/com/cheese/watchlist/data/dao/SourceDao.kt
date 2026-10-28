package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.SourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY name ASC")
    fun getAll(): Flow<List<SourceEntity>>

    @Query("SELECT COUNT(*) FROM sources")
    suspend fun count(): Int

    @Query("SELECT * FROM sources WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByNameIgnoreCase(name: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(sources: List<SourceEntity>)

    @Delete
    suspend fun delete(source: SourceEntity)
}
