package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "series_genre_cross_ref",
    primaryKeys = ["genreId", "seriesId"],
    foreignKeys = [
        ForeignKey(
            entity = GenreEntity::class,
            parentColumns = ["id"],
            childColumns = ["genreId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SeriesEntity::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["seriesId"]),
        Index(value = ["genreId"])
    ]
)
data class SeriesGenreCrossRef(
    val genreId: Long,
    val seriesId: Long
)
