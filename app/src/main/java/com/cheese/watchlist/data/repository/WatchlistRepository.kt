package com.cheese.watchlist.data.repository

import com.cheese.watchlist.data.dao.GenreDao
import com.cheese.watchlist.data.dao.SourceDao
import com.cheese.watchlist.data.dao.WatchItemDao
import com.cheese.watchlist.data.entities.GenreEntity
import com.cheese.watchlist.data.entities.SourceEntity
import com.cheese.watchlist.data.entities.WatchItem
import kotlinx.coroutines.flow.Flow

class WatchlistRepository(
    private val watchItemDao: WatchItemDao,
    private val genreDao: GenreDao,
    private val sourceDao: SourceDao
) {
    val allItems: Flow<List<WatchItem>> = watchItemDao.getAll()
    val allGenres: Flow<List<GenreEntity>> = genreDao.getAll()
    val allSources: Flow<List<SourceEntity>> = sourceDao.getAll()

    suspend fun getItem(id: Long): WatchItem? = watchItemDao.getById(id)

    suspend fun saveItem(item: WatchItem): Long {
        return if (item.id == 0L) {
            watchItemDao.insert(item)
        } else {
            watchItemDao.update(item)
            item.id
        }
    }

    suspend fun deleteItem(item: WatchItem) = watchItemDao.delete(item)

    suspend fun addGenre(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val existing = genreDao.findByNameIgnoreCase(trimmed)
        if (existing != null) return false
        genreDao.insert(GenreEntity(name = trimmed))
        return true
    }

    suspend fun deleteGenre(genre: GenreEntity) = genreDao.delete(genre)

    suspend fun addSource(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val existing = sourceDao.findByNameIgnoreCase(trimmed)
        if (existing != null) return false
        sourceDao.insert(SourceEntity(name = trimmed))
        return true
    }

    suspend fun deleteSource(source: SourceEntity) = sourceDao.delete(source)
}
