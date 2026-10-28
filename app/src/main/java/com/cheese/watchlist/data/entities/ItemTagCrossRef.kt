package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

enum class TaggableType { MOVIE, SERIES }

/**
 * Join table between [TagEntity] and either a movie ([WatchItem]) or a
 * [SeriesEntity]. [itemId] intentionally has no Room [ForeignKey] constraint —
 * it points at two different parent tables depending on [itemType], which Room
 * can't express as a single FK. Only [tagId] is a real FK. Any code that deletes
 * a movie or series is responsible for also deleting its matching rows here
 * (by (itemId, itemType)) in the same transaction, since the DB can't cascade
 * that automatically.
 */
@Entity(
    tableName = "item_tag_cross_ref",
    primaryKeys = ["tagId", "itemId", "itemType"],
    foreignKeys = [ForeignKey(
        entity = TagEntity::class,
        parentColumns = ["id"],
        childColumns = ["tagId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["itemId", "itemType"])]
)
data class ItemTagCrossRef(
    val tagId: Long,
    val itemId: Long,
    val itemType: TaggableType
)
