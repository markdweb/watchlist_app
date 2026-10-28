package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections", indices = [Index(value = ["title"], unique = true)])
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val comments: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
