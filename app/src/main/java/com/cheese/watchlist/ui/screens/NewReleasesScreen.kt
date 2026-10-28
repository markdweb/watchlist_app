package com.cheese.watchlist.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.AsyncImage
import com.cheese.watchlist.data.tmdb.TmdbApi
import com.cheese.watchlist.ui.theme.BackgroundDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow

private val Purple = Color(0xFF8B5CF6)
private val Orange = Color(0xFFFF8A3D)

private val LightTop = Color(0xFFFFF8F1)
private val LightBottom = Color(0xFFF4EEFF)

private val DarkTop = Color(0xFF0D0917)
private val DarkMiddle = Color(0xFF171024)
private val DarkBottom = Color(0xFF0E0A16)

@Composable
fun NewReleasesScreen(
    tmdbApi: TmdbApi,
    onBack: () -> Unit,
    onOpenTmdbItem: (Int, String) -> Unit
) {
    val dark = MaterialTheme.colorScheme.background == BackgroundDark
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    var items by remember {
        mutableStateOf<List<TmdbApi.Item>>(emptyList())
    }

    var page by rememberSaveable {
        mutableIntStateOf(1)
    }

    var loadingInitial by remember {
        mutableStateOf(true)
    }

    var loadingMore by remember {
        mutableStateOf(false)
    }

    var threeColumnMode by rememberSaveable {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    fun loadNextPage() {
        if (loadingMore) return

        loadingMore = true

        scope.launch {
            // Same loading behavior as TrendingScreen reference.
            delay(250)

            runCatching {
                tmdbApi.nowPlayingPage(page)
            }.onSuccess { result ->

                val newItems = result.filter {
                    items.none { old ->
                        old.id == it.id &&
                                old.mediaType == it.mediaType
                    }
                }

                if (newItems.isNotEmpty()) {
                    items = items + newItems
                    page += 1
                } else {
                    page += 1
                }

                error = null

            }.onFailure {
                error = it.message ?: "Unable to load New Releases"
            }

            loadingInitial = false
            loadingMore = false
        }
    }

    LaunchedEffect(Unit) {
        loadNextPage()
    }

    // Same pagination trigger as TrendingScreen.
    LaunchedEffect(gridState) {
        snapshotFlow {
            val info = gridState.layoutInfo
            val lastVisible =
                info.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = info.totalItemsCount

            lastVisible to total

        }.collectLatest { (lastVisible, total) ->

            if (
                !loadingInitial &&
                total > 0 &&
                lastVisible >= total - 5
            ) {
                loadNextPage()
            }
        }
    }

    val backgroundBrush = if (dark) {
        Brush.verticalGradient(
            listOf(
                DarkTop,
                DarkMiddle,
                DarkBottom
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                LightTop,
                LightBottom
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {

        LazyVerticalGrid(
            columns = GridCells.Fixed(
                if (threeColumnMode) 3 else 2
            ),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 92.dp,
                bottom = 34.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            items(
                items = items,
                key = {
                    "${it.mediaType}_${it.id}"
                }
            ) { item ->

                NewReleasesGridCard(
                    item = item,
                    onClick = {
                        onOpenTmdbItem(
                            item.id,
                            item.mediaType
                        )
                    }
                )
            }

            if (loadingMore) {
                item(
                    span = {
                        GridItemSpan(
                            if (threeColumnMode) 3 else 2
                        )
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = if (dark) Orange else Purple,
                            strokeWidth = 2.5.dp
                        )
                    }
                }
            }

            if (!loadingMore && error != null) {
                item(
                    span = {
                        GridItemSpan(
                            if (threeColumnMode) 3 else 2
                        )
                    }
                ) {
                    Text(
                        text = error
                            ?: "Unable to load New Releases",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Same header layout as TrendingScreen.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            color = if (dark) DarkTop else LightTop,
            shadowElevation = 0.dp
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp,
                        bottom = 4.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                GlassIconButton(
                    icon = Icons.Outlined.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                    dark = dark
                )

                Text(
                    text = "New Releases",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                GlassIconButton(
                    icon = if (threeColumnMode) {
                        Icons.Outlined.GridView
                    } else {
                        Icons.Outlined.Apps
                    },
                    contentDescription = if (threeColumnMode) {
                        "Two column view"
                    } else {
                        "Three column view"
                    },
                    onClick = {
                        threeColumnMode = !threeColumnMode
                    },
                    dark = dark
                )
            }
        }

        if (loadingInitial && items.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(30.dp)
                    .align(Alignment.Center),
                color = if (dark) Orange else Purple,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    dark: Boolean
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (dark) {
            Color.White.copy(alpha = 0.035f)
        } else {
            Color.White.copy(alpha = 0.82f)
        },
        border = BorderStroke(
            1.dp,
            if (dark) {
                Color.White.copy(alpha = 0.13f)
            } else {
                Purple.copy(alpha = 0.16f)
            }
        ),
        shadowElevation = if (dark) 0.dp else 2.dp
    ) {

        Box(
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(24.dp),
                tint = if (dark) {
                    Color.White.copy(alpha = 0.82f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun NewReleasesGridCard(
    item: TmdbApi.Item,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        AsyncImage(
            // Use backdrop as fallback if TMDB has no poster.
            model = item.posterUrl ?: item.backdropUrl,
            contentDescription = item.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.68f)
                .clip(
                    RoundedCornerShape(20.dp)
                ),
            contentScale = ContentScale.Crop
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Text(
            text = item.title,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp
            ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = Icons.Outlined.Language,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.width(4.dp)
            )

            Text(
                text = String.format(
                    "%.1f",
                    item.rating
                ),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp
                ),
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}