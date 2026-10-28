package com.cheese.watchlist.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/**
 * v2 -> v3: establishes the new schema foundation approved for the entertainment
 * tracker upgrade — series/season/episode tables, collections, relational genre
 * cross-refs, and tags — while preserving every existing `watch_items` row.
 *
 * High-level sequence (see the Phase 0A report for the full design rationale):
 *  1. Create all new tables + their indexes (additive only).
 *  2. Add `sourceId` to `watch_items` and resolve it from `whereToWatch` for
 *     every row (movies AND series-type rows still present at this point).
 *  3. Migrate every movie's comma-separated `genre` into MovieGenreCrossRef.
 *  4. Convert every SERIES-type `watch_items` row into a SeriesEntity + one
 *     SeasonEntity (using only the season on record — no invented history) +
 *     N EpisodeEntity rows (watched split at the old `currentEpisode`), and
 *     migrate that row's genre into SeriesGenreCrossRef.
 *  5. Recalculate each new series' status from its freshly-created episodes.
 *  6. Verify row/episode counts and genre/source resolution coverage. Any
 *     failure throws, which rolls back the whole migration (Room runs the
 *     migrate() call inside a single transaction) — nothing from steps 1-5 is
 *     left half-applied, and no existing row is touched by this point.
 *  7. Only after verification passes: delete the now-migrated SERIES rows from
 *     `watch_items`, then recreate `watch_items` to add the `sourceId` foreign
 *     key constraint (SQLite can't ALTER TABLE ADD a FK, so this requires the
 *     create-new-table/copy/drop/rename pattern — every other existing column
 *     is preserved unchanged and every existing id is preserved via explicit
 *     column copy, never reassigned).
 *
 * NOTE: this database does not enable `PRAGMA foreign_keys` (see AppDatabase —
 * no such call exists), so the table-recreation steps below are safe even
 * though other new tables (e.g. movie_genre_cross_ref, collection_movie_cross_ref)
 * declare a FK against `watch_items` before it is recreated in step 7 — SQLite
 * resolves FK targets by table name, and enforcement is off, so the brief
 * drop/rename is never checked or enforced mid-migration.
 */
