package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Join table between [CollectionEntity] and [WatchItem] (movies).
 *
 * Deleting a row here only removes the movie's membership in the collection —
 * it never deletes the movie itself, since the FK on [movieId] only cascades
 * from the movie side (deleting a movie removes its cross-ref rows), never the
 * other way around.
 */
@Entity(
    tableName = "collection_movie_cross_ref",
    primaryKeys = ["collectionId", "movieId"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WatchItem::class,
            parentColumns = ["id"],
            childColumns = ["movieId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["movieId"]),
        Index(value = ["collectionId", "watchOrder"])
    ]
)
data class CollectionMovieCrossRef(
    val collectionId: Long,
    val movieId: Long,
    val watchOrder: Int
)
