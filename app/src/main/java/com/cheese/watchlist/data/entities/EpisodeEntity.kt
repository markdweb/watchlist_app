package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episodes",
    foreignKeys = [ForeignKey(
        entity = SeasonEntity::class,
        parentColumns = ["id"],
        childColumns = ["seasonId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index(value = ["seasonId"]),
        Index(value = ["seasonId", "episodeNumber"], unique = true),
        Index(value = ["watched"])
    ]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seasonId: Long,
    val episodeNumber: Int,
    val title: String? = null,
    val watched: Boolean = false,
    val watchedAt: Long? = null
)
