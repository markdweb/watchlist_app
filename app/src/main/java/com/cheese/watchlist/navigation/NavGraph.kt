package com.cheese.watchlist.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cheese.watchlist.data.entities.WatchType
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.data.tmdb.TmdbApi
import com.cheese.watchlist.ui.components.WatchlistBottomNavBar
import com.cheese.watchlist.ui.screens.*
import com.cheese.watchlist.viewmodel.LibraryViewModel
import com.cheese.watchlist.viewmodel.WatchlistViewModel
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

@Composable
fun WatchlistNavGraph(
    viewModel: WatchlistViewModel,
    libraryViewModel: LibraryViewModel,
    catalog: CatalogRepository,
    darkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route

    val showBottom =
        route == Screen.Home.route ||
                route == Screen.Lists.route ||
                route == Screen.Profile.route

    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            if (showBottom) {
                WatchlistBottomNavBar(
                    currentRoute = route,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { padding ->

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),

            // Main pages behave like a horizontal carousel.
            // The whole page slides — not just the bottom navigation.
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { fullWidth ->
                        when (initialState.destination.route) {
                            Screen.Home.route -> {
                                when (targetState.destination.route) {
                                    Screen.Lists.route -> fullWidth
                                    Screen.Profile.route -> fullWidth
                                    else -> fullWidth
                                }
                            }
                            Screen.Lists.route -> {
                                when (targetState.destination.route) {
                                    Screen.Profile.route -> fullWidth
                                    else -> -fullWidth
                                }
                            }
                            Screen.Profile.route -> -fullWidth
                            else -> fullWidth
                        }
                    }
                ) + fadeIn(animationSpec = tween(120))
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(220),
                    targetOffsetX = { fullWidth ->
                        when (initialState.destination.route) {
                            Screen.Home.route -> -fullWidth
                            Screen.Lists.route -> {
                                if (targetState.destination.route == Screen.Home.route) {
                                    fullWidth
                                } else {
                                    -fullWidth
                                }
                            }
                            Screen.Profile.route -> fullWidth
                            else -> -fullWidth
                        }
                    }
                ) + fadeOut(animationSpec = tween(120))
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = tween(220),
                    initialOffsetX = { fullWidth ->
                        when (targetState.destination.route) {
                            Screen.Home.route -> -fullWidth
                            Screen.Lists.route -> -fullWidth
                            Screen.Profile.route -> fullWidth
                            else -> -fullWidth
                        }
                    }
                ) + fadeIn(animationSpec = tween(120))
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(220),
                    targetOffsetX = { fullWidth ->
                        when (initialState.destination.route) {
                            Screen.Profile.route -> fullWidth
                            Screen.Lists.route -> fullWidth
                            Screen.Home.route -> -fullWidth
                            else -> fullWidth
                        }
                    }
                ) + fadeOut(animationSpec = tween(120))
            }
        ) {

            // =============================================================
            // HOME
            // =============================================================

            composable(Screen.Home.route) {
                DiscoveryScreen(
                    catalog = catalog,

                    onOpenWatchlist = {
                        navController.navigate(Screen.Lists.route)
                    },

                    onOpenTmdbItem = { id, mediaType ->
                        navController.navigate(
                            Screen.TmdbDetail.createRoute(
                                mediaType,
                                id
                            )
                        )
                    },

                    onOpenTrending = {
                        navController.navigate("trending_full")
                    },

                    onOpenNewReleases = {
                        navController.navigate("new_releases_full")
                    },

                    onOpenUpcoming = {
                        navController.navigate("upcoming_full")
                    }
                )
            }

            // =============================================================
            // TRENDING FULL
            // =============================================================

            composable("trending_full") {
                TrendingScreen(
                    tmdbApi = TmdbApi(),
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenTmdbItem = { id, mediaType ->
                        navController.navigate(
                            Screen.TmdbDetail.createRoute(
                                mediaType,
                                id
                            )
                        )
                    }
                )
            }

            // =============================================================
            // NEW RELEASES FULL
            // =============================================================

            composable("new_releases_full") {
                NewReleasesScreen(
                    tmdbApi = TmdbApi(),
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenTmdbItem = { id, mediaType ->
                        navController.navigate(
                            Screen.TmdbDetail.createRoute(
                                mediaType,
                                id
                            )
                        )
                    }
                )
            }

            // =============================================================
            // UPCOMING FULL
            // =============================================================

            composable("upcoming_full") {
                UpcomingScreen(
                    tmdbApi = TmdbApi(),
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenTmdbItem = { id, mediaType ->
                        navController.navigate(
                            Screen.TmdbDetail.createRoute(
                                mediaType,
                                id
                            )
                        )
                    }
                )
            }

            // =============================================================
            // LISTS
            // =============================================================

            composable(Screen.Lists.route) {
                LibraryScreen(
                    viewModel = libraryViewModel,

                    onAddMovie = {
                        navController.navigate(
                            Screen.AddEdit.createRoute(
                                type = WatchType.MOVIE.name
                            )
                        )
                    },

                    onAddSeries = {
                        navController.navigate(
                            Screen.SeriesEditor.createRoute()
                        )
                    },

                    onAddCollection = {
                        navController.navigate(
                            Screen.CollectionEditor.createRoute()
                        )
                    },

                    onMovie = { id ->
                        val movie = libraryViewModel.uiState.value.movies
                            .firstOrNull { it.id == id }

                        val tmdbId = movie?.tmdbId

                        if (tmdbId != null && tmdbId > 0) {
                            navController.navigate(
                                Screen.TmdbDetail.createRoute(
                                    mediaType = "movie",
                                    tmdbId = tmdbId
                                )
                            )
                        } else {
                            // Fallback for older/local movies without a TMDB ID.
                            navController.navigate(
                                Screen.MovieDetail.createRoute(id)
                            )
                        }
                    },

                    onSeries = { id ->
                        val series = libraryViewModel.uiState.value.series
                            .firstOrNull { it.id == id }

                        val tmdbId = series?.url
                            ?.substringAfter("/tv/", "")
                            ?.substringBefore("/")
                            ?.substringBefore("?")
                            ?.toIntOrNull()

                        if (tmdbId != null && tmdbId > 0) {
                            navController.navigate(
                                Screen.TmdbDetail.createRoute(
                                    mediaType = "tv",
                                    tmdbId = tmdbId
                                )
                            )
                        } else {
                            // Fallback for older/local series without a TMDB URL.
                            navController.navigate(
                                Screen.SeriesDetail.createRoute(id)
                            )
                        }
                    },

                    onCollection = { id ->
                        navController.navigate(
                            Screen.CollectionDetail.createRoute(id)
                        )
                    },

                    onSettings = {
                        navController.navigate(Screen.Settings.route)
                    },

                    onEditSeries = { id ->
                        navController.navigate(
                            Screen.SeriesEditor.createRoute(id)
                        )
                    },

                    onEditCollection = { id ->
                        navController.navigate(
                            Screen.CollectionEditor.createRoute(id)
                        )
                    }
                )
            }

            // =============================================================
            // PROFILE
            // =============================================================

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = libraryViewModel,
                    onSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            // =============================================================
            // SETTINGS
            // =============================================================

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    darkTheme = darkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,

                    onManageGenres = {
                        navController.navigate(Screen.Genres.route)
                    },

                    onManageSources = {
                        navController.navigate(Screen.ManageSources.route)
                    },

                    onPcManager = {
                        navController.navigate(Screen.PcManager.route)
                    }
                )
            }

            // =============================================================
            // GENRES
            // =============================================================

            composable(Screen.Genres.route) {
                GenresScreen(
                    viewModel = viewModel
                )
            }

            // =============================================================
            // MANAGE SOURCES
            // =============================================================

            composable(Screen.ManageSources.route) {
                ManageSourcesScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // =============================================================
            // PC MANAGER
            // =============================================================

            composable(Screen.PcManager.route) {
                PcManagerScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // =============================================================
            // SERIES EDITOR
            // =============================================================

            composable(
                Screen.SeriesEditor.route,
                arguments = listOf(
                    navArgument("seriesId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { e ->

                val id =
                    e.arguments?.getLong("seriesId")
                        ?: -1L

                SeriesEditorScreen(
                    catalog,
                    id.takeIf { it > 0 },
                    onDone = {
                        navController.popBackStack()
                    }
                )
            }

            // =============================================================
            // COLLECTION EDITOR
            // =============================================================

            composable(
                Screen.CollectionEditor.route,
                arguments = listOf(
                    navArgument("collectionId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { e ->

                val id =
                    e.arguments?.getLong("collectionId")
                        ?: -1L

                CollectionEditorScreen(
                    catalog,
                    id.takeIf { it > 0 },
                    onDone = {
                        navController.popBackStack()
                    }
                )
            }

            // =============================================================
            // TMDB DETAIL
            // =============================================================

            composable(
                Screen.TmdbDetail.route,
                arguments = listOf(
                    navArgument("mediaType") {
                        type = NavType.StringType
                    },
                    navArgument("tmdbId") {
                        type = NavType.IntType
                    }
                )
            ) { e ->

                TmdbDetailScreen(
                    tmdbId = e.arguments!!.getInt("tmdbId"),
                    mediaType =
                        e.arguments?.getString("mediaType")
                            ?: "movie",
                    catalog = catalog,
                    onBack = {
                        navController.popBackStack()
                    },
                    onOpenTmdbItem = { id, type ->
                        navController.navigate(
                            Screen.TmdbDetail.createRoute(
                                type,
                                id
                            )
                        )
                    }
                )
            }

            // =============================================================
            // MOVIE DETAIL
            // =============================================================

            composable(
                Screen.MovieDetail.route,
                arguments = listOf(
                    navArgument("movieId") {
                        type = NavType.LongType
                    }
                )
            ) { e ->

                MovieDetailScreen(
                    movieId = e.arguments!!.getLong("movieId"),
                    catalog = catalog,
                    tmdbApi = TmdbApi(),

                    onBack = {
                        navController.popBackStack()
                    },

                    onEdit = { id ->
                        navController.navigate(
                            Screen.AddEdit.createRoute(
                                itemId = id
                            )
                        )
                    },

                    onDelete = {
                        scope.launch {
                            catalog.deleteMovie(
                                e.arguments!!.getLong("movieId")
                            )
                            navController.popBackStack()
                        }
                    }
                )
            }

            // =============================================================
            // SERIES DETAIL
            // =============================================================

            composable(
                Screen.SeriesDetail.route,
                arguments = listOf(
                    navArgument("seriesId") {
                        type = NavType.LongType
                    }
                )
            ) { e ->

                SeriesDetailScreen(
                    e.arguments!!.getLong("seriesId"),
                    libraryViewModel,

                    onBack = {
                        navController.popBackStack()
                    },

                    onEdit = { id ->
                        navController.navigate(
                            Screen.SeriesEditor.createRoute(id)
                        )
                    },

                    onDelete = {
                        navController.popBackStack()
                    }
                )
            }

            // =============================================================
            // COLLECTIONS
            // =============================================================

            composable(Screen.Collections.route) {
                CollectionsScreen(
                    catalog,
                    onBack = {
                        navController.popBackStack()
                    },
                    onMovie = { id ->
                        val movie = libraryViewModel.uiState.value.movies
                            .firstOrNull { it.id == id }

                        val tmdbId = movie?.tmdbId

                        if (tmdbId != null && tmdbId > 0) {
                            navController.navigate(
                                Screen.TmdbDetail.createRoute(
                                    mediaType = "movie",
                                    tmdbId = tmdbId
                                )
                            )
                        } else {
                            navController.navigate(
                                Screen.MovieDetail.createRoute(id)
                            )
                        }
                    },
                    onCollection = { id ->
                        navController.navigate(
                            Screen.CollectionDetail.createRoute(id)
                        )
                    }
                )
            }

            // =============================================================
            // COLLECTION DETAIL
            // =============================================================

            composable(
                Screen.CollectionDetail.route,
                arguments = listOf(
                    navArgument("collectionId") {
                        type = NavType.LongType
                    }
                )
            ) { e ->

                CollectionsScreen(
                    catalog = catalog,
                    collectionId =
                        e.arguments!!.getLong("collectionId"),

                    onBack = {
                        navController.popBackStack()
                    },

                    onMovie = { id ->
                        val movie = libraryViewModel.uiState.value.movies
                            .firstOrNull { it.id == id }

                        val tmdbId = movie?.tmdbId

                        if (tmdbId != null && tmdbId > 0) {
                            navController.navigate(
                                Screen.TmdbDetail.createRoute(
                                    mediaType = "movie",
                                    tmdbId = tmdbId
                                )
                            )
                        } else {
                            navController.navigate(
                                Screen.MovieDetail.createRoute(id)
                            )
                        }
                    },

                    onEdit = { id ->
                        navController.navigate(
                            Screen.CollectionEditor.createRoute(id)
                        )
                    },

                    onDelete = {
                        navController.popBackStack()
                    }
                )
            }

            // =============================================================
            // ADD / EDIT
            // =============================================================

            composable(
                Screen.AddEdit.route,
                arguments = listOf(
                    navArgument("itemId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { e ->

                val id =
                    e.arguments?.getLong("itemId")
                        ?: -1L

                val type =
                    e.arguments?.getString("type")
                        ?: ""

                AddEditScreen(
                    viewModel = viewModel,
                    itemId = id.takeIf { it > 0 },
                    initialType =
                        if (type == WatchType.SERIES.name) {
                            WatchType.SERIES
                        } else {
                            WatchType.MOVIE
                        },
                    onDone = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}