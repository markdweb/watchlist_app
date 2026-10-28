package com.cheese.watchlist.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalDensity
import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.data.tmdb.TmdbApi
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val DetailOrange = Color(0xFFFF8A3D)
private val DetailPink = Color(0xFFEC4899)
private val DetailPurple = Color(0xFF8B5CF6)
private val DetailDark = Color(0xFF0E0B18)
private val DetailPanel = Color(0xFF19131F)
private val DetailPanelLight = Color(0xFFF8F5FA)

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/"

@Composable
fun MovieDetailScreen(
    movieId: Long,
    catalog: CatalogRepository,
    tmdbApi: TmdbApi,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: () -> Unit
) {
    var movie by remember { mutableStateOf<WatchItem?>(null) }
    var tmdbDetails by remember { mutableStateOf<TmdbApi.Details?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var showDelete by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showWatchMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current


    LaunchedEffect(movieId) {
        loading = true
        error = null

        runCatching {
            val localMovie = catalog.getMovie(movieId)
                ?: error("Movie not found")

            movie = localMovie

            /*
             * If this movie already has a TMDB ID, use it.
             * Otherwise search TMDB using the local title so older/manual
             * watchlist entries can also receive TMDB artwork and metadata.
             */
            val tmdbItem = if (localMovie.tmdbId != null) {
                TmdbApi.Item(
                    id = localMovie.tmdbId,
                    mediaType = "movie",
                    title = localMovie.title,
                    overview = "",
                    posterPath = localMovie.posterPath,
                    backdropPath = localMovie.backdropPath,
                    year = localMovie.yearDate,
                    rating = localMovie.rating.toDouble(),
                    genres = emptyList(),
                    tmdbUrl = localMovie.url
                )
            } else {
                val results = tmdbApi.search(localMovie.title)

                results.firstOrNull {
                    it.mediaType == "movie" &&
                            it.title.equals(localMovie.title, ignoreCase = true)
                } ?: results.firstOrNull {
                    it.mediaType == "movie"
                }
            }

            if (tmdbItem != null) {
                val details = tmdbApi.details(tmdbItem)
                tmdbDetails = details

                /*
                 * Save missing TMDB metadata back into the local movie.
                 * User-entered values are preserved when they already exist.
                 */
                val remote = details.item

                val updatedMovie = localMovie.copy(
                    tmdbId = localMovie.tmdbId ?: remote.id,
                    posterPath = localMovie.posterPath ?: remote.posterPath,
                    backdropPath = localMovie.backdropPath ?: remote.backdropPath,
                    yearDate = localMovie.yearDate.ifBlank { remote.year },
                    rating = if (localMovie.rating > 0) {
                        localMovie.rating
                    } else {
                        remote.rating.roundToInt()
                    },
                    url = localMovie.url.ifBlank { remote.tmdbUrl }
                )

                if (updatedMovie != localMovie) {
                    catalog.saveMovie(
                        updatedMovie,
                        genreNames = remote.genres.toSet()
                    )
                    movie = updatedMovie
                }
            }
        }.onFailure {
            error = it.message ?: "Unable to load movie"
        }

        loading = false
    }

    val currentMovie = movie

    if (currentMovie == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DetailDark),
            contentAlignment = Alignment.Center
        ) {
            if (loading) {
                CircularProgressIndicator(color = DetailOrange)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = error ?: "Unable to load movie",
                        color = Color.White
                    )

                    TextButton(onClick = onBack) {
                        Text("Go Back", color = DetailOrange)
                    }
                }
            }
        }

        return
    }

    MovieDetailContent(
        movie = currentMovie,
        tmdbDetails = tmdbDetails,
        context = context,
        showMenu = showMenu,
        showWatchMenu = showWatchMenu,
        onShowMenu = { showMenu = true },
        onDismissMenu = { showMenu = false },
        onShowWatchMenu = { showWatchMenu = true },
        onDismissWatchMenu = { showWatchMenu = false },
        onBack = onBack,
        onEdit = { onEdit(currentMovie.id) },
        onDelete = { showDelete = true }
    )

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            shape = RoundedCornerShape(28.dp),
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text("Delete movie?")
            },
            text = {
                Text(
                    "\"${currentMovie.title}\" will be removed from your watchlist."
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showDelete = false }
                ) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        )
    }
}

