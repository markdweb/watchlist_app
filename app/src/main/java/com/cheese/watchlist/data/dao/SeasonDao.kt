package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {
    @Query("SELECT * FROM seasons WHERE seriesId = :seriesId ORDER BY seasonNumber ASC")
    fun getForSeries(seriesId: Long): Flow<List<SeasonEntity>>

    @Query("SELECT * FROM seasons WHERE seriesId = :seriesId ORDER BY seasonNumber ASC")
    suspend fun getForSeriesOnce(seriesId: Long): List<SeasonEntity>

    @Query("SELECT * FROM seasons WHERE id = :id")
    suspend fun getById(id: Long): SeasonEntity?

    @Query("SELECT COUNT(*) FROM seasons WHERE seriesId = :seriesId AND seasonNumber = :seasonNumber")
    suspend fun countBySeasonNumber(seriesId: Long, seasonNumber: Int): Int

    @Insert
    suspend fun insert(season: SeasonEntity): Long

    @Update
    suspend fun update(season: SeasonEntity)

    @Delete
    suspend fun delete(season: SeasonEntity)
}
