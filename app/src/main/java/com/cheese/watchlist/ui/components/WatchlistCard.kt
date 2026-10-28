package com.cheese.watchlist.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cheese.watchlist.data.entities.WatchItem
import com.cheese.watchlist.data.entities.WatchType
import com.cheese.watchlist.ui.theme.BackgroundDark
import com.cheese.watchlist.ui.theme.StarYellow

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w342"

private val CardPurple = Color(0xFF8B5CF6)
private val CardPink = Color(0xFFEC4899)

@Composable
fun WatchlistCard(
    item: WatchItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenUrl: () -> Unit,
    onToggleFavorite: () -> Unit,
    onIncrementEpisode: () -> Unit,
    onDecrementEpisode: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    val isDark = MaterialTheme.colorScheme.background == BackgroundDark

    val cardBackground = if (isDark) {
        Color(0xFF17121F)
    } else {
        Color.White
    }

    val cardBorder = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDark) {
            Color.White.copy(alpha = 0.08f)
        } else {
            Color.Black.copy(alpha = 0.07f)
        }
    }

    val primaryText = if (isDark) {
        Color(0xFFF5F0F8)
    } else {
        Color(0xFF1B1520)
    }

    val secondaryText = if (isDark) {
        Color(0xFFAFA4B5)
    } else {
        Color(0xFF746A7B)
    }

    val iconBackground = if (isDark) {
        Color.White.copy(alpha = 0.055f)
    } else {
        Color(0xFFF5F0FA)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    cardBackground
                }
            )
            .border(
                BorderStroke(1.dp, cardBorder),
                RoundedCornerShape(24.dp)
            )
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongPress
            )
            .padding(14.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {

            /*
             * TMDB POSTER
             *
             * If posterPath exists, load the real TMDB poster.
             * Otherwise show the existing placeholder.
             */
            Box(
                modifier = Modifier
                    .width(82.dp)
                    .height(122.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                CardPurple.copy(
                                    alpha = if (isDark) 0.24f else 0.12f
                                ),
                                CardPink.copy(
                                    alpha = if (isDark) 0.16f else 0.08f
                                )
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {

                if (!item.posterPath.isNullOrBlank() && !selectionMode) {

                    AsyncImage(
                        model = TMDB_IMAGE_BASE + item.posterPath,
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                } else if (selectionMode) {

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        border = if (isSelected) {
                            null
                        } else {
                            BorderStroke(
                                2.dp,
                                if (isDark) {
                                    Color.White.copy(alpha = 0.55f)
                                } else {
                                    Color.Black.copy(alpha = 0.25f)
                                }
                            )
                        }
                    ) {

                        Icon(
                            imageVector = if (isSelected) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Outlined.Circle
                            },
                            contentDescription = if (isSelected) {
                                "Selected"
                            } else {
                                "Not selected"
                            },
                            tint = if (isSelected) {
                                Color.White
                            } else {
                                secondaryText
                            },
                            modifier = Modifier
                                .padding(10.dp)
                                .size(26.dp)
                        )
                    }

                } else {

                    Icon(
                        imageVector = if (item.type == WatchType.MOVIE) {
                            Icons.Outlined.Movie
                        } else {
                            Icons.Outlined.Tv
                        },
                        contentDescription = item.type.name,
                        tint = if (isDark) {
                            Color(0xFFD2BFFF)
                        } else {
                            CardPurple
                        },
                        modifier = Modifier.size(32.dp)
                    )
                }

                /*
                 * Small gradient overlay makes poster text/details
                 * readable if we add them later.
                 */
                if (!item.posterPath.isNullOrBlank() && !selectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.18f)
                                    )
                                )
                            )
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                /*
                 * TITLE + FAVORITE
                 */
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = primaryText,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!selectionMode) {

                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(34.dp)
                        ) {

                            Icon(
                                imageVector = if (item.favorite) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Outlined.StarOutline
                                },
                                contentDescription = "Favorite",
                                tint = if (item.favorite) {
                                    StarYellow
                                } else {
                                    secondaryText
                                },
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                /*
                 * TYPE + STATUS
                 */
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    StatusPill(
                        text = if (item.type == WatchType.MOVIE) {
                            "Movie"
                        } else {
                            "Series"
                        },
                        color = CardPurple,
                        isDark = isDark
                    )

                    Spacer(Modifier.width(7.dp))

                    StatusPill(
                        text = item.status.label,
                        color = statusColor(item.status),
                        isDark = isDark
                    )
                }

                /*
                 * YEAR
                 */
                if (item.yearDate.isNotBlank()) {

                    Spacer(Modifier.height(7.dp))

                    Text(
                        text = item.yearDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = secondaryText,
                        fontWeight = FontWeight.Medium
                    )
                }

                /*
                 * GENRE
                 */
                if (item.genre.isNotBlank()) {

                    Spacer(Modifier.height(5.dp))

                    Text(
                        text = item.genre,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                /*
                 * RATING
                 */
                if (item.rating > 0) {

                    Spacer(Modifier.height(7.dp))

                    RatingStars(
                        rating = item.rating,
                        starSize = 15.dp
                    )
                }

                /*
                 * SERIES PROGRESS
                 */
                if (
                    item.type == WatchType.SERIES &&
                    item.totalEpisodes > 0
                ) {

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Season ${item.season}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    EpisodeProgress(
                        currentEpisode = item.currentEpisode,
                        totalEpisodes = item.totalEpisodes,
                        showControls = !selectionMode,
                        onIncrement = onIncrementEpisode,
                        onDecrement = onDecrementEpisode
                    )
                }
            }
        }

        /*
         * BOTTOM ACTION AREA
         */
        if (!selectionMode) {

            Spacer(Modifier.height(13.dp))

            HorizontalDivider(
                color = if (isDark) {
                    Color.White.copy(alpha = 0.07f)
                } else {
                    Color.Black.copy(alpha = 0.06f)
                }
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (item.url.isNotBlank()) {

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconBackground)
                            .padding(
                                horizontal = 10.dp,
                                vertical = 7.dp
                            )
                            .clickable(onClick = onOpenUrl),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = "Open link",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )

                        Spacer(Modifier.width(6.dp))

                        Text(
                            text = item.url,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                } else {

                    Spacer(Modifier.weight(1f))
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {

                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = if (isDark) {
                            Color(0xFFB7AEBB)
                        } else {
                            Color(0xFF766D79)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    color: Color,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(
            alpha = if (isDark) 0.16f else 0.10f
        )
    ) {

        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 5.dp
            ),
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}