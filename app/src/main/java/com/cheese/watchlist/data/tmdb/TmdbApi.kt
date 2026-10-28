package com.cheese.watchlist.data.tmdb

import com.cheese.watchlist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/** Lightweight TMDB v3 client. Uses the bearer token from local.properties. */
class TmdbApi {

    companion object {
        private const val BASE = "https://api.themoviedb.org/3"
        private const val IMAGE = "https://image.tmdb.org/t/p/"
    }

    data class Item(
        val id: Int,
        val mediaType: String,
        val title: String,
        val overview: String,
        val posterPath: String?,
        val backdropPath: String?,
        val year: String,
        val rating: Double,
        val genres: List<String> = emptyList(),
        val tmdbUrl: String
    ) {
        val posterUrl: String?
            get() = posterPath?.let {
                IMAGE + "w500" + it
            }

        val backdropUrl: String?
            get() = backdropPath?.let {
                IMAGE + "w780" + it
            }
    }

    data class TvSeason(
        val number: Int,
        val episodeCount: Int,
        val name: String
    )

    data class Details(
        val item: Item,
        val seasons: List<TvSeason> = emptyList(),
        val trailerUrl: String? = null,
        val stills: List<String> = emptyList(),
        val similar: List<Item> = emptyList(),
        val providers: List<String> = emptyList(),
        val country: String = "",
        val language: String = "",
        val director: String = "",
        val writers: List<String> = emptyList(),
        val cast: List<String> = emptyList(),

        // NEW
        val runtimeMinutes: Int = 0,
        val contentRating: String = ""
    )

    suspend fun trending(): List<Item> =
        list("/trending/all/day")

    suspend fun trendingDrama(): List<Item> {
        val results = mutableListOf<Item>()

        for (page in 1..3) {
            results += list(
                "/trending/tv/day",
                page
            )
        }

        return results.filter { item ->
            item.genres.contains("Drama")
        }
    }

    suspend fun trendingPage(page: Int): List<Item> =
        list("/trending/all/day", page.coerceAtLeast(1))

    suspend fun nowPlaying(): List<Item> =
        list("/movie/now_playing?region=PH")

    suspend fun nowPlayingPage(page: Int): List<Item> =
        list(
            "/movie/now_playing?region=PH",
            page.coerceAtLeast(1)
        )

    suspend fun upcoming(): List<Item> =
        list("/movie/upcoming?region=PH")

    suspend fun upcomingPage(page: Int): List<Item> =
        list(
            "/movie/upcoming?region=PH",
            page.coerceAtLeast(1)
        )


    suspend fun search(query: String): List<Item> =
        list(
            "/search/multi?include_adult=false&query=" +
                    URLEncoder.encode(query, "UTF-8")
        ).filter {
            it.mediaType == "movie" ||
                    it.mediaType == "tv"
        }

    suspend fun details(item: Item): Details =
        details(
            item.id,
            item.mediaType,
            item
        )

    suspend fun details(
        id: Int,
        mediaType: String
    ): Details {

        val type =
            if (mediaType == "tv")
                "tv"
            else
                "movie"

        val seed = Item(
            id = id,
            mediaType = type,
            title = "",
            overview = "",
            posterPath = null,
            backdropPath = null,
            year = "",
            rating = 0.0,
            genres = emptyList(),
            tmdbUrl =
                "https://www.themoviedb.org/$type/$id"
        )

        return details(
            id,
            mediaType,
            seed
        )
    }

    private suspend fun details(
        id: Int,
        mediaType: String,
        seed: Item
    ): Details {

        val type =
            if (mediaType == "tv")
                "tv"
            else
                "movie"

        // ---------------------------------------------------------
        // BASIC DETAILS
        // ---------------------------------------------------------

        val json =
            get(
                "/$type/$id?language=en-US"
            )

        val genres =
            json.optJSONArray("genres")?.let { array ->

                (0 until array.length())
                    .mapNotNull { index ->

                        array
                            .optJSONObject(index)
                            ?.optString("name")
                            ?.takeIf(
                                String::isNotBlank
                            )
                    }

            } ?: seed.genres

        val dateKey =
            if (type == "tv")
                "first_air_date"
            else
                "release_date"

        val titleKey =
            if (type == "tv")
                "name"
            else
                "title"

        val updated =
            seed.copy(

                title =
                    json.optString(
                        titleKey,
                        seed.title
                    ),

                overview =
                    json.optString(
                        "overview",
                        seed.overview
                    ),

                posterPath =
                    json.optString(
                        "poster_path",
                        seed.posterPath
                    ).takeIf {
                        it != "null" &&
                                it.isNotBlank()
                    },

                backdropPath =
                    json.optString(
                        "backdrop_path",
                        seed.backdropPath
                    ).takeIf {
                        it != "null" &&
                                it.isNotBlank()
                    },

                year =
                    json.optString(
                        dateKey,
                        seed.year
                    ).take(4),

                rating =
                    json.optDouble(
                        "vote_average",
                        seed.rating
                    ),

                genres = genres
            )

        // ---------------------------------------------------------
        // RUNTIME
        // ---------------------------------------------------------

        val runtimeMinutes =
            if (type == "movie") {

                json.optInt(
                    "runtime",
                    0
                )

            } else {

                val episodeRuntime =
                    json.optJSONArray(
                        "episode_run_time"
                    )

                episodeRuntime
                    ?.optInt(0, 0)
                    ?: 0
            }

        // ---------------------------------------------------------
        // TV SEASONS
        // ---------------------------------------------------------

        val seasons =
            if (type == "tv") {

                val array =
                    json.optJSONArray(
                        "seasons"
                    )

                if (array == null) {

                    emptyList()

                } else {

                    (0 until array.length())
                        .mapNotNull { index ->

                            val season =
                                array.optJSONObject(
                                    index
                                )
                                    ?: return@mapNotNull null

                            val number =
                                season.optInt(
                                    "season_number",
                                    -1
                                )

                            if (number <= 0) {

                                null

                            } else {

                                TvSeason(
                                    number = number,
                                    episodeCount =
                                        season.optInt(
                                            "episode_count",
                                            0
                                        ),
                                    name =
                                        season.optString(
                                            "name",
                                            "Season $number"
                                        )
                                )
                            }
                        }
                }

            } else {

                emptyList()
            }

        // ---------------------------------------------------------
        // CONTENT RATING / AGE RATING
        // ---------------------------------------------------------

        val contentRating =
            if (type == "movie") {

                getMovieCertification(id)

            } else {

                getTvCertification(id)
            }

        // ---------------------------------------------------------
        // CREDITS
        // ---------------------------------------------------------

        val credits =
            runCatching {
                get(
                    "/$type/$id/credits?language=en-US"
                )
            }.getOrNull()

        val crew =
            credits?.optJSONArray("crew")

        val director =
            crew?.let { array ->

                (0 until array.length())
                    .mapNotNull {
                        array.optJSONObject(it)
                    }
                    .firstOrNull {
                        it.optString("job") ==
                                "Director"
                    }
                    ?.optString("name")
                    .orEmpty()

            }.orEmpty()

        val writers =
            crew?.let { array ->

                (0 until array.length())
                    .mapNotNull {
                        array.optJSONObject(it)
                    }
                    .filter {

                        it.optString(
                            "department"
                        ) == "Writing" ||

                                it.optString(
                                    "job"
                                ).contains(
                                    "Writer",
                                    true
                                )
                    }
                    .mapNotNull {
                        it.optString("name")
                            .takeIf(
                                String::isNotBlank
                            )
                    }
                    .distinct()
                    .take(4)

            } ?: emptyList()

        val cast =
            credits
                ?.optJSONArray("cast")
                ?.let { array ->

                    (0 until array.length())
                        .mapNotNull {

                            array
                                .optJSONObject(it)
                                ?.optString("name")
                                ?.takeIf(
                                    String::isNotBlank
                                )
                        }
                        .take(6)

                } ?: emptyList()

        // ---------------------------------------------------------
        // TRAILER
        // ---------------------------------------------------------

        val videos =
            runCatching {
                get(
                    "/$type/$id/videos?language=en-US"
                )
            }.getOrNull()

        val trailerKey =
            videos
                ?.optJSONArray("results")
                ?.let { array ->

                    (0 until array.length())
                        .mapNotNull {
                            array.optJSONObject(it)
                        }
                        .firstOrNull {

                            it.optString(
                                "site"
                            ) == "YouTube" &&

                                    it.optString(
                                        "type"
                                    ) == "Trailer"
                        }
                        ?.optString("key")
                }

        val trailerUrl =
            trailerKey?.let {
                "https://www.youtube.com/watch?v=$it"
            }

        // ---------------------------------------------------------
        // STILLS
        // ---------------------------------------------------------

        val images =
            runCatching {
                get(
                    "/$type/$id/images" +
                            "?include_image_language=en,null"
                )
            }.getOrNull()

        val stills =
            images
                ?.optJSONArray("backdrops")
                ?.let { array ->

                    (0 until array.length())
                        .mapNotNull {

                            array
                                .optJSONObject(it)
                                ?.optString(
                                    "file_path"
                                )
                                ?.takeIf(
                                    String::isNotBlank
                                )
                                ?.let { path ->
                                    IMAGE +
                                            "w780" +
                                            path
                                }
                        }
                        .take(8)

                } ?: emptyList()

        // ---------------------------------------------------------
        // SIMILAR
        // ---------------------------------------------------------

        val similar =
            runCatching {
                list(
                    "/$type/$id/similar"
                )
            }
                .getOrDefault(
                    emptyList()
                )
                .take(10)

        // ---------------------------------------------------------
        // WATCH PROVIDERS
        // ---------------------------------------------------------

        val providersJson =
            runCatching {
                get(
                    "/$type/$id/watch/providers"
                )
            }.getOrNull()

        val providers =
            providersJson
                ?.optJSONObject("results")
                ?.optJSONObject("PH")
                ?.let { ph ->

                    listOf(
                        "flatrate",
                        "free",
                        "ads",
                        "rent",
                        "buy"
                    )
                        .flatMap { key ->

                            val array =
                                ph.optJSONArray(key)
                                    ?: return@flatMap emptyList()

                            (0 until array.length())
                                .mapNotNull {

                                    array
                                        .optJSONObject(it)
                                        ?.optString(
                                            "provider_name"
                                        )
                                        ?.takeIf(
                                            String::isNotBlank
                                        )
                                }
                        }
                        .distinct()
                        .take(8)

                } ?: emptyList()

        // ---------------------------------------------------------
        // COUNTRY / LANGUAGE
        // ---------------------------------------------------------

        val country =
            json
                .optJSONArray(
                    "production_countries"
                )
                ?.optJSONObject(0)
                ?.optString("name")
                .orEmpty()

        val language =
            json
                .optString(
                    "original_language"
                )
                .takeIf(
                    String::isNotBlank
                )
                .orEmpty()

        return Details(
            item = updated,
            seasons = seasons,
            trailerUrl = trailerUrl,
            stills = stills,
            similar = similar,
            providers = providers,
            country = country,
            language = language,
            director = director,
            writers = writers,
            cast = cast,
            runtimeMinutes = runtimeMinutes,
            contentRating = contentRating
        )
    }

    // =============================================================
    // MOVIE AGE RATING
    // =============================================================

    private suspend fun getMovieCertification(
        id: Int
    ): String {

        val json =
            runCatching {
                get(
                    "/movie/$id/release_dates"
                )
            }.getOrNull()
                ?: return ""

        val results =
            json.optJSONArray(
                "results"
            )
                ?: return ""

        fun findCountry(
            code: String
        ): String {

            for (i in 0 until results.length()) {

                val country =
                    results.optJSONObject(i)
                        ?: continue

                if (
                    country.optString(
                        "iso_3166_1"
                    ) != code
                ) {
                    continue
                }

                val dates =
                    country.optJSONArray(
                        "release_dates"
                    )
                        ?: continue

                for (j in 0 until dates.length()) {

                    val certification =
                        dates
                            .optJSONObject(j)
                            ?.optString(
                                "certification"
                            )
                            ?.trim()
                            .orEmpty()

                    if (
                        certification.isNotBlank()
                    ) {
                        return certification
                    }
                }
            }

            return ""
        }

        // Philippines first
        val ph =
            findCountry("PH")

        if (ph.isNotBlank()) {
            return normalizeCertification(ph)
        }

        // US fallback
        return normalizeCertification(
            findCountry("US")
        )
    }

    // =============================================================
    // TV AGE RATING
    // =============================================================

    private suspend fun getTvCertification(
        id: Int
    ): String {

        val json =
            runCatching {
                get(
                    "/tv/$id/content_ratings"
                )
            }.getOrNull()
                ?: return ""

        val results =
            json.optJSONArray(
                "results"
            )
                ?: return ""

        fun findCountry(
            code: String
        ): String {

            for (i in 0 until results.length()) {

                val country =
                    results.optJSONObject(i)
                        ?: continue

                if (
                    country.optString(
                        "iso_3166_1"
                    ) == code
                ) {

                    return country
                        .optString(
                            "rating"
                        )
                        .trim()
                }
            }

            return ""
        }

        val ph =
            findCountry("PH")

        return if (
            ph.isNotBlank()
        ) {

            normalizeCertification(ph)

        } else {

            normalizeCertification(
                findCountry("US")
            )
        }
    }

    // =============================================================
    // NORMALIZE AGE RATINGS
    // =============================================================

    private fun normalizeCertification(
        value: String
    ): String {

        val clean =
            value.trim()

        return when {

            clean.equals(
                "R-16",
                true
            ) -> "16+"

            clean.equals(
                "R16",
                true
            ) -> "16+"

            clean.equals(
                "16",
                true
            ) -> "16+"

            clean.equals(
                "R-18",
                true
            ) -> "18+"

            clean.equals(
                "R18",
                true
            ) -> "18+"

            clean.equals(
                "18",
                true
            ) -> "18+"

            else -> clean
        }
    }

    // =============================================================
    // LIST
    // =============================================================

    private suspend fun list(
        path: String,
        page: Int = 1
    ): List<Item> {

        val json =
            get(
                "$path" +
                        "${if ('?' in path) "&" else "?"}" +
                        "language=en-US&page=$page"
            )

        val array =
            json.optJSONArray(
                "results"
            )
                ?: return emptyList()

        return (0 until array.length())
            .mapNotNull {
                parseItem(
                    array.optJSONObject(it)
                )
            }
    }

    // =============================================================
    // PARSE ITEM
    // =============================================================

    private fun parseItem(
        o: JSONObject?
    ): Item? {

        if (o == null) {
            return null
        }

        val media =
            o.optString(
                "media_type"
            ).ifBlank {

                when {

                    o.has("first_air_date") ||
                            o.has("name") ->
                        "tv"

                    else ->
                        "movie"
                }
            }

        if (
            media != "movie" &&
            media != "tv"
        ) {
            return null
        }

        val title =
            o.optString(
                if (media == "tv")
                    "name"
                else
                    "title"
            ).trim()

        if (title.isBlank()) {
            return null
        }

        val date =
            o.optString(
                if (media == "tv")
                    "first_air_date"
                else
                    "release_date"
            )

        // TMDB list/similar endpoints return `genre_ids`, not the
        // full `genres` objects used by the details endpoint. Convert
        // those IDs here so recommendation filtering can actually see
        // categories such as Animation, Family, Adventure and Fantasy.
        val genreIds = o.optJSONArray("genre_ids")
        val genresFromIds = if (genreIds != null) {
            (0 until genreIds.length())
                .mapNotNull { index ->
                    genreName(media, genreIds.optInt(index, -1))
                }
        } else {
            emptyList()
        }

        return Item(

            id =
                o.optInt("id"),

            mediaType =
                media,

            title =
                title,

            overview =
                o.optString("overview"),

            posterPath =
                o.optString(
                    "poster_path"
                ).takeIf {
                    it.isNotBlank() &&
                            it != "null"
                },

            backdropPath =
                o.optString(
                    "backdrop_path"
                ).takeIf {
                    it.isNotBlank() &&
                            it != "null"
                },

            year =
                date.take(4),

            rating =
                o.optDouble(
                    "vote_average",
                    0.0
                ),

            genres =
                genresFromIds,

            tmdbUrl =
                "https://www.themoviedb.org/" +
                        "${if (media == "tv") "tv" else "movie"}/" +
                        o.optInt("id")
        )
    }

    // TMDB uses different genre IDs for movies and TV.
    private fun genreName(
        mediaType: String,
        id: Int
    ): String? {
        val movieGenres = mapOf(
            28 to "Action",
            12 to "Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            14 to "Fantasy",
            36 to "History",
            27 to "Horror",
            10402 to "Music",
            9648 to "Mystery",
            10749 to "Romance",
            878 to "Science Fiction",
            10770 to "TV Movie",
            53 to "Thriller",
            10752 to "War",
            37 to "Western"
        )

        val tvGenres = mapOf(
            10759 to "Action & Adventure",
            16 to "Animation",
            35 to "Comedy",
            80 to "Crime",
            99 to "Documentary",
            18 to "Drama",
            10751 to "Family",
            10762 to "Kids",
            9648 to "Mystery",
            10763 to "News",
            10764 to "Reality",
            10765 to "Sci-Fi & Fantasy",
            10766 to "Soap",
            10767 to "Talk",
            10768 to "War & Politics",
            37 to "Western"
        )

        return (if (mediaType == "tv") tvGenres else movieGenres)[id]
    }


// =============================================================
// HTTP
// =============================================================

    private suspend fun get(
        path: String
    ): JSONObject =
        withContext(Dispatchers.IO) {

            require(
                BuildConfig.TMDB_API_KEY.isNotBlank()
            ) {
                "TMDB API key is missing. " +
                        "Add TMDB_API_KEY=... to local.properties."
            }

            val connection =
                (
                        URL(
                            BASE + path
                        ).openConnection()
                                as HttpURLConnection
                        ).apply {

                        requestMethod = "GET"

                        connectTimeout =
                            15_000

                        readTimeout =
                            20_000

                        setRequestProperty(
                            "Authorization",
                            "Bearer ${BuildConfig.TMDB_API_KEY}"
                        )

                        setRequestProperty(
                            "accept",
                            "application/json"
                        )
                    }

            try {

                val code =
                    connection.responseCode

                val stream =
                    if (
                        code in 200..299
                    ) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val body =
                    stream
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        .orEmpty()

                if (
                    code !in 200..299
                ) {
                    error(
                        "TMDB request failed " +
                                "($code): $body"
                    )
                }

                JSONObject(body)

            } finally {

                connection.disconnect()
            }
        }
}