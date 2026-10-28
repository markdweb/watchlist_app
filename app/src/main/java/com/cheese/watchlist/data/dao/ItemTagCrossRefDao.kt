package com.cheese.watchlist.data.dao

import androidx.room.*
import com.cheese.watchlist.data.entities.ItemTagCrossRef
import com.cheese.watchlist.data.entities.TaggableType
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemTagCrossRefDao {
    @Query("SELECT * FROM item_tag_cross_ref WHERE itemId = :itemId AND itemType = :itemType")
    fun getForItem(itemId: Long, itemType: TaggableType): Flow<List<ItemTagCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(crossRef: ItemTagCrossRef)

    @Query("DELETE FROM item_tag_cross_ref WHERE tagId = :tagId AND itemId = :itemId AND itemType = :itemType")
    suspend fun remove(tagId: Long, itemId: Long, itemType: TaggableType)

    @Query("DELETE FROM item_tag_cross_ref WHERE itemId = :itemId AND itemType = :itemType")
    suspend fun removeAllForItem(itemId: Long, itemType: TaggableType)
}
