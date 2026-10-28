package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A tracked series (distinct from [WatchItem], which now represents movies only).
 *
 * [resumeEpisodeId] is a nullable, non-FK-constrained pointer into [EpisodeEntity.id].
 * NULL means "use the computed default" (lowest-numbered unwatched episode in the
 * lowest-numbered season that has one). It is intentionally not declared as a Room
 * [ForeignKey] here: episodes for a series are created after the series row exists,
 * and the pointer is expected to be cleared back to NULL once the user watches the
 * episode it points to (see SeriesRepository, to be added in a later phase) — treating
 * it as a soft reference keeps that read/clear cycle simple.
 */
@Entity(
    tableName = "series",
    foreignKeys = [ForeignKey(
        entity = SourceEntity::class,
        parentColumns = ["id"],
        childColumns = ["sourceId"],
        onDelete = ForeignKey.SET_NULL
    )],
    indices = [
        Index(value = ["title"]),
        Index(value = ["status"]),
        Index(value = ["favorite"]),
        Index(value = ["sourceId"])
    ]
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val status: WatchStatus = WatchStatus.TO_WATCH,
    val whereToWatch: String = "",
    val sourceId: Long? = null,

    // TMDB URL / identity. Do not use this for the custom watch URL.
    val url: String = "",

    // Custom website/app URL used by the "Where to watch" feature.
    val watchUrl: String = "",

    val rating: Int = 0,
    val favorite: Boolean = false,
    val comments: String = "",
    val yearDate: String = "",
    val resumeEpisodeId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)