package com.cheese.watchlist.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class WatchType { MOVIE, SERIES }

enum class WatchStatus(val label: String) {
    TO_WATCH("To Watch"),
    WATCHING("Watching"),
    WATCHED("Watched"),
    DROPPED("Dropped")
}

/**
 * Movies (and, until the ViewModel/UI layer is migrated in a later phase, any
 * newly-added SERIES-type rows created through the existing Add/Edit flow).
 *
 * Phase 0A note: [genre], [whereToWatch], [season], [totalEpisodes] and
 * [currentEpisode] are kept here unchanged, even though the approved
 * architecture ultimately replaces genre with [MovieGenreCrossRef] and
 * whereToWatch with [sourceId]. Removing them now would break every existing
 * screen/ViewModel/PC-Manager code path that reads them — all of which are
 * explicitly out of scope for this phase. [sourceId] is added and populated by
 * the migration as new, additional data; [MovieGenreCrossRef]/
 * [SeriesGenreCrossRef] are populated the same way. Cutting the UI over to the
 * relational data (and dropping these legacy columns) is deferred to the
 * ViewModel/UI phase — see the Phase 0A report for the full rationale.
 *
 * Existing rows of type SERIES are migrated into [SeriesEntity]/[SeasonEntity]/
 * [EpisodeEntity] and removed from this table by MIGRATION_2_3; going forward
 * this table is expected to hold movies only, though nothing currently stops a
 * new SERIES-type row from being created here via the untouched Add/Edit
 * screen until that screen is rewired in the UI phase (flagged as a known
 * limitation, not fixed in this phase).
 */
@Entity(
    tableName = "watch_items",
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
data class WatchItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val status: WatchStatus = WatchStatus.TO_WATCH,
    val type: WatchType = WatchType.MOVIE,
    val genre: String = "",
    val whereToWatch: String = "",
    val sourceId: Long? = null,
    val url: String = "",
    val watchUrl: String = "",
    val rating: Int = 0,
    val favorite: Boolean = false,
    val comments: String = "",
    val yearDate: String = "",
    val totalEpisodes: Int = 0,
    val currentEpisode: Int = 0,
    val season: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    val tmdbId: Int? = null,
    val posterPath: String? = null,
    val backdropPath: String? = null
)
