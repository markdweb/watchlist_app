package com.cheese.watchlist.data.database.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cheese.watchlist.data.database.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the Phase 0A required checklist (A-P from the implementation request):
 *  A/B/C  existing movie survives with its id and metadata intact
 *  D/E    existing movie genre/source migrate into the relational tables
 *  F/G/H/I existing series becomes SeriesEntity + the one season on record +
 *          the correct episode count, watched/unwatched split at currentEpisode
 *  J/K    existing series genre/source migrate correctly
 *  L      duplicate series titles are NOT merged into one SeriesEntity
 *  M/N    collection and tag tables exist with the expected shape
 *  O      foreign keys / unique constraints are enforced as designed
 *  P      no destructive fallback is used — verified by code inspection of
 *         AppDatabase.getInstance() (no fallbackToDestructiveMigration() call),
 *         not a runtime assertion, since a destructive fallback wouldn't leave
 *         anything to assert against in the first place.
 *
 * Requires a device/emulator to run (MigrationTestHelper needs Instrumentation)
 * and the exported schema JSON under app/schemas — see the sourceSets block
 * added to app/build.gradle.kts.
 */
@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    private val testDbName = "migration-test-2-3"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrate2To3_preservesMoviesAndConvertsSeriesAsExpected() {
        var db = helper.createDatabase(testDbName, 2)

        // --- seed genres/sources exactly as they'd exist pre-migration ---
        db.execSQL("INSERT INTO genres (id, name) VALUES (1, 'Action')")
        db.execSQL("INSERT INTO genres (id, name) VALUES (2, 'Horror')")
        db.execSQL("INSERT INTO sources (id, name) VALUES (1, 'Netflix')")

        // --- A/B/C/D/E: one existing movie ---
        db.execSQL(
            """
            INSERT INTO watch_items
                (id, title, status, type, genre, whereToWatch, url, rating, favorite,
                 comments, yearDate, totalEpisodes, currentEpisode, season, createdAt, updatedAt)
            VALUES
                (100, 'Wrong Turn', 'WATCHED', 'MOVIE', 'Action, Horror', 'Netflix', 'http://example.com',
                 4, 1, 'Great movie', '2003', 0, 0, 1, 1000, 2000)
            """.trimIndent()
        )

        // --- F/G/H/I/J/K: one existing series, Season 5, 16 episodes, 8 watched ---
        db.execSQL(
            """
            INSERT INTO watch_items
                (id, title, status, type, genre, whereToWatch, url, rating, favorite,
                 comments, yearDate, totalEpisodes, currentEpisode, season, createdAt, updatedAt)
            VALUES
                (200, 'The Walking Dead', 'WATCHING', 'SERIES', 'Action', 'Netflix', '',
                 5, 0, '', '2010', 16, 8, 5, 1500, 2500)
            """.trimIndent()
        )

        // --- L: two SERIES rows sharing the same title, must NOT be merged ---
        db.execSQL(
            """
            INSERT INTO watch_items
                (id, title, status, type, genre, whereToWatch, url, rating, favorite,
                 comments, yearDate, totalEpisodes, currentEpisode, season, createdAt, updatedAt)
            VALUES
                (300, 'Duplicate Show', 'TO_WATCH', 'SERIES', '', '', '', 0, 0, '', '', 10, 0, 1, 1600, 1600)
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO watch_items
                (id, title, status, type, genre, whereToWatch, url, rating, favorite,
                 comments, yearDate, totalEpisodes, currentEpisode, season, createdAt, updatedAt)
            VALUES
                (301, 'Duplicate Show', 'TO_WATCH', 'SERIES', '', '', '', 0, 0, '', '', 8, 0, 2, 1601, 1601)
            """.trimIndent()
        )

        db.close()

        // --- run the real migration under test ---
        db = helper.runMigrationsAndValidate(testDbName, 3, true, Migration2To3)

        // MigrationTestHelper returns the raw post-migration database without
        // going through AppDatabase's onOpen callback, so FK enforcement must be
        // turned on explicitly here to exercise CASCADE/unique behavior the way
        // the real running app will (see AppDatabase.onOpen).
        db.execSQL("PRAGMA foreign_keys = ON")

        // A/B: movie survives with its original id
        db.query("SELECT * FROM watch_items WHERE id = 100").use { cursor ->
            assertTrue("Movie row with id=100 should survive migration", cursor.moveToFirst())
            // C: metadata intact
            assertEquals("Wrong Turn", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("WATCHED", cursor.getString(cursor.getColumnIndexOrThrow("status")))
            assertEquals(4, cursor.getInt(cursor.getColumnIndexOrThrow("rating")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("favorite")))
            assertEquals("Great movie", cursor.getString(cursor.getColumnIndexOrThrow("comments")))
            assertEquals("2003", cursor.getString(cursor.getColumnIndexOrThrow("yearDate")))
            // E: source resolved
            val sourceId = cursor.getLong(cursor.getColumnIndexOrThrow("sourceId"))
            assertEquals(1L, sourceId)
        }

        // D: movie genres migrated into MovieGenreCrossRef (Action + Horror)
        var movieGenreCount = 0
        db.query(
            """
            SELECT COUNT(*) FROM movie_genre_cross_ref mg
            INNER JOIN genres g ON mg.genreId = g.id
            WHERE mg.movieId = 100 AND g.name IN ('Action', 'Horror')
            """.trimIndent()
        ).use { cursor -> if (cursor.moveToFirst()) movieGenreCount = cursor.getInt(0) }
        assertEquals("Both genres should be linked to the movie", 2, movieGenreCount)

        // F: series row no longer exists in watch_items, converted to SeriesEntity
        db.query("SELECT COUNT(*) FROM watch_items WHERE type = 'SERIES'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("No SERIES rows should remain in watch_items", 0, cursor.getInt(0))
        }
        var twdSeriesId = -1L
        db.query("SELECT id, sourceId FROM series WHERE title = 'The Walking Dead'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            twdSeriesId = cursor.getLong(0)
            // K: series source resolved
            assertEquals(1L, cursor.getLong(1))
        }
        assertTrue(twdSeriesId > 0)

        // G: exactly one season, Season 5 (no invented seasons 1-4 or 6+)
        db.query("SELECT COUNT(*), MIN(seasonNumber), MAX(seasonNumber) FROM seasons WHERE seriesId = $twdSeriesId").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Exactly one season should exist for this series", 1, cursor.getInt(0))
            assertEquals(5, cursor.getInt(1))
            assertEquals(5, cursor.getInt(2))
        }
        var season5Id = -1L
        db.query("SELECT id FROM seasons WHERE seriesId = $twdSeriesId AND seasonNumber = 5").use { cursor ->
            assertTrue(cursor.moveToFirst())
            season5Id = cursor.getLong(0)
        }

        // H: episode count matches totalEpisodes (16), not more, not fewer
        db.query("SELECT COUNT(*) FROM episodes WHERE seasonId = $season5Id").use { cursor ->
            cursor.moveToFirst()
            assertEquals(16, cursor.getInt(0))
        }

        // I: episodes 1-8 watched, 9-16 unwatched
        db.query("SELECT COUNT(*) FROM episodes WHERE seasonId = $season5Id AND watched = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(8, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM episodes WHERE seasonId = $season5Id AND watched = 0").use { cursor ->
            cursor.moveToFirst()
            assertEquals(8, cursor.getInt(0))
        }
        db.query("SELECT MAX(episodeNumber) FROM episodes WHERE seasonId = $season5Id AND watched = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(8, cursor.getInt(0))
        }
        db.query("SELECT MIN(episodeNumber) FROM episodes WHERE seasonId = $season5Id AND watched = 0").use { cursor ->
            cursor.moveToFirst()
            assertEquals(9, cursor.getInt(0))
        }

        // J: series genre migrated
        var seriesGenreCount = 0
        db.query(
            """
            SELECT COUNT(*) FROM series_genre_cross_ref sg
            INNER JOIN genres g ON sg.genreId = g.id
            WHERE sg.seriesId = $twdSeriesId AND g.name = 'Action'
            """.trimIndent()
        ).use { cursor -> if (cursor.moveToFirst()) seriesGenreCount = cursor.getInt(0) }
        assertEquals(1, seriesGenreCount)

        // Status recalculation: 8/16 watched -> WATCHING (unchanged from original in this case)
        db.query("SELECT status FROM series WHERE id = $twdSeriesId").use { cursor ->
            cursor.moveToFirst()
            assertEquals("WATCHING", cursor.getString(0))
        }

        // L: duplicate-titled series are NOT merged -> two separate series rows
        db.query("SELECT id, title FROM series WHERE title = 'Duplicate Show'").use { cursor ->
            var count = 0
            while (cursor.moveToNext()) count++
            assertEquals("Duplicate-titled series must remain as separate rows", 2, count)
        }

        // M: collection tables exist and accept a row with the expected shape
        db.execSQL("INSERT INTO collections (id, title, comments, createdAt, updatedAt) VALUES (1, 'Wrong Turn Collection', '', 1, 1)")
        db.execSQL("INSERT INTO collection_movie_cross_ref (collectionId, movieId, watchOrder) VALUES (1, 100, 1)")
        db.query("SELECT COUNT(*) FROM collection_movie_cross_ref WHERE collectionId = 1 AND movieId = 100").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        // removing a movie from a collection must not delete the movie
        db.execSQL("DELETE FROM collection_movie_cross_ref WHERE collectionId = 1 AND movieId = 100")
        db.query("SELECT COUNT(*) FROM watch_items WHERE id = 100").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Movie must still exist after removal from a collection", 1, cursor.getInt(0))
        }

        // N: tag tables exist and accept a row for both a movie and a series
        db.execSQL("INSERT INTO tags (id, name) VALUES (1, 'Rewatch')")
        db.execSQL("INSERT INTO item_tag_cross_ref (tagId, itemId, itemType) VALUES (1, 100, 'MOVIE')")
        db.execSQL("INSERT INTO item_tag_cross_ref (tagId, itemId, itemType) VALUES (1, $twdSeriesId, 'SERIES')")
        db.query("SELECT COUNT(*) FROM item_tag_cross_ref WHERE tagId = 1").use { cursor ->
            cursor.moveToFirst()
            assertEquals(2, cursor.getInt(0))
        }

        // O: unique constraints are enforced — a duplicate (seriesId, seasonNumber) must fail
        var uniqueViolationThrown = false
        try {
            db.execSQL("INSERT INTO seasons (seriesId, seasonNumber) VALUES ($twdSeriesId, 5)")
        } catch (e: Exception) {
            uniqueViolationThrown = true
        }
        assertTrue("Duplicate (seriesId, seasonNumber) must violate the unique index", uniqueViolationThrown)

        // O: FK cascade — deleting the series should cascade-delete its season and episodes
        db.execSQL("DELETE FROM series WHERE id = $twdSeriesId")
        db.query("SELECT COUNT(*) FROM seasons WHERE seriesId = $twdSeriesId").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Deleting a series must cascade-delete its seasons", 0, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM episodes WHERE seasonId = $season5Id").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Deleting a series must cascade-delete its episodes", 0, cursor.getInt(0))
        }

        db.close()
    }
}
