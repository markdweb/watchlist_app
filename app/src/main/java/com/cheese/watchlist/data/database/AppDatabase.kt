package com.cheese.watchlist.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cheese.watchlist.data.dao.CollectionDao
import com.cheese.watchlist.data.dao.CollectionMovieCrossRefDao
import com.cheese.watchlist.data.dao.EpisodeDao
import com.cheese.watchlist.data.dao.GenreDao
import com.cheese.watchlist.data.dao.ItemTagCrossRefDao
import com.cheese.watchlist.data.dao.MovieGenreCrossRefDao
import com.cheese.watchlist.data.dao.SeasonDao
import com.cheese.watchlist.data.dao.SeriesDao
import com.cheese.watchlist.data.dao.SeriesGenreCrossRefDao
import com.cheese.watchlist.data.dao.SourceDao
import com.cheese.watchlist.data.dao.TagDao
import com.cheese.watchlist.data.dao.WatchItemDao
import com.cheese.watchlist.data.database.migrations.Migration2To3
import com.cheese.watchlist.data.entities.CollectionEntity
import com.cheese.watchlist.data.entities.CollectionMovieCrossRef
import com.cheese.watchlist.data.entities.Converters
import com.cheese.watchlist.data.entities.EpisodeEntity
import com.cheese.watchlist.data.entities.GenreEntity
import com.cheese.watchlist.data.entities.ItemTagCrossRef
import com.cheese.watchlist.data.entities.MovieGenreCrossRef
import com.cheese.watchlist.data.entities.SeasonEntity
import com.cheese.watchlist.data.entities.SeriesEntity
import com.cheese.watchlist.data.entities.SeriesGenreCrossRef
import com.cheese.watchlist.data.entities.SourceEntity
import com.cheese.watchlist.data.entities.TagEntity
import com.cheese.watchlist.data.entities.WatchItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WatchItem::class,
        GenreEntity::class,
        SourceEntity::class,
        SeriesEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        CollectionEntity::class,
        CollectionMovieCrossRef::class,
        MovieGenreCrossRef::class,
        SeriesGenreCrossRef::class,
        TagEntity::class,
        ItemTagCrossRef::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun watchItemDao(): WatchItemDao
    abstract fun genreDao(): GenreDao
    abstract fun sourceDao(): SourceDao
    abstract fun seriesDao(): SeriesDao
    abstract fun seasonDao(): SeasonDao
    abstract fun episodeDao(): EpisodeDao
    abstract fun collectionDao(): CollectionDao
    abstract fun collectionMovieCrossRefDao(): CollectionMovieCrossRefDao
    abstract fun movieGenreCrossRefDao(): MovieGenreCrossRefDao
    abstract fun seriesGenreCrossRefDao(): SeriesGenreCrossRefDao
    abstract fun tagDao(): TagDao
    abstract fun itemTagCrossRefDao(): ItemTagCrossRefDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        val DEFAULT_GENRES = listOf(
            "Anime",
            "Comedy",
            "Adventure",
            "Fantasy",
            "Horror",
            "Thriller",
            "Romance",
            "Mystery",
            "Action"
        )

        val DEFAULT_SOURCES = listOf(
            "Cinemas",
            "Netflix",
            "Hurawatch",
            "Moviebox",
            "9anime"
        )

        /**
         * v1 -> v2:
         * Adds season tracking to watch_items, cleans up duplicate
         * genres/sources, and enforces uniqueness.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE watch_items ADD COLUMN season INTEGER NOT NULL DEFAULT 1"
                )

                db.execSQL(
                    """
                    DELETE FROM genres
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM genres
                        GROUP BY name COLLATE NOCASE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    DELETE FROM sources
                    WHERE id NOT IN (
                        SELECT MIN(id)
                        FROM sources
                        GROUP BY name COLLATE NOCASE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_genres_name ON genres(name)"
                )

                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_sources_name ON sources(name)"
                )
            }
        }

        /**
         * v3 -> v4:
         * Adds TMDB metadata to watch_items.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE watch_items ADD COLUMN tmdbId INTEGER DEFAULT NULL"
                )

                db.execSQL(
                    "ALTER TABLE watch_items ADD COLUMN posterPath TEXT DEFAULT NULL"
                )

                db.execSQL(
                    "ALTER TABLE watch_items ADD COLUMN backdropPath TEXT DEFAULT NULL"
                )
            }
        }

        /**
         * v4 -> v5:
         * Adds a separate custom watch URL for series.
         *
         * SeriesEntity.url remains reserved for the TMDB URL/identity.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE series ADD COLUMN watchUrl TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * v5 -> v6:
         * Adds a separate custom watch URL for movies.
         *
         * WatchItem.url remains reserved for the TMDB URL/identity.
         */
        private val MIGRATION_5_6 = object : Migration(5, 6) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    "ALTER TABLE watch_items ADD COLUMN watchUrl TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "watchlist.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        Migration2To3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .addCallback(
                        object : RoomDatabase.Callback() {

                            override fun onCreate(
                                db: SupportSQLiteDatabase
                            ) {
                                super.onCreate(db)

                                CoroutineScope(Dispatchers.IO).launch {

                                    val database = INSTANCE

                                    database?.genreDao()?.insertAll(
                                        DEFAULT_GENRES.map {
                                            GenreEntity(name = it)
                                        }
                                    )

                                    database?.sourceDao()?.insertAll(
                                        DEFAULT_SOURCES.map {
                                            SourceEntity(name = it)
                                        }
                                    )
                                }
                            }

                            /**
                             * SQLite requires PRAGMA foreign_keys = ON
                             * for FK behavior to be enforced.
                             */
                            override fun onOpen(
                                db: SupportSQLiteDatabase
                            ) {
                                super.onOpen(db)

                                db.execSQL(
                                    "PRAGMA foreign_keys = ON"
                                )
                            }
                        }
                    )
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}