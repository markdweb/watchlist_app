package com.cheese.watchlist.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlin.math.abs
import com.cheese.watchlist.data.entities.WatchStatus
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.data.tmdb.TmdbApi
import com.cheese.watchlist.ui.theme.BackgroundDark
import com.cheese.watchlist.viewmodel.TmdbDetailViewModel

private val AccentOrange = Color(0xFFFF8A3D)
private val AccentPink = Color(0xFFEC4899)
private val AccentPurple = Color(0xFF8B5CF6)

@Composable
fun TmdbDetailScreen(
    tmdbId: Int,
    mediaType: String,
    catalog: CatalogRepository,
    onBack: () -> Unit,
    onOpenTmdbItem: (Int, String) -> Unit
) {
    val vm: TmdbDetailViewModel =
        viewModel(factory = TmdbDetailViewModel.Factory(TmdbApi()))

    val state by vm.state.collectAsState()
    val context = LocalContext.current

    val isDark = MaterialTheme.colorScheme.background == BackgroundDark

    var adding by remember { mutableStateOf(false) }
    var showWatchSheet by remember { mutableStateOf(false) }
    var editingWatchUrl by remember { mutableStateOf(false) }
    var watchUrlInput by remember { mutableStateOf("") }
    var showMoveBackDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val localMovies by catalog.allMovies.collectAsState(initial = emptyList())
    val localSeries by catalog.allSeries.collectAsState(initial = emptyList())

    val density = LocalDensity.current
    val minSheetOffsetPx = with(density) { (-300).dp.toPx() }
    val maxSheetOffsetPx = 0f
    var sheetOffsetPx by remember { mutableFloatStateOf(0f) }
    val sheetProgress =
        (-sheetOffsetPx / abs(minSheetOffsetPx)).coerceIn(0f, 1f)
    val backdropBlur = with(density) { (18f * sheetProgress).dp }

    val pageBackground = if (isDark) {
        Color(0xFF0C0910)
    } else {
        Color(0xFFF7F3FA)
    }

    val panelBackground = if (isDark) {
        Color(0xFF171218).copy(alpha = 0.96f)
    } else {
        Color.White.copy(alpha = 0.94f)
    }

    val secondaryPanel = if (isDark) {
        Color(0xFF211A25).copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }

    val primaryText = if (isDark) {
        Color(0xFFF7F2F8)
    } else {
        Color(0xFF19131E)
    }

    val secondaryText = if (isDark) {
        Color(0xFFB9AFBD)
    } else {
        Color(0xFF716778)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    var runtimeMinutes by remember { mutableIntStateOf(0) }

    LaunchedEffect(tmdbId, mediaType) {
        vm.load(tmdbId, mediaType)
        runtimeMinutes = runCatching {
            TmdbApi().details(tmdbId, mediaType).runtimeMinutes
        }.getOrDefault(0)
    }

    val currentItem = state.item

    val savedWatchUrl = if (mediaType == "tv") {
        localSeries
            .firstOrNull { it.url == currentItem?.tmdbUrl }
            ?.watchUrl
            .orEmpty()
    } else {
        localMovies
            .firstOrNull { it.tmdbId == tmdbId }
            ?.watchUrl
            .orEmpty()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
    ) {

        val item = currentItem
        val localMovie = localMovies.firstOrNull { it.tmdbId == tmdbId }
        val localSeriesEntry = localSeries.firstOrNull { it.url == item?.tmdbUrl }

        val localStatus = if (mediaType == "tv") {
            localSeriesEntry?.status
        } else {
            localMovie?.status
        }

        val isPlanned = localStatus == WatchStatus.TO_WATCH
        val isWatching = localStatus == WatchStatus.WATCHING
        val isWatched = localStatus == WatchStatus.WATCHED

        val seriesId = localSeriesEntry?.id ?: 0L
        val seriesProgressFlow = remember(seriesId, mediaType) {
            if (mediaType == "tv" && seriesId != 0L) {
                catalog.observeSeriesProgress(seriesId)
            } else {
                flowOf(null)
            }
        }
        val seriesProgress by seriesProgressFlow.collectAsState(initial = null)

        if (item != null) {

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                /*
                 * FIXED BACKGROUND IMAGE
                 *
                 * The image stays behind the detail sheet.
                 * It starts sharp and blurs as the sheet moves upward.
                 */
                AsyncImage(
                    model = item.posterUrl ?: item.backdropUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(backdropBlur),
                    contentScale = ContentScale.Crop
                )

                /*
                 * BACKGROUND OVERLAY
                 *
                 * No white fade. The image remains visible behind
                 * the detail sheet.
                 */
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.45f)
                                )
                            )
                        )
                )

                /*
                 * FIXED TOP NAVIGATION
                 *
                 * No Share button.
                 */
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                        .statusBarsPadding()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 10.dp
                        ),
                    horizontalArrangement = Arrangement.Start
                ) {
                    GlassIconButton(
                        icon = Icons.Outlined.ArrowBack,
                        contentDescription = "Back",
                        isDark = isDark,
                        onClick = onBack
                    )
                }

                /*
                 * SCROLLABLE DETAIL CONTENT
                 *
                 * The spacer keeps the sheet at the same starting height
                 * while the background image itself stays fixed.
                 */
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    Spacer(Modifier.height(455.dp))

                    /*
                     * MAIN GLASS PANEL
                     */
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(
                                y = (-34).dp + with(density) { sheetOffsetPx.toDp() }
                            )
                            .clip(
                                RoundedCornerShape(
                                    topStart = 34.dp,
                                    topEnd = 34.dp
                                )
                            )
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        panelBackground,
                                        pageBackground
                                    )
                                )
                            )
                            .then(
                                Modifier
                            )
                            .padding(
                                horizontal = 20.dp,
                                vertical = 18.dp
                            )
                    ) {

                        /*
                         * MEDIA BADGE
                         *
                         * Kept inside the detail card so it moves with
                         * the card instead of floating above it.
                         */
                        Surface(
                            modifier = Modifier
                                .wrapContentWidth()
                                .align(Alignment.Start),
                            shape = RoundedCornerShape(14.dp),
                            color = AccentOrange.copy(alpha = 0.92f)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 7.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Movie,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    if (mediaType == "tv") "SERIES" else "MOVIE",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        /*
                         * Handle
                         */
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { _, dragAmount ->
                                            sheetOffsetPx =
                                                (sheetOffsetPx + dragAmount)
                                                    .coerceIn(minSheetOffsetPx, maxSheetOffsetPx)
                                        },
                                        onDragEnd = {
                                            sheetOffsetPx =
                                                if (sheetOffsetPx < minSheetOffsetPx / 2f) {
                                                    minSheetOffsetPx
                                                } else {
                                                    maxSheetOffsetPx
                                                }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isDark) {
                                            Color.White.copy(alpha = 0.14f)
                                        } else {
                                            Color.Black.copy(alpha = 0.10f)
                                        }
                                    )
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        /*
     * TITLE + RATING
     */
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = item.title,
                                color = primaryText,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(9.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier.size(19.dp)
                                )

                                Spacer(Modifier.width(5.dp))

                                Text(
                                    String.format("%.1f", item.rating),
                                    color = AccentOrange,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "  •  ${item.year.ifBlank { "—" }}",
                                    color = secondaryText,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (item.genres.isNotEmpty()) {
                                Spacer(Modifier.height(9.dp))

                                Text(
                                    item.genres
                                        .take(3)
                                        .joinToString(" • "),
                                    color = secondaryText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        /*
                         * META CHIPS
                         */
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            MetaChip(
                                icon = Icons.Outlined.CalendarMonth,
                                text = item.year.ifBlank { "—" },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )

                            MetaChip(
                                icon = Icons.Outlined.Star,
                                text = String.format("%.1f", item.rating),
                                isDark = isDark,
                                accent = AccentOrange,
                                modifier = Modifier.weight(1f)
                            )

                            MetaChip(
                                icon = Icons.Outlined.Movie,
                                text = if (mediaType == "tv") {
                                    "Series"
                                } else {
                                    "Movie"
                                },
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (runtimeMinutes > 0) {
                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                MetaChip(
                                    icon = Icons.Outlined.Schedule,
                                    text = formatRuntime(runtimeMinutes),
                                    isDark = isDark,
                                    accent = AccentPurple,
                                    modifier = Modifier.wrapContentWidth()
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        /*
                         * TRAILER + WATCH
                         *
                         * These buttons are shared by both Movies and Series.
                         */
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DetailActionButton(
                                icon = Icons.Outlined.PlayArrow,
                                label = "Trailer",
                                modifier = Modifier.weight(1f),
                                isDark = isDark,
                                filled = true,
                                onClick = {
                                    val trailer = state.trailerUrl

                                    if (!trailer.isNullOrBlank()) {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse(trailer)
                                                )
                                            )
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Trailer unavailable",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            )

                            DetailActionButton(
                                icon = Icons.Outlined.Public,
                                label = "Watch",
                                modifier = Modifier.weight(1f),
                                isDark = isDark,
                                onClick = {
                                    showWatchSheet = true
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        /*
                         * STATUS ACTIONS
                         */
                        if (mediaType == "tv") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SeriesStatusButton(
                                    label = "Want",
                                    icon = Icons.Outlined.BookmarkBorder,
                                    selected = isPlanned,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!adding) {
                                            adding = true

                                            if (isPlanned && localSeriesEntry != null) {
                                                // Want is already selected: tapping it again removes the Series.
                                                scope.launch {
                                                    runCatching {
                                                        catalog.deleteSeries(localSeriesEntry.id)
                                                    }
                                                    adding = false
                                                    Toast.makeText(
                                                        context,
                                                        "“${item.title}” was removed from the list.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            } else if (localSeriesEntry == null) {
                                                vm.addToLibrary(
                                                    catalog = catalog,
                                                    status = WatchStatus.TO_WATCH,
                                                    onDone = {
                                                        adding = false
                                                        Toast.makeText(
                                                            context,
                                                            "“${item.title}” was added to Want.",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    },
                                                    onDuplicate = { adding = false }
                                                )
                                            } else {
                                                // Move an existing Series back to Want.
                                                scope.launch {
                                                    runCatching {
                                                        catalog.setSeriesStatus(
                                                            localSeriesEntry.id,
                                                            WatchStatus.TO_WATCH,
                                                            clearRating = true
                                                        )
                                                    }
                                                    adding = false
                                                    Toast.makeText(
                                                        context,
                                                        "“${item.title}” was moved to Want.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                )

                                SeriesStatusButton(
                                    label = "Watching",
                                    icon = Icons.Outlined.PlayCircleOutline,
                                    selected = isWatching,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!adding) {
                                            adding = true
                                            if (localSeriesEntry == null) {
                                                vm.addToLibrary(
                                                    catalog = catalog,
                                                    status = WatchStatus.WATCHING,
                                                    onDone = {
                                                        adding = false
                                                        Toast.makeText(context, "“${item.title}” was added to Watching.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDuplicate = { adding = false }
                                                )
                                            } else {
                                                scope.launch {
                                                    runCatching { catalog.setSeriesStatus(localSeriesEntry.id, WatchStatus.WATCHING) }
                                                    adding = false
                                                    Toast.makeText(context, "“${item.title}” is now Watching.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )

                                SeriesStatusButton(
                                    label = "Seen",
                                    icon = Icons.Outlined.CheckCircle,
                                    selected = isWatched,
                                    isDark = isDark,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (!adding && !isWatched) {
                                            adding = true
                                            if (localSeriesEntry == null) {
                                                vm.addToLibrary(
                                                    catalog = catalog,
                                                    status = WatchStatus.WATCHED,
                                                    onDone = {
                                                        adding = false
                                                        Toast.makeText(context, "“${item.title}” was added as Already seen.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDuplicate = { adding = false }
                                                )
                                            } else {
                                                vm.markAsWatched(
                                                    catalog = catalog,
                                                    onDone = {
                                                        adding = false
                                                        Toast.makeText(context, "“${item.title}” is now marked as Already seen.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onNotFound = { adding = false }
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (isPlanned) {
                                    PlannedActionButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (!adding) {
                                                adding = true
                                                scope.launch {
                                                    runCatching { localMovie?.let { catalog.deleteMovie(it.id) } }
                                                    adding = false
                                                    Toast.makeText(context, "“${item.title}” was removed from the list.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    DetailActionButton(
                                        icon = Icons.Outlined.BookmarkBorder,
                                        label = "Want to watch",
                                        modifier = Modifier.weight(1f),
                                        isDark = isDark,
                                        onClick = {
                                            if (isWatched) {
                                                showMoveBackDialog = true
                                            } else if (!adding) {
                                                adding = true
                                                vm.addToLibrary(
                                                    catalog = catalog,
                                                    status = WatchStatus.TO_WATCH,
                                                    onDone = {
                                                        adding = false
                                                        Toast.makeText(context, "“${item.title}” was added to Want to watch.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDuplicate = { adding = false }
                                                )
                                            }
                                        }
                                    )
                                }

                                if (isWatched) {
                                    PlannedActionButton(
                                        modifier = Modifier.weight(1f),
                                        label = "Already seen",
                                        icon = Icons.Outlined.CheckCircle,
                                        onClick = {}
                                    )
                                } else {
                                    DetailActionButton(
                                        icon = Icons.Outlined.CheckCircle,
                                        label = "Already seen",
                                        modifier = Modifier.weight(1f),
                                        isDark = isDark,
                                        onClick = {
                                            if (!adding) {
                                                adding = true
                                                if (isPlanned) {
                                                    vm.markAsWatched(
                                                        catalog = catalog,
                                                        onDone = {
                                                            adding = false
                                                            Toast.makeText(context, "“${item.title}” is now marked as Already seen.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onNotFound = { adding = false }
                                                    )
                                                } else {
                                                    vm.addToLibrary(
                                                        catalog = catalog,
                                                        status = WatchStatus.WATCHED,
                                                        onDone = {
                                                            adding = false
                                                            Toast.makeText(context, "“${item.title}” was added to My List as Already seen.", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onDuplicate = { adding = false }
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        if (adding) {

                            Spacer(Modifier.height(12.dp))

                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = AccentOrange,
                                trackColor = if (isDark) {
                                    Color.White.copy(alpha = 0.08f)
                                } else {
                                    Color.Black.copy(alpha = 0.06f)
                                }
                            )
                        }

                        Spacer(Modifier.height(25.dp))

                        /*
                         * TABS
                         */
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            DetailTab(
                                text = "About",
                                selected = true,
                                modifier = Modifier.weight(1f),
                                isDark = isDark
                            )

                        }

                        if (mediaType == "tv" && seriesProgress != null) {
                            Spacer(Modifier.height(26.dp))

                            SeriesEpisodesSection(
                                progress = seriesProgress!!,
                                catalog = catalog,
                                isDark = isDark,
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                borderColor = borderColor
                            )
                        }

                        Spacer(Modifier.height(22.dp))

                        /*
                         * OVERVIEW
                         */
                        SectionTitle(
                            icon = Icons.Outlined.Info,
                            title = "Overview",
                            isDark = isDark
                        )

                        Spacer(Modifier.height(9.dp))

                        Text(
                            text = item.overview.ifBlank {
                                "No overview available for this title."
                            },
                            color = secondaryText,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.25f
                        )

                        /*
                         * STILLS
                         */
                        if (state.stills.isNotEmpty()) {

                            Spacer(Modifier.height(28.dp))

                            SectionTitle(
                                icon = Icons.Outlined.PhotoLibrary,
                                title = "Stills",
                                isDark = isDark
                            )

                            Spacer(Modifier.height(11.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {

                                items(state.stills) { still ->

                                    AsyncImage(
                                        model = still,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(255.dp)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(22.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        /*
                         * DETAILS
                         */
                        Spacer(Modifier.height(28.dp))

                        DetailsCard(
                            item = item,
                            state = state,
                            isDark = isDark,
                            panelColor = secondaryPanel,
                            borderColor = borderColor,
                            primaryText = primaryText,
                            secondaryText = secondaryText
                        )

                        Spacer(Modifier.height(24.dp))

                        Spacer(Modifier.height(18.dp))
                    }
                }
            }

        } else {

            /*
             * LOADING / ERROR
             */
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                if (state.loading) {

                    CircularProgressIndicator(
                        color = AccentOrange
                    )

                } else {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(30.dp)
                    ) {

                        Icon(
                            Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = AccentOrange,
                            modifier = Modifier.size(46.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Text(
                            state.error ?: "Unable to load title",
                            color = primaryText,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(18.dp))

                        Button(
                            onClick = {
                                vm.load(tmdbId, mediaType)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentOrange
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    /*
 * WHERE TO WATCH SHEET
 */
    if (showWatchSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                showWatchSheet = false
                editingWatchUrl = false
            },
            containerColor = if (isDark) {
                Color(0xFF171218)
            } else {
                Color.White
            },
            contentColor = if (isDark) {
                Color.White
            } else {
                Color(0xFF19131E)
            },
            dragHandle = {
                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isDark) {
                                Color.White.copy(alpha = 0.15f)
                            } else {
                                Color.Black.copy(alpha = 0.12f)
                            }
                        )
                )
            }
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 22.dp,
                        end = 22.dp,
                        bottom = 34.dp
                    )
            ) {

                Text(
                    text = "Where to watch",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "This title is available on these platforms:",
                    color = secondaryText
                )

                Spacer(Modifier.height(18.dp))

                if (!editingWatchUrl && savedWatchUrl.isNotBlank()) {

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(savedWatchUrl)
                                )

                                runCatching {
                                    context.startActivity(intent)
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "No app or browser can open this link.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                        shape = RoundedCornerShape(22.dp),
                        color = if (isDark) {
                            Color.White.copy(alpha = 0.05f)
                        } else {
                            Color.Black.copy(alpha = 0.035f)
                        },
                        border = BorderStroke(
                            1.dp,
                            borderColor
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = 18.dp,
                                    top = 15.dp,
                                    bottom = 15.dp,
                                    end = 10.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = AccentOrange.copy(alpha = 0.13f)
                            ) {
                                Icon(
                                    Icons.Outlined.Public,
                                    contentDescription = null,
                                    tint = AccentOrange,
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .size(23.dp)
                                )
                            }

                            Spacer(Modifier.width(13.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = remember(savedWatchUrl) {
                                        runCatching {
                                            Uri.parse(savedWatchUrl).host
                                                ?.removePrefix("www.")
                                                ?: savedWatchUrl
                                        }.getOrDefault(savedWatchUrl)
                                    },
                                    color = primaryText,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(Modifier.height(3.dp))

                                Text(
                                    text = "Tap to open",
                                    color = secondaryText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            IconButton(
                                onClick = {
                                    watchUrlInput = savedWatchUrl
                                    editingWatchUrl = true
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = "Edit website address",
                                    tint = if (isDark) {
                                        Color.White
                                    } else {
                                        Color(0xFF302637)
                                    }
                                )
                            }

                            Icon(
                                Icons.Outlined.ArrowForwardIos,
                                contentDescription = null,
                                tint = secondaryText,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                } else {

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        color = if (isDark) {
                            Color.White.copy(alpha = 0.05f)
                        } else {
                            Color.Black.copy(alpha = 0.035f)
                        },
                        border = BorderStroke(
                            1.dp,
                            borderColor
                        )
                    ) {

                        Column(
                            modifier = Modifier.padding(18.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Surface(
                                    shape = RoundedCornerShape(13.dp),
                                    color = AccentOrange.copy(alpha = 0.13f)
                                ) {
                                    Icon(
                                        Icons.Outlined.Link,
                                        contentDescription = null,
                                        tint = AccentOrange,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(23.dp)
                                    )
                                }

                                Spacer(Modifier.width(13.dp))

                                Text(
                                    text = "Enter a website address",
                                    color = primaryText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )

                                if (savedWatchUrl.isNotBlank()) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = null,
                                        tint = secondaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            OutlinedTextField(
                                value = watchUrlInput,
                                onValueChange = {
                                    watchUrlInput = it
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = {
                                    Text(
                                        "https://example.com/...",
                                        color = secondaryText
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentOrange,
                                    unfocusedBorderColor = borderColor,
                                    focusedTextColor = primaryText,
                                    unfocusedTextColor = primaryText,
                                    cursorColor = AccentOrange
                                )
                            )

                            Spacer(Modifier.height(14.dp))

                            Button(
                                onClick = {

                                    var finalUrl = watchUrlInput.trim()

                                    if (
                                        finalUrl.isNotBlank() &&
                                        !finalUrl.startsWith("http://") &&
                                        !finalUrl.startsWith("https://")
                                    ) {
                                        finalUrl = "https://$finalUrl"
                                    }

                                    if (finalUrl.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Please enter a website address.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@Button
                                    }

                                    vm.saveWatchUrl(
                                        catalog = catalog,
                                        url = finalUrl,
                                        onDone = {
                                            watchUrlInput = finalUrl
                                            editingWatchUrl = false

                                            Toast.makeText(
                                                context,
                                                "Website address saved.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onNotFound = {
                                            Toast.makeText(
                                                context,
                                                "This title is not in your list yet.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        onError = {
                                            Toast.makeText(
                                                context,
                                                it.message ?: "Unable to save website address.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentOrange
                                )
                            ) {
                                Text(
                                    text = if (savedWatchUrl.isBlank()) {
                                        "Save"
                                    } else {
                                        "Update"
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (!editingWatchUrl && savedWatchUrl.isBlank()) {

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "You can paste any website address.",
                        color = secondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
    if (showMoveBackDialog) {
        AlertDialog(
            onDismissRequest = {
                showMoveBackDialog = false
            },
            title = {
                Text(
                    text = "Move back to \"Want to watch\"",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "“${currentItem?.title.orEmpty()}” will be moved back to Want to watch and its rating will be cleared."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = currentItem?.title.orEmpty()
                        showMoveBackDialog = false

                        if (!adding) {
                            adding = true
                            vm.moveBackToWantToWatch(
                                catalog = catalog,
                                onDone = {
                                    adding = false
                                    Toast.makeText(
                                        context,
                                        "“$title” was returned to Want to watch.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onNotFound = {
                                    adding = false
                                }
                            )
                        }
                    }
                ) {
                    Text(
                        "Move back",
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showMoveBackDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

}

private fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val remainingMinutes = minutes % 60

    return when {
        hours > 0 && remainingMinutes > 0 -> "${hours} h ${remainingMinutes} min"
        hours > 0 -> "${hours} h"
        else -> "${remainingMinutes} min"
    }
}

/*
 * GLASS ICON BUTTON
 */
@Composable
private fun GlassIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isDark: Boolean,
    onClick: () -> Unit
) {

    Surface(
        onClick = onClick,
        modifier = Modifier.size(52.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isDark) {
            Color(0xFF101019).copy(alpha = 0.68f)
        } else {
            Color.White.copy(alpha = 0.72f)
        },
        border = BorderStroke(
            1.dp,
            if (isDark) {
                Color.White.copy(alpha = 0.13f)
            } else {
                Color.White.copy(alpha = 0.72f)
            }
        )
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                icon,
                contentDescription = contentDescription,
                tint = if (isDark) {
                    Color.White
                } else {
                    Color(0xFF241B2B)
                },
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

/*
 * META CHIP
 */
@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = AccentPurple
) {

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = if (isDark) {
            Color.White.copy(alpha = 0.055f)
        } else {
            Color.White.copy(alpha = 0.82f)
        },
        border = BorderStroke(
            1.dp,
            if (isDark) {
                Color.White.copy(alpha = 0.08f)
            } else {
                Color.Black.copy(alpha = 0.06f)
            }
        )
    ) {

        Row(
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 9.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(17.dp)
            )

            Spacer(Modifier.width(5.dp))

            Text(
                text,
                color = if (isDark) {
                    Color(0xFFE5DDE9)
                } else {
                    Color(0xFF4D4355)
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/*
 * PLANNED ACTION BUTTON
 */
@Composable
private fun PlannedActionButton(
    modifier: Modifier,
    onClick: () -> Unit,
    label: String = "Planned",
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Outlined.Bookmark
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(AccentOrange, AccentPink)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(23.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    label,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SeriesStatusButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    isDark: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val background = if (selected) {
        Brush.horizontalGradient(listOf(AccentOrange, AccentPink))
    } else {
        Brush.horizontalGradient(
            listOf(
                if (isDark) Color.White.copy(alpha = 0.045f) else Color.White.copy(alpha = 0.76f),
                if (isDark) Color.White.copy(alpha = 0.045f) else Color.White.copy(alpha = 0.76f)
            )
        )
    }
    val content = if (selected) Color.White else if (isDark) Color.White else Color(0xFF302637)

    Surface(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, if (selected) AccentOrange.copy(alpha = 0.30f) else if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(background),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text(label, color = content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SeriesEpisodesSection(
    progress: com.cheese.watchlist.data.repository.SeriesProgress,
    catalog: CatalogRepository,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color
) {
    SectionTitle(Icons.Outlined.VideoLibrary, "Episodes", isDark)

    Spacer(Modifier.height(12.dp))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (isDark) Color.White.copy(alpha = 0.045f) else Color.White.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Series progress", color = primaryText, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(3.dp))
                    Text("${progress.watched} of ${progress.total} episodes watched", color = secondaryText, style = MaterialTheme.typography.bodySmall)
                }
                Text("${progress.percentage}%", color = AccentOrange, fontWeight = FontWeight.ExtraBold)
            }

            Spacer(Modifier.height(11.dp))

            LinearProgressIndicator(
                progress = progress.progress,
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                color = AccentOrange,
                trackColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
            )

            if (progress.nextEpisode != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Next: Episode ${progress.nextEpisode.episodeNumber}", color = secondaryText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Spacer(Modifier.height(14.dp))

    progress.seasons.forEach { seasonProgress ->
        SeasonEpisodesBlock(
            seasonProgress = seasonProgress,
            catalog = catalog,
            isDark = isDark,
            primaryText = primaryText,
            secondaryText = secondaryText,
            borderColor = borderColor
        )
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun SeasonEpisodesBlock(
    seasonProgress: com.cheese.watchlist.data.repository.SeasonProgress,
    catalog: CatalogRepository,
    isDark: Boolean,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color
) {
    val episodes by catalog.observeEpisodes(seasonProgress.season.id).collectAsState(initial = emptyList())
    var expanded by rememberSaveable(seasonProgress.season.id) { mutableStateOf(true) }
    val episodeScope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color.White.copy(alpha = 0.035f) else Color.White.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        seasonProgress.season.title?.takeIf { it.isNotBlank() } ?: "Season ${seasonProgress.season.seasonNumber}",
                        color = primaryText,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text("${seasonProgress.watched} / ${seasonProgress.total} watched", color = secondaryText, style = MaterialTheme.typography.bodySmall)
                }
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = secondaryText
                )
            }

            if (expanded) {
                episodes.forEachIndexed { index, episode ->
                    if (index > 0) {
                        HorizontalDivider(color = borderColor)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (episode.watched) AccentOrange.copy(alpha = 0.14f) else if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.035f)
                        ) {
                            Text(
                                "${episode.episodeNumber}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                color = if (episode.watched) AccentOrange else secondaryText,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                episode.title?.takeIf { it.isNotBlank() } ?: "Episode ${episode.episodeNumber}",
                                color = primaryText,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                episodeScope.launch {
                                    catalog.setEpisodeWatched(episode.id, !episode.watched)
                                }
                            }
                        ) {
                            Icon(
                                if (episode.watched) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = if (episode.watched) "Watched" else "Mark watched",
                                tint = if (episode.watched) AccentOrange else secondaryText
                            )
                        }
                    }
                }
            }
        }
    }
}

/*
 * ACTION BUTTON
 */
@Composable
private fun DetailActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier,
    isDark: Boolean,
    onClick: () -> Unit,
    filled: Boolean = false
) {

    val background = if (filled) {
        AccentOrange.copy(alpha = if (isDark) 0.16f else 0.12f)
    } else {
        if (isDark) {
            Color.White.copy(alpha = 0.045f)
        } else {
            Color.White.copy(alpha = 0.76f)
        }
    }

    val content = if (filled) {
        AccentOrange
    } else {
        if (isDark) Color.White else Color(0xFF302637)
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(20.dp),
        color = background,
        border = BorderStroke(
            1.dp,
            if (filled) {
                AccentOrange.copy(alpha = 0.28f)
            } else {
                if (isDark) {
                    Color.White.copy(alpha = 0.09f)
                } else {
                    Color.Black.copy(alpha = 0.07f)
                }
            }
        )
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(23.dp)
            )

            Spacer(Modifier.width(7.dp))

            Text(
                label,
                color = content,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/*
 * SECTION TITLE
 */
@Composable
private fun SectionTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    isDark: Boolean
) {

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = AccentPurple.copy(alpha = if (isDark) 0.18f else 0.11f)
        ) {

            Icon(
                icon,
                contentDescription = null,
                tint = if (isDark) {
                    Color(0xFFB99CFF)
                } else {
                    AccentPurple
                },
                modifier = Modifier
                    .padding(8.dp)
                    .size(18.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Text(
            title,
            color = if (isDark) {
                Color(0xFFF5EFF8)
            } else {
                Color(0xFF201824)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/*
 * TAB
 */
@Composable
private fun DetailTab(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    isDark: Boolean
) {

    Surface(
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(17.dp),
        color = if (selected) {
            AccentOrange.copy(alpha = if (isDark) 0.14f else 0.10f)
        } else {
            if (isDark) {
                Color.White.copy(alpha = 0.035f)
            } else {
                Color.Black.copy(alpha = 0.025f)
            }
        },
        border = BorderStroke(
            1.dp,
            if (selected) {
                AccentOrange.copy(alpha = 0.22f)
            } else {
                if (isDark) {
                    Color.White.copy(alpha = 0.06f)
                } else {
                    Color.Black.copy(alpha = 0.05f)
                }
            }
        )
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text,
                color = if (selected) {
                    AccentOrange
                } else {
                    if (isDark) {
                        Color(0xFFAFA5B4)
                    } else {
                        Color(0xFF7B7181)
                    }
                },
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/*
 * DETAILS CARD
 */
@Composable
private fun DetailsCard(
    item: TmdbApi.Item,
    state: TmdbDetailViewModel.UiState,
    isDark: Boolean,
    panelColor: Color,
    borderColor: Color,
    primaryText: Color,
    secondaryText: Color
) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(27.dp),
        color = panelColor,
        border = BorderStroke(
            1.dp,
            borderColor
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 18.dp,
                vertical = 17.dp
            )
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentOrange.copy(alpha = 0.13f)
                ) {

                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = AccentOrange,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(19.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    "Details",
                    color = primaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(Modifier.height(4.dp))

            DetailRow(
                label = "Year",
                value = item.year.ifBlank { "—" },
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark
            )

            DetailRow(
                label = "Country",
                value = state.country.ifBlank { "—" },
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark
            )

            DetailRow(
                label = "Genre",
                value = item.genres
                    .joinToString(", ")
                    .ifBlank { "—" },
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark
            )

            DetailRow(
                label = "Director",
                value = state.director.ifBlank { "—" },
                primaryText = primaryText,
                secondaryText = secondaryText,
                accent = AccentOrange,
                isDark = isDark
            )

            DetailRow(
                label = "Writers",
                value = state.writers
                    .joinToString(", ")
                    .ifBlank { "—" },
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark
            )

            DetailRow(
                label = "Cast",
                value = state.cast
                    .take(6)
                    .joinToString(", ")
                    .ifBlank { "—" },
                primaryText = primaryText,
                secondaryText = secondaryText,
                accent = AccentPink,
                isDark = isDark
            )

            DetailRow(
                label = "Language",
                value = state.language.ifBlank { "English" },
                primaryText = primaryText,
                secondaryText = secondaryText,
                isDark = isDark
            )
        }
    }
}

/*
 * DETAIL ROW
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    primaryText: Color,
    secondaryText: Color,
    isDark: Boolean,
    accent: Color = primaryText
) {

    HorizontalDivider(
        color = if (isDark) {
            Color.White.copy(alpha = 0.07f)
        } else {
            Color.Black.copy(alpha = 0.06f)
        },
        modifier = Modifier.padding(vertical = 11.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        Text(
            label,
            color = secondaryText,
            modifier = Modifier.weight(0.36f),
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            value,
            color = accent,
            modifier = Modifier.weight(0.64f),
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}