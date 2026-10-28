package com.cheese.watchlist

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cheese.watchlist.data.database.AppDatabase
import com.cheese.watchlist.data.preferences.ThemePreferences
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.data.repository.WatchlistRepository
import com.cheese.watchlist.navigation.WatchlistNavGraph
import com.cheese.watchlist.ui.screens.SplashScreen
import com.cheese.watchlist.ui.theme.WatchlistTheme
import com.cheese.watchlist.viewmodel.LibraryViewModel
import com.cheese.watchlist.viewmodel.WatchlistViewModel
import com.cheese.watchlist.viewmodel.WatchlistViewModelFactory

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge is intentionally NOT enabled.
        // The system will reserve status/navigation bar space automatically.

        val database = AppDatabase.getInstance(applicationContext)

        val repository = WatchlistRepository(
            watchItemDao = database.watchItemDao(),
            genreDao = database.genreDao(),
            sourceDao = database.sourceDao()
        )

        setContent {
            WatchlistApp(repository, database)
        }
    }
}

private sealed class LaunchStep {
    data object Splash : LaunchStep()
    data object Main : LaunchStep()
}

@Composable
fun WatchlistApp(
    repository: WatchlistRepository,
    database: AppDatabase
) {
    val context = LocalContext.current

    var darkTheme by remember {
        mutableStateOf(ThemePreferences.isDarkMode(context))
    }

    var step by remember {
        mutableStateOf<LaunchStep>(LaunchStep.Splash)
    }

    WatchlistTheme(darkTheme = darkTheme) {

        val backgroundColor = MaterialTheme.colorScheme.background

        SideEffect {
            val window = (context as? ComponentActivity)?.window ?: return@SideEffect

            window.statusBarColor = backgroundColor.toArgb()
            window.navigationBarColor = backgroundColor.toArgb()

            window.decorView.systemUiVisibility =
                if (darkTheme) {
                    0
                } else {
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                            View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = backgroundColor
        ) {

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "launch_flow"
            ) { currentStep ->

                when (currentStep) {

                    is LaunchStep.Splash -> {

                        SplashScreen(
                            onFinished = {
                                // Splash → Home directly.
                                // No onboarding.
                                // No name input.
                                // No first greeting.
                                step = LaunchStep.Main
                            }
                        )
                    }

                    is LaunchStep.Main -> {

                        val viewModel: WatchlistViewModel = viewModel(
                            factory = WatchlistViewModelFactory(repository)
                        )

                        val catalog = remember(database) {
                            CatalogRepository(
                                db = database,
                                movies = database.watchItemDao(),
                                series = database.seriesDao(),
                                seasons = database.seasonDao(),
                                episodes = database.episodeDao(),
                                collections = database.collectionDao(),
                                collectionMovies = database.collectionMovieCrossRefDao(),
                                genres = database.genreDao(),
                                movieGenres = database.movieGenreCrossRefDao(),
                                seriesGenres = database.seriesGenreCrossRefDao(),
                                tags = database.tagDao(),
                                itemTags = database.itemTagCrossRefDao(),
                                sources = database.sourceDao()
                            )
                        }

                        val libraryViewModel: LibraryViewModel = viewModel(
                            factory = LibraryViewModel.Factory(catalog)
                        )

                        WatchlistNavGraph(
                            viewModel = viewModel,
                            libraryViewModel = libraryViewModel,
                            catalog = catalog,
                            darkTheme = darkTheme,
                            onToggleDarkTheme = { enabled ->
                                darkTheme = enabled
                                ThemePreferences.setDarkMode(
                                    context,
                                    enabled
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}