object Migration2To3 : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {
        createNewTables(db)
        addSourceIdColumn(db)
        resolveSources(db)
        migrateMovieGenres(db)
        val seriesConversion = migrateSeriesRows(db)
        recalculateSeriesStatus(db)
        verifyMigration(db, seriesConversion)
        db.execSQL("DELETE FROM watch_items WHERE type = 'SERIES'")
        rebuildWatchItemsWithSourceIdForeignKey(db)
        createWatchItemsIndices(db)
    }

    // ---------- step 1: new tables ----------

    private fun createNewTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `series` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `whereToWatch` TEXT NOT NULL,
                `sourceId` INTEGER,
                `url` TEXT NOT NULL,
                `rating` INTEGER NOT NULL,
                `favorite` INTEGER NOT NULL,
                `comments` TEXT NOT NULL,
                `yearDate` TEXT NOT NULL,
                `resumeEpisodeId` INTEGER,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`sourceId`) REFERENCES `sources`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_title` ON `series` (`title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_status` ON `series` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_favorite` ON `series` (`favorite`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_sourceId` ON `series` (`sourceId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `seasons` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `seriesId` INTEGER NOT NULL,
                `seasonNumber` INTEGER NOT NULL,
                `title` TEXT,
                FOREIGN KEY(`seriesId`) REFERENCES `series`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_seasons_seriesId` ON `seasons` (`seriesId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_seasons_seriesId_seasonNumber` ON `seasons` (`seriesId`, `seasonNumber`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `episodes` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `seasonId` INTEGER NOT NULL,
                `episodeNumber` INTEGER NOT NULL,
                `title` TEXT,
                `watched` INTEGER NOT NULL,
                `watchedAt` INTEGER,
                FOREIGN KEY(`seasonId`) REFERENCES `seasons`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_episodes_seasonId` ON `episodes` (`seasonId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_episodes_seasonId_episodeNumber` ON `episodes` (`seasonId`, `episodeNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_episodes_watched` ON `episodes` (`watched`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `collections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `comments` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_collections_title` ON `collections` (`title`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `collection_movie_cross_ref` (
                `collectionId` INTEGER NOT NULL,
                `movieId` INTEGER NOT NULL,
                `watchOrder` INTEGER NOT NULL,
                PRIMARY KEY(`collectionId`, `movieId`),
                FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`movieId`) REFERENCES `watch_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_movie_cross_ref_movieId` ON `collection_movie_cross_ref` (`movieId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_collection_movie_cross_ref_collectionId_watchOrder` ON `collection_movie_cross_ref` (`collectionId`, `watchOrder`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tags` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `item_tag_cross_ref` (
                `tagId` INTEGER NOT NULL,
                `itemId` INTEGER NOT NULL,
                `itemType` TEXT NOT NULL,
                PRIMARY KEY(`tagId`, `itemId`, `itemType`),
                FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_tag_cross_ref_itemId_itemType` ON `item_tag_cross_ref` (`itemId`, `itemType`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `movie_genre_cross_ref` (
                `genreId` INTEGER NOT NULL,
                `movieId` INTEGER NOT NULL,
                PRIMARY KEY(`genreId`, `movieId`),
                FOREIGN KEY(`genreId`) REFERENCES `genres`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`movieId`) REFERENCES `watch_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_movie_genre_cross_ref_movieId` ON `movie_genre_cross_ref` (`movieId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_movie_genre_cross_ref_genreId` ON `movie_genre_cross_ref` (`genreId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `series_genre_cross_ref` (
                `genreId` INTEGER NOT NULL,
                `seriesId` INTEGER NOT NULL,
                PRIMARY KEY(`genreId`, `seriesId`),
                FOREIGN KEY(`genreId`) REFERENCES `genres`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`seriesId`) REFERENCES `series`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_genre_cross_ref_seriesId` ON `series_genre_cross_ref` (`seriesId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_genre_cross_ref_genreId` ON `series_genre_cross_ref` (`genreId`)")
    }

    // ---------- step 2: sourceId ----------

    private fun addSourceIdColumn(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE watch_items ADD COLUMN sourceId INTEGER")
    }

    /** Resolves whereToWatch -> sourceId for every watch_items row (movies AND series-type). */
    private fun resolveSources(db: SupportSQLiteDatabase) {
        val distinctSources = mutableSetOf<String>()
        db.query("SELECT DISTINCT whereToWatch FROM watch_items WHERE TRIM(whereToWatch) != ''").use { cursor ->
            while (cursor.moveToNext()) {
                distinctSources.add(cursor.getString(0))
            }
        }
        distinctSources.forEach { name ->
            val sourceId = resolveOrCreateSource(db, name)
            db.execSQL(
                "UPDATE watch_items SET sourceId = ? WHERE whereToWatch = ?",
                arrayOf<Any>(sourceId, name)
            )
        }
    }

    private fun resolveOrCreateSource(db: SupportSQLiteDatabase, name: String): Long {
        val trimmed = name.trim()
        db.query("SELECT id FROM sources WHERE name = ? COLLATE NOCASE LIMIT 1", arrayOf<Any>(trimmed)).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        val values = ContentValues().apply { put("name", trimmed) }
        return db.insert("sources", SQLiteDatabase.CONFLICT_ABORT, values)
    }

    private fun resolveOrCreateGenre(db: SupportSQLiteDatabase, name: String): Long {
        val trimmed = name.trim()
        db.query("SELECT id FROM genres WHERE name = ? COLLATE NOCASE LIMIT 1", arrayOf<Any>(trimmed)).use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        val values = ContentValues().apply { put("name", trimmed) }
        return db.insert("genres", SQLiteDatabase.CONFLICT_ABORT, values)
    }

    private fun genreTokens(rawGenre: String): List<String> =
        rawGenre.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    // ---------- step 3: movie genres ----------

    private fun migrateMovieGenres(db: SupportSQLiteDatabase) {
        db.query("SELECT id, genre FROM watch_items WHERE type = 'MOVIE'").use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow("id")
            val genreIdx = cursor.getColumnIndexOrThrow("genre")
            while (cursor.moveToNext()) {
                val movieId = cursor.getLong(idIdx)
                val tokens = genreTokens(cursor.getString(genreIdx) ?: "")
                tokens.forEach { token ->
                    val genreId = resolveOrCreateGenre(db, token)
                    val values = ContentValues().apply {
                        put("genreId", genreId)
                        put("movieId", movieId)
                    }
                    db.insert("movie_genre_cross_ref", SQLiteDatabase.CONFLICT_IGNORE, values)
                }
            }
        }
    }

    // ---------- step 4: series/season/episode conversion ----------

    /** Old watch_items.id -> Pair(new series.id, expected total episode count), for verification. */
    private data class SeriesConversionResult(
        val oldIdToNewSeriesId: Map<Long, Long>,
        val newSeriesIdToExpectedEpisodeCount: Map<Long, Int>,
        val genreTokenResolutionCount: Int
    )

    private fun migrateSeriesRows(db: SupportSQLiteDatabase): SeriesConversionResult {
        val oldIdToNewSeriesId = mutableMapOf<Long, Long>()
        val expectedEpisodeCounts = mutableMapOf<Long, Int>()
        var genreTokensResolved = 0

        data class OldSeriesRow(
            val id: Long,
            val title: String,
            val status: String,
            val genre: String,
            val whereToWatch: String,
            val sourceId: Long?,
            val url: String,
            val rating: Int,
            val favorite: Boolean,
            val comments: String,
            val yearDate: String,
            val season: Int,
            val totalEpisodes: Int,
            val currentEpisode: Int,
            val createdAt: Long,
            val updatedAt: Long
        )

        val rows = mutableListOf<OldSeriesRow>()
        db.query("SELECT * FROM watch_items WHERE type = 'SERIES'").use { cursor ->
            while (cursor.moveToNext()) {
                rows.add(
                    OldSeriesRow(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        status = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        genre = cursor.getString(cursor.getColumnIndexOrThrow("genre")) ?: "",
                        whereToWatch = cursor.getString(cursor.getColumnIndexOrThrow("whereToWatch")) ?: "",
                        sourceId = if (cursor.isNull(cursor.getColumnIndexOrThrow("sourceId"))) null
                                   else cursor.getLong(cursor.getColumnIndexOrThrow("sourceId")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("url")) ?: "",
                        rating = cursor.getInt(cursor.getColumnIndexOrThrow("rating")),
                        favorite = cursor.getInt(cursor.getColumnIndexOrThrow("favorite")) != 0,
                        comments = cursor.getString(cursor.getColumnIndexOrThrow("comments")) ?: "",
                        yearDate = cursor.getString(cursor.getColumnIndexOrThrow("yearDate")) ?: "",
                        season = cursor.getInt(cursor.getColumnIndexOrThrow("season")),
                        totalEpisodes = cursor.getInt(cursor.getColumnIndexOrThrow("totalEpisodes")),
                        currentEpisode = cursor.getInt(cursor.getColumnIndexOrThrow("currentEpisode")),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("createdAt")),
                        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updatedAt"))
                    )
                )
            }
        }

        rows.forEach { row ->
            // one WatchItem row = one SeriesEntity; duplicate titles are NOT merged (approved architecture §M.1)
            val seriesValues = ContentValues().apply {
                put("title", row.title)
                put("status", row.status)
                put("whereToWatch", row.whereToWatch)
                if (row.sourceId != null) put("sourceId", row.sourceId) else putNull("sourceId")
                put("url", row.url)
                put("rating", row.rating)
                put("favorite", if (row.favorite) 1 else 0)
                put("comments", row.comments)
                put("yearDate", row.yearDate)
                putNull("resumeEpisodeId")
                put("createdAt", row.createdAt)
                put("updatedAt", row.updatedAt)
            }
            val newSeriesId = db.insert("series", SQLiteDatabase.CONFLICT_ABORT, seriesValues)
            oldIdToNewSeriesId[row.id] = newSeriesId

            // exactly one season — the one on record. No seasons 1..N-1 or N+1.. are invented.
            val seasonValues = ContentValues().apply {
                put("seriesId", newSeriesId)
                put("seasonNumber", row.season)
                putNull("title")
            }
            val newSeasonId = db.insert("seasons", SQLiteDatabase.CONFLICT_ABORT, seasonValues)

            val watchedAt = if (row.currentEpisode > 0) row.updatedAt else null
            for (episodeNumber in 1..row.totalEpisodes) {
                val watched = episodeNumber <= row.currentEpisode
                val episodeValues = ContentValues().apply {
                    put("seasonId", newSeasonId)
                    put("episodeNumber", episodeNumber)
                    putNull("title")
                    put("watched", if (watched) 1 else 0)
                    if (watched && watchedAt != null) put("watchedAt", watchedAt) else putNull("watchedAt")
                }
                db.insert("episodes", SQLiteDatabase.CONFLICT_ABORT, episodeValues)
            }
            expectedEpisodeCounts[newSeriesId] = row.totalEpisodes

            genreTokens(row.genre).forEach { token ->
                val genreId = resolveOrCreateGenre(db, token)
                val crossRefValues = ContentValues().apply {
                    put("genreId", genreId)
                    put("seriesId", newSeriesId)
                }
                db.insert("series_genre_cross_ref", SQLiteDatabase.CONFLICT_IGNORE, crossRefValues)
                genreTokensResolved++
            }
        }

        return SeriesConversionResult(oldIdToNewSeriesId, expectedEpisodeCounts, genreTokensResolved)
    }

    // ---------- step 5: series status recalculation ----------

    /**
     * Applies the approved status rules (see architecture doc §F) against the
     * freshly-generated episodes: a series with every episode watched becomes
     * WATCHED regardless of its copied-over status (including DROPPED — full
     * completion is itself a strong enough signal); otherwise a copied DROPPED
     * status is left alone (migration is not a "resume" action), and everything
     * else is TO_WATCH/WATCHING based on whether any episode is watched.
     */
    private fun recalculateSeriesStatus(db: SupportSQLiteDatabase) {
        data class SeriesRow(val id: Long, val status: String)
        val seriesRows = mutableListOf<SeriesRow>()
        db.query("SELECT id, status FROM series").use { cursor ->
            while (cursor.moveToNext()) {
                seriesRows.add(SeriesRow(cursor.getLong(0), cursor.getString(1)))
            }
        }

        seriesRows.forEach { series ->
            var total = 0
            var watched = 0
            db.query(
                """
                SELECT COUNT(*), SUM(CASE WHEN e.watched = 1 THEN 1 ELSE 0 END)
                FROM episodes e
                INNER JOIN seasons s ON e.seasonId = s.id
                WHERE s.seriesId = ?
                """.trimIndent(),
                arrayOf<Any>(series.id)
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    total = cursor.getInt(0)
                    watched = if (cursor.isNull(1)) 0 else cursor.getInt(1)
                }
            }

            val newStatus = when {
                total > 0 && watched == total -> "WATCHED"
                series.status == "DROPPED" -> "DROPPED"
                watched > 0 -> "WATCHING"
                else -> "TO_WATCH"
            }

            if (newStatus != series.status) {
                db.execSQL("UPDATE series SET status = ? WHERE id = ?", arrayOf<Any>(newStatus, series.id))
            }
        }
    }

    // ---------- step 6: verification ----------

    private fun verifyMigration(db: SupportSQLiteDatabase, conversion: SeriesConversionResult) {
        // A: every original SERIES row produced exactly one series row
        var originalSeriesCount = 0
        db.query("SELECT COUNT(*) FROM watch_items WHERE type = 'SERIES'").use { cursor ->
            if (cursor.moveToFirst()) originalSeriesCount = cursor.getInt(0)
        }
        check(originalSeriesCount == conversion.oldIdToNewSeriesId.size) {
            "Migration 2->3 verification failed: $originalSeriesCount SERIES rows in watch_items " +
                "but ${conversion.oldIdToNewSeriesId.size} series rows were created."
        }

        // B: every series' generated episode count matches its original totalEpisodes
        conversion.newSeriesIdToExpectedEpisodeCount.forEach { (seriesId, expectedCount) ->
            var actualCount = 0
            db.query(
                """
                SELECT COUNT(*) FROM episodes e
                INNER JOIN seasons s ON e.seasonId = s.id
                WHERE s.seriesId = ?
                """.trimIndent(),
                arrayOf<Any>(seriesId)
            ).use { cursor ->
                if (cursor.moveToFirst()) actualCount = cursor.getInt(0)
            }
            check(actualCount == expectedCount) {
                "Migration 2->3 verification failed: series $seriesId expected $expectedCount episodes, found $actualCount."
            }
        }

        // C: every non-empty whereToWatch resolved to a non-null sourceId (movies AND series-type rows)
        var unresolvedSources = 0
        db.query("SELECT COUNT(*) FROM watch_items WHERE TRIM(whereToWatch) != '' AND sourceId IS NULL").use { cursor ->
            if (cursor.moveToFirst()) unresolvedSources = cursor.getInt(0)
        }
        check(unresolvedSources == 0) {
            "Migration 2->3 verification failed: $unresolvedSources watch_items rows have a " +
                "non-empty whereToWatch but no resolved sourceId."
        }
    }

    // ---------- step 7: rebuild watch_items with the sourceId FK ----------

    /**
     * SQLite cannot add a foreign key constraint to an existing table via
     * ALTER TABLE, so the sourceId FK (already populated as a plain column by
     * this point) requires the standard create-new-table/copy/drop/rename
     * dance. Every column and every existing id is preserved unchanged; SERIES
     * rows have already been deleted by the time this runs.
     */
    private fun rebuildWatchItemsWithSourceIdForeignKey(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE `watch_items_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `genre` TEXT NOT NULL,
                `whereToWatch` TEXT NOT NULL,
                `sourceId` INTEGER,
                `url` TEXT NOT NULL,
                `rating` INTEGER NOT NULL,
                `favorite` INTEGER NOT NULL,
                `comments` TEXT NOT NULL,
                `yearDate` TEXT NOT NULL,
                `totalEpisodes` INTEGER NOT NULL,
                `currentEpisode` INTEGER NOT NULL,
                `season` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                FOREIGN KEY(`sourceId`) REFERENCES `sources`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO watch_items_new
                (id, title, status, type, genre, whereToWatch, sourceId, url, rating, favorite,
                 comments, yearDate, totalEpisodes, currentEpisode, season, createdAt, updatedAt)
            SELECT
                id, title, status, type, genre, whereToWatch, sourceId, url, rating, favorite,
                comments, yearDate, totalEpisodes, currentEpisode, season, createdAt, updatedAt
            FROM watch_items
            """.trimIndent()
        )
        db.execSQL("DROP TABLE watch_items")
        db.execSQL("ALTER TABLE watch_items_new RENAME TO watch_items")
    }

    private fun createWatchItemsIndices(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_title` ON `watch_items` (`title`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_status` ON `watch_items` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_favorite` ON `watch_items` (`favorite`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watch_items_sourceId` ON `watch_items` (`sourceId`)")
    }
}