@Composable
private fun MovieDetailContent(
    movie: WatchItem,
    tmdbDetails: TmdbApi.Details?,
    context: android.content.Context,
    showMenu: Boolean,
    showWatchMenu: Boolean,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onShowWatchMenu: () -> Unit,
    onDismissWatchMenu: () -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailDark),
        propagateMinConstraints = true
    ) {
        val maxHeightPx = constraints.maxHeight.toFloat()

        /*
         * Bottom card occupies about 70% of the screen.
         * It starts lower and can be dragged upward.
         */
        val sheetHeightPx = maxHeightPx * 0.70f
        val maxOffsetPx = (maxHeightPx - sheetHeightPx).coerceAtLeast(0f)

        val sheetOffset = remember(maxOffsetPx) {
            Animatable(maxOffsetPx)
        }

        val scope = rememberCoroutineScope()

        val progress = if (maxOffsetPx <= 0f) {
            1f
        } else {
            1f - (sheetOffset.value / maxOffsetPx)
        }

        /*
         * Dynamic blur:
         * card down  -> almost no blur
         * card up    -> stronger blur
         */
        val blurAmount = (progress * 18f).coerceIn(0f, 18f).dp

        val isDark = MaterialTheme.colorScheme.background == DetailDark

        val backdropPath =
            tmdbDetails?.item?.backdropPath
                ?: movie.backdropPath
                ?: tmdbDetails?.item?.posterPath
                ?: movie.posterPath

        val backdropUrl = backdropPath?.let {
            "$TMDB_IMAGE_BASE/original$it"
        }

        /*
         * BACKDROP
         */
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurAmount),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                DetailPurple.copy(alpha = 0.45f),
                                DetailPink.copy(alpha = 0.18f),
                                DetailDark
                            )
                        )
                    )
            )
        }

        /*
         * BACKDROP GRADIENT
         */
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.12f),
                        0.38f to Color.Black.copy(alpha = 0.35f),
                        0.65f to DetailDark.copy(alpha = 0.78f),
                        1f to DetailDark
                    )
                )
        )

        /*
         * TOP BACK BUTTON
         */
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 10.dp)
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.Black.copy(alpha = 0.48f))
        ) {
            Icon(
                Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        /*
         * TOP RIGHT MENU
         */
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 14.dp, top = 10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                movie.url.ifBlank {
                                    tmdbDetails?.item?.tmdbUrl.orEmpty()
                                }
                            )
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                movie.title
                            )
                        }

                        runCatching {
                            context.startActivity(
                                Intent.createChooser(
                                    sendIntent,
                                    "Share movie"
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.48f))
                ) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }

                Box {
                    IconButton(
                        onClick = onShowMenu,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.48f))
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "More",
                            tint = Color.White
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = onDismissMenu
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit movie") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onDismissMenu()
                                onEdit()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete movie") },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                onDismissMenu()
                                onDelete()
                            }
                        )
                    }
                }
            }
        }

        /*
         * BOTTOM SHEET
         */
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(
                    with(LocalDensity.current) {
                        sheetHeightPx.toDp()
                    }
                )
                .align(Alignment.BottomCenter)
                .offset {
                    IntOffset(
                        x = 0,
                        y = sheetOffset.value.roundToInt()
                    )
                },
            shape = RoundedCornerShape(
                topStart = 34.dp,
                topEnd = 34.dp
            ),
            color = if (isDark) {
                DetailPanel.copy(alpha = 0.96f)
            } else {
                DetailPanelLight.copy(alpha = 0.97f)
            },
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                /*
                 * DRAG HANDLE
                 */
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .pointerInput(maxOffsetPx) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()

                                    val newOffset =
                                        (sheetOffset.value + dragAmount)
                                            .coerceIn(0f, maxOffsetPx)

                                    scope.launch {
                                        sheetOffset.snapTo(newOffset)
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        val target =
                                            if (sheetOffset.value < maxOffsetPx / 2f) {
                                                0f
                                            } else {
                                                maxOffsetPx
                                            }

                                        sheetOffset.animateTo(
                                            targetValue = target,
                                            animationSpec = tween(280)
                                        )
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(82.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (isDark) {
                                    Color.White.copy(alpha = 0.20f)
                                } else {
                                    Color.Black.copy(alpha = 0.16f)
                                }
                            )
                    )
                }

                /*
                 * SCROLLABLE CONTENT
                 */
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 32.dp
                        )
                ) {

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = tmdbDetails?.item?.title ?: movie.title,
                        color = if (isDark) Color.White else Color(0xFF1C1620),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(Modifier.height(12.dp))

                    /*
                     * METADATA
                     */
                    val year =
                        tmdbDetails?.item?.year
                            ?.takeIf { it.isNotBlank() }
                            ?: movie.yearDate

                    val rating =
                        tmdbDetails?.item?.rating
                            ?: movie.rating.toDouble()

                    val genreText =
                        tmdbDetails?.item?.genres
                            ?.take(3)
                            ?.joinToString(", ")
                            ?.takeIf { it.isNotBlank() }
                            ?: movie.genre

                    val ageRating =
                        tmdbDetails?.contentRating
                            ?.takeIf { it.isNotBlank() }
                            ?: "NR"

                    val runtimeText =
                        tmdbDetails?.runtimeMinutes
                            ?.takeIf { it > 0 }
                            ?.let(::formatRuntime)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (year.isNotBlank()) {
                            MetaText(year, isDark)
                        }

                        if (rating > 0) {
                            MetaText(
                                "★ ${String.format("%.1f", rating)}",
                                isDark,
                                accent = DetailOrange
                            )
                        }

                        if (ageRating != "NR") {
                            MetaText(ageRating, isDark)
                        }

                        if (runtimeText != null) {
                            MetaText(runtimeText, isDark)
                        }
                    }

                    if (genreText.isNotBlank()) {
                        Spacer(Modifier.height(9.dp))

                        Text(
                            text = genreText,
                            color = if (isDark) {
                                Color(0xFFBDB3C4)
                            } else {
                                Color(0xFF766C7D)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    /*
                     * TRAILER + WATCH
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailActionButton(
                            icon = Icons.Outlined.PlayArrow,
                            text = "Trailer",
                            filled = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val trailerUrl = tmdbDetails?.trailerUrl

                                if (!trailerUrl.isNullOrBlank()) {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(trailerUrl)
                                            )
                                        )
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Couldn't open trailer",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Trailer not available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )

                        Box(
                            modifier = Modifier.weight(1f)
                        ) {
                            DetailActionButton(
                                icon = Icons.Outlined.Public,
                                text = "Watch",
                                trailingIcon = Icons.Outlined.KeyboardArrowDown,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onShowWatchMenu
                            )

                            DropdownMenu(
                                expanded = showWatchMenu,
                                onDismissRequest = onDismissWatchMenu
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text("Open on TMDB")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.OpenInNew,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        onDismissWatchMenu()

                                        val url =
                                            movie.url.ifBlank {
                                                tmdbDetails?.item?.tmdbUrl.orEmpty()
                                            }

                                        if (url.isNotBlank()) {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(url)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("Watch options")
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        onDismissWatchMenu()

                                        Toast.makeText(
                                            context,
                                            "Provider links depend on available TMDB data.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    /*
                     * WANT TO WATCH + ALREADY SEEN
                     */
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusActionButton(
                            icon = Icons.Outlined.BookmarkBorder,
                            text = "Want to watch",
                            active = movie.status == WatchStatus.TO_WATCH,
                            modifier = Modifier.weight(1f)
                        )

                        StatusActionButton(
                            icon = Icons.Outlined.Check,
                            text = "Already seen",
                            active = movie.status == WatchStatus.WATCHED,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    /*
                     * ABOUT TAB ONLY
                     */
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                DetailOrange.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "About",
                            color = DetailOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Overview",
                        color = if (isDark) Color.White else Color(0xFF1C1620),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = tmdbDetails?.item?.overview
                            ?.takeIf { it.isNotBlank() }
                            ?: "No overview available for this title.",
                        color = if (isDark) {
                            Color(0xFFD5CDD7)
                        } else {
                            Color(0xFF625867)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f
                    )

                    /*
  * STILLS
  */
                    if (!tmdbDetails?.stills.isNullOrEmpty()) {
                        Spacer(Modifier.height(26.dp))

                        Text(
                            text = "Stills",
                            color = if (isDark) Color.White else Color(0xFF1C1620),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(
                                    rememberScrollState()
                                ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            tmdbDetails?.stills?.forEach { still ->
                                AsyncImage(
                                    model = still,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .width(250.dp)
                                        .height(145.dp)
                                        .clip(
                                            RoundedCornerShape(22.dp)
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    if (movie.comments.isNotBlank()) {
                        Spacer(Modifier.height(26.dp))

                        Text(
                            text = "Your Notes",
                            color = if (isDark) Color.White else Color(0xFF1C1620),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) {
                                Color.White.copy(alpha = 0.055f)
                            } else {
                                Color.Black.copy(alpha = 0.035f)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isDark) {
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    Color.Black.copy(alpha = 0.07f)
                                }
                            )
                        ) {
                            Text(
                                text = movie.comments,
                                modifier = Modifier.padding(16.dp),
                                color = if (isDark) {
                                    Color(0xFFD5CDD7)
                                } else {
                                    Color(0xFF625867)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                        color = if (isDark) {
                            Color(0xFF8F858E)
                        } else {
                            Color(0xFF8A808E)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaText(
    text: String,
    isDark: Boolean,
    accent: Color? = null
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) {
            Color.White.copy(alpha = 0.075f)
        } else {
            Color.Black.copy(alpha = 0.045f)
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
            color = accent ?: if (isDark) {
                Color(0xFFC8BFCC)
            } else {
                Color(0xFF716777)
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier,
    onClick: () -> Unit,
    filled: Boolean = false,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (filled) {
            DetailOrange.copy(alpha = 0.18f)
        } else {
            Color.White.copy(alpha = 0.055f)
        },
        border = BorderStroke(
            1.dp,
            if (filled) {
                DetailOrange.copy(alpha = 0.30f)
            } else {
                Color.White.copy(alpha = 0.10f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (filled) DetailOrange else Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            if (trailingIcon != null) {
                Spacer(Modifier.width(4.dp))

                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    active: Boolean,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (active) {
            DetailOrange.copy(alpha = 0.12f)
        } else {
            Color.White.copy(alpha = 0.045f)
        },
        border = BorderStroke(
            1.dp,
            if (active) {
                DetailOrange.copy(alpha = 0.28f)
            } else {
                Color.White.copy(alpha = 0.08f)
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) DetailOrange else Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(21.dp)
            )

            Spacer(Modifier.width(7.dp))

            Text(
                text = text,
                color = if (active) {
                    DetailOrange
                } else {
                    Color.White.copy(alpha = 0.88f)
                },
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return when {
        hours > 0 && remainingMinutes > 0 ->
            "${hours}h ${remainingMinutes}m"

        hours > 0 ->
            "${hours}h"

        else ->
            "${remainingMinutes}m"
    }
}