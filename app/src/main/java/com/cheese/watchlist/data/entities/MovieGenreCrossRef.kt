package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Relational replacement for the comma-separated genre string, for movies.
 * [WatchItem.genre] is left in place for now (see WatchItem.kt doc comment) —
 * this table is populated from it during migration and is the source of truth
 * for any new genre-aware querying going forward.
 */
@Entity(
    tableName = "movie_genre_cross_ref",
    primaryKeys = ["genreId", "movieId"],
    foreignKeys = [
        ForeignKey(
            entity = GenreEntity::class,
            parentColumns = ["id"],
            childColumns = ["genreId"],
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
        Index(value = ["genreId"])
    ]
)
data class MovieGenreCrossRef(
    val genreId: Long,
    val movieId: Long
)
