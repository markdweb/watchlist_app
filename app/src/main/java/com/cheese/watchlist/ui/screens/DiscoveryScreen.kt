package com.cheese.watchlist.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.cheese.watchlist.data.repository.CatalogRepository
import com.cheese.watchlist.data.tmdb.TmdbApi
import com.cheese.watchlist.ui.theme.BackgroundDark
import com.cheese.watchlist.viewmodel.DiscoveryUiState
import com.cheese.watchlist.viewmodel.DiscoveryViewModel

private val Purple = Color(0xFF8B5CF6)
private val Pink = Color(0xFFEC4899)
private val Orange = Color(0xFFFF8A3D)

private val LightTop = Color(0xFFFFF8F1)
private val LightBottom = Color(0xFFF7F1FF)

private val DarkTop = Color(0xFF0D0917)
private val DarkMiddle = Color(0xFF171024)
private val DarkBottom = Color(0xFF0E0A16)

private val DarkCard = Color(0xFF1B1427)

@Composable
fun DiscoveryScreen(
    catalog: CatalogRepository,
    onOpenWatchlist: () -> Unit,
    onOpenTmdbItem: (Int, String) -> Unit,
    onOpenTrending: () -> Unit,
    onOpenNewReleases: () -> Unit,
    onOpenUpcoming: () -> Unit
) {
    val vm: DiscoveryViewModel = viewModel(
        factory = DiscoveryViewModel.Factory(
            TmdbApi(),
            catalog
        )
    )

    val state by vm.state.collectAsState()
    val localMovies by catalog.allMovies.collectAsState(initial = emptyList())
    val localSeries by catalog.allSeries.collectAsState(initial = emptyList())

    var selectedTab by remember {
        mutableStateOf("All")
    }

    val dark = MaterialTheme.colorScheme.background == BackgroundDark

    val backgroundBrush = if (dark) {
        Brush.verticalGradient(
            colors = listOf(
                DarkTop,
                DarkMiddle,
                DarkBottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                LightTop,
                LightBottom,
                Color(0xFFF4EEFF)
            )
        )
    }

    val primary = if (dark) {
        Orange
    } else {
        Purple
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush),
        contentPadding = PaddingValues(
            top = 18.dp,
            bottom = 120.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        /*
         * ---------------------------------------------------------
         * HEADER
         * ---------------------------------------------------------
         */

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(
                                    if (dark) {
                                        Purple.copy(alpha = 0.18f)
                                    } else {
                                        Purple.copy(alpha = 0.10f)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Movie,
                                contentDescription = null,
                                tint = if (dark) Color.White else Purple,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Movie List",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Text(
                                text = "Discover something worth watching",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                }

                /*
                 * SEARCH
                 */

                Box(Modifier.fillMaxWidth().zIndex(10f)) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = vm::search,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        leadingIcon = { Icon(Icons.Outlined.Search, null, Modifier.size(27.dp)) },
                        placeholder = { Text("Search movies, series, anime...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (dark) Orange else Purple,
                            unfocusedBorderColor = if (dark) Color.White.copy(alpha = .12f) else Purple.copy(alpha = .22f),
                            focusedContainerColor = if (dark) Color.White.copy(alpha = .035f) else Color.White.copy(alpha = .80f),
                            unfocusedContainerColor = if (dark) Color.White.copy(alpha = .025f) else Color.White.copy(alpha = .70f)
                        )
                    )
                    if (state.searchQuery.isNotBlank() && state.searchResults.isNotEmpty()) {
                        Surface(
                            Modifier.fillMaxWidth().offset(y = 64.dp).zIndex(20f),
                            RoundedCornerShape(18.dp),
                            color = if (dark) DarkCard else Color.White,
                            tonalElevation = 8.dp, shadowElevation = 8.dp
                        ) {
                            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                                state.searchResults.take(8).forEach { item ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable { vm.search(""); onOpenTmdbItem(item.id, item.mediaType) }.padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(item.posterUrl ?: item.backdropUrl, item.title, Modifier.width(42.dp).height(62.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(item.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(Modifier.height(3.dp))
                                            Text(if (item.mediaType == "tv") "Series • ${item.year}" else "Movie • ${item.year}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                /*
                 * CATEGORY CHIPS
                 */

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(
                            rememberScrollState()
                        ),
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {

                    listOf(
                        "All",
                        "Movies",
                        "Series",
                        "Animation",
                        "Anime",
                        "Top"
                    ).forEach { tab ->

                        val selected = selectedTab == tab

                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTab = tab
                            },
                            label = {
                                Text(
                                    text = tab,
                                    fontWeight = if (selected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    }
                                )
                            },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (dark) {
                                    Orange.copy(alpha = 0.16f)
                                } else {
                                    Purple.copy(alpha = 0.12f)
                                },
                                selectedLabelColor = if (dark) {
                                    Color.White
                                } else {
                                    Purple
                                },
                                selectedLeadingIconColor = if (dark) {
                                    Orange
                                } else {
                                    Purple
                                },
                                containerColor = if (dark) {
                                    Color.White.copy(alpha = 0.035f)
                                } else {
                                    Color.White.copy(alpha = 0.65f)
                                }
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = if (dark) {
                                    Color.White.copy(alpha = 0.09f)
                                } else {
                                    Purple.copy(alpha = 0.12f)
                                },
                                selectedBorderColor = if (dark) {
                                    Orange.copy(alpha = 0.35f)
                                } else {
                                    Purple.copy(alpha = 0.25f)
                                }
                            )
                        )
                    }
                }
            }
        }

        /*
         * ---------------------------------------------------------
         * ERROR
         * ---------------------------------------------------------
         */

        state.error?.let { message ->

            item {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        Text(
                            text = "TMDB isn't connected yet",
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            text = "Add TMDB_API_KEY=your_key to local.properties, then rebuild.",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        /*
         * ---------------------------------------------------------
         * LOADING
         * ---------------------------------------------------------
         */

        if (state.loading) {

            item {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator(
                        color = primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        }

        else {

            /*
             * -----------------------------------------------------
             * TRENDING
             * -----------------------------------------------------
             */

            item {

                SectionHeader(
                    text = "Trending Now",
                    accent = Pink,
                    showAction = true,
                    onAction = onOpenTrending
                )
            }

            item {

                DiscoveryRow(
                    items = state.trending.filter {
                        matchesTab(it, selectedTab)
                    },
                    localMovies = localMovies,
                    localSeries = localSeries,
                    onOpen = onOpenTmdbItem
                )
            }


            /*
             * -----------------------------------------------------
             * NEW RELEASES
             * -----------------------------------------------------
             */

            if (selectedTab != "Series") {

                item {

                    SectionHeader(
                        text = "New Releases",
                        accent = Orange,
                        showAction = true,
                        onAction = onOpenNewReleases
                    )
                }

                item {

                    DiscoveryRow(
                        items = state.newReleases.filter {
                            matchesTab(it, selectedTab)
                        },
                        localMovies = localMovies,
                        localSeries = localSeries,
                        onOpen = onOpenTmdbItem
                    )
                }
            }

            /*
             * -----------------------------------------------------
             * UPCOMING
             * -----------------------------------------------------
             */

            if (selectedTab != "Series") {

                item {

                    SectionHeader(
                        text = "Upcoming",
                        accent = Orange,
                        showAction = true,
                        onAction = onOpenUpcoming
                    )
                }

                item {

                    DiscoveryRow(
                        items = state.upcoming.filter {
                            matchesTab(it, selectedTab)
                        },
                        localMovies = localMovies,
                        localSeries = localSeries,
                        onOpen = onOpenTmdbItem
                    )
                }
            }



            /*
             * -----------------------------------------------------
             * YOUR LIBRARY
             * -----------------------------------------------------
             */

            item {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {

                    Text(
                        text = "YOUR LIBRARY",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary
                    )

                    Text(
                        text = "Save anything you want to watch and continue tracking it from My List.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onOpenWatchlist,
                        modifier = Modifier.padding(top = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary
                        )
                    ) {
                        Text(
                            text = "Open My List",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "This product uses the TMDB API but is not endorsed or certified by TMDB.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Watch-provider data is supplied by JustWatch when available.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/*
 * ================================================================
 * SECTION HEADER
 * ================================================================
 */

@Composable
private fun SectionHeader(
    text: String,
    accent: Color,
    showAction: Boolean = false,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                )
        )

        Spacer(modifier = Modifier.width(10.dp))

        if (showAction) {
            Surface(
                modifier = if (onAction != null) {
                    Modifier.clickable { onAction.invoke() }
                } else {
                    Modifier
                },
                shape = CircleShape,
                color = if (MaterialTheme.colorScheme.background == BackgroundDark) {
                    Color.White.copy(alpha = 0.035f)
                } else {
                    Color.White.copy(alpha = 0.70f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (MaterialTheme.colorScheme.background == BackgroundDark) {
                        Color.White.copy(alpha = 0.12f)
                    } else {
                        Purple.copy(alpha = 0.16f)
                    }
                ),
                shadowElevation = if (MaterialTheme.colorScheme.background == BackgroundDark) 0.dp else 2.dp
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "Open section",
                        tint = if (MaterialTheme.colorScheme.background == BackgroundDark) {
                            Color.White.copy(alpha = 0.82f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

/*
 * ================================================================
 * DISCOVERY ROW
 * ================================================================
 */

@Composable
private fun DiscoveryRow(
    items: List<TmdbApi.Item>,
    localMovies: List<com.cheese.watchlist.data.entities.WatchItem>,
    localSeries: List<com.cheese.watchlist.data.entities.SeriesEntity>,
    onOpen: (Int, String) -> Unit
) {

    if (items.isEmpty()) {

        Text(
            text = "Nothing to show",
            modifier = Modifier.padding(horizontal = 20.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        return
    }

    LazyRow(
        modifier = Modifier.height(292.dp),
        contentPadding = PaddingValues(
            horizontal = 20.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        lazyItems(
            items = items
                .distinctBy {
                    "${it.mediaType}_${it.id}"
                }
                .take(12),
            key = {
                "${it.mediaType}_${it.id}"
            }
        ) { item ->

            DiscoveryCard(
                item = item,
                showWantBadge = if (item.mediaType == "tv") {
                    localSeries.any {
                        it.url == item.tmdbUrl &&
                                it.status == com.cheese.watchlist.data.entities.WatchStatus.TO_WATCH
                    }
                } else {
                    localMovies.any {
                        it.tmdbId == item.id &&
                                it.status == com.cheese.watchlist.data.entities.WatchStatus.TO_WATCH
                    }
                },
                showWatchingBadge = if (item.mediaType == "tv") {
                    localSeries.any {
                        it.url == item.tmdbUrl &&
                                it.status == com.cheese.watchlist.data.entities.WatchStatus.WATCHING
                    }
                } else {
                    localMovies.any {
                        it.tmdbId == item.id &&
                                it.status == com.cheese.watchlist.data.entities.WatchStatus.WATCHING
                    }
                },
                showWatchedBadge = if (item.mediaType == "tv") {
                    localSeries.any {
                        it.url == item.tmdbUrl &&
                                it.status == com.cheese.watchlist.data.entities.WatchStatus.WATCHED
                    }
                } else {
                    localMovies.any {
                        it.tmdbId == item.id &&
                                it.status == com.cheese.watchlist.data.entities.WatchStatus.WATCHED
                    }
                },
                onOpen = onOpen
            )
        }
    }
}

/*
 * ================================================================
 * DISCOVERY CARD
 * ================================================================
 */

@Composable
private fun DiscoveryCard(
    item: TmdbApi.Item,
    showWantBadge: Boolean,
    showWatchingBadge: Boolean,
    showWatchedBadge: Boolean,
    onOpen: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(142.dp)
            .height(292.dp)
            .clickable {
                onOpen(item.id, item.mediaType)
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )

                if (showWantBadge) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Orange,
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "WANT",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(
                                horizontal = 18.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                } else if (showWatchingBadge) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF35B978),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "WATCHING",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(
                                horizontal = 15.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                } else if (showWatchedBadge) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF3B82F6),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = "WATCHED",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(
                                horizontal = 15.dp,
                                vertical = 6.dp
                            )
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(
                    if (showWantBadge || showWatchingBadge || showWatchedBadge) 17.dp else 9.dp
                )
            )

            Text(
                text = item.title,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(5.dp))

                Text(
                    text = String.format("%.1f", item.rating),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/*
 * ================================================================
 * FILTERING
 * ================================================================
 */

private fun matchesTab(
    item: TmdbApi.Item,
    tab: String
): Boolean {

    return when (tab) {

        "Movies" -> {
            item.mediaType == "movie"
        }

        "Series" -> {
            item.mediaType == "tv"
        }

        "Animation" -> {
            item.genres.any {
                it.contains(
                    "animation",
                    ignoreCase = true
                )
            }
        }

        "Anime" -> {
            item.genres.any {
                it.contains(
                    "animation",
                    ignoreCase = true
                )
            }
        }

        "Top" -> {
            item.rating >= 7.5
        }

        else -> true
    }
}