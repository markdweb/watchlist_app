package com.cheese.watchlist.navigation

sealed class Screen(val route: String) {

    // =========================================================
    // MAIN NAVIGATION
    // =========================================================

    data object Home : Screen("home")

    data object Lists : Screen("lists")

    data object Profile : Screen("profile")


    // =========================================================
    // OTHER SCREENS
    // =========================================================

    data object Genres : Screen("genres")

    data object Settings : Screen("settings")

    data object ManageSources : Screen("manage_sources")

    data object PcManager : Screen("pc_manager")


    // =========================================================
    // SERIES
    // =========================================================

    data object SeriesEditor : Screen("series_editor?seriesId={seriesId}") {
        fun createRoute(id: Long? = null) =
            "series_editor?seriesId=${id ?: -1L}"
    }

    data object SeriesDetail : Screen("series_detail/{seriesId}") {
        fun createRoute(id: Long) =
            "series_detail/$id"
    }


    // =========================================================
    // COLLECTIONS
    // =========================================================

    data object Collections : Screen("collections")

    data object CollectionEditor :
        Screen("collection_editor?collectionId={collectionId}") {

        fun createRoute(id: Long? = null) =
            "collection_editor?collectionId=${id ?: -1L}"
    }

    data object CollectionDetail :
        Screen("collection_detail/{collectionId}") {

        fun createRoute(id: Long) =
            "collection_detail/$id"
    }


    // =========================================================
    // MOVIES
    // =========================================================

    data object MovieDetail : Screen("movie_detail/{movieId}") {

        fun createRoute(id: Long) =
            "movie_detail/$id"
    }


    // =========================================================
    // TMDB DETAIL
    // =========================================================

    data object TmdbDetail :
        Screen("tmdb_detail/{mediaType}/{tmdbId}") {

        fun createRoute(
            mediaType: String,
            tmdbId: Int
        ) =
            "tmdb_detail/$mediaType/$tmdbId"
    }


    // =========================================================
    // ADD / EDIT
    // =========================================================

    data object AddEdit :
        Screen("add_edit?itemId={itemId}&type={type}") {

        fun createRoute(
            itemId: Long? = null,
            type: String? = null
        ): String {

            val id = itemId ?: -1L
            val t = type ?: ""

            return "add_edit?itemId=$id&type=$t"
        }
    }
}