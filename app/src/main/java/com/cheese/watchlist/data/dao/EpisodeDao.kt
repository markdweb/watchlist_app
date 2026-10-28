package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes WHERE seasonId = :seasonId ORDER BY episodeNumber ASC")
    fun getForSeason(seasonId: Long): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE seasonId = :seasonId ORDER BY episodeNumber ASC")
    suspend fun getForSeasonOnce(seasonId: Long): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun getById(id: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes WHERE seasonId IN (:seasonIds) ORDER BY seasonId, episodeNumber")
    suspend fun getForSeasonsOnce(seasonIds: List<Long>): List<EpisodeEntity>

    @Query("SELECT COUNT(*) FROM episodes WHERE seasonId = :seasonId")
    suspend fun countForSeason(seasonId: Long): Int

    @Query("SELECT COUNT(*) FROM episodes WHERE seasonId = :seasonId AND watched = 1")
    suspend fun watchedCountForSeason(seasonId: Long): Int

    @Query("SELECT COUNT(*) FROM episodes e INNER JOIN seasons s ON e.seasonId=s.id WHERE s.seriesId=:seriesId")
    suspend fun countForSeries(seriesId: Long): Int

    @Query("SELECT COUNT(*) FROM episodes e INNER JOIN seasons s ON e.seasonId=s.id WHERE s.seriesId=:seriesId AND e.watched=1")
    suspend fun watchedCountForSeries(seriesId: Long): Int

    @Query("SELECT * FROM episodes e INNER JOIN seasons s ON e.seasonId=s.id WHERE s.seriesId=:seriesId AND e.watched=0 ORDER BY s.seasonNumber ASC, e.episodeNumber ASC LIMIT 1")
    suspend fun firstUnwatchedForSeries(seriesId: Long): EpisodeEntity?

    @Query("SELECT * FROM episodes e INNER JOIN seasons s ON e.seasonId=s.id WHERE s.seriesId=:seriesId ORDER BY s.seasonNumber ASC, e.episodeNumber ASC LIMIT 1")
    suspend fun firstEpisodeForSeries(seriesId: Long): EpisodeEntity?

    @Insert
    suspend fun insert(episode: EpisodeEntity): Long

    @Insert
    suspend fun insertAll(episodes: List<EpisodeEntity>): List<Long>

    @Update
    suspend fun update(episode: EpisodeEntity)

    @Delete
    suspend fun delete(episode: EpisodeEntity)
}
