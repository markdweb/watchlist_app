package com.cheese.watchlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.navigation.Screen

data class BottomNavDestination(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

val bottomNavDestinations = listOf(
    BottomNavDestination(
        Screen.Home,
        "Home",
        Icons.Outlined.Home
    ),
    BottomNavDestination(
        Screen.Lists,
        "Lists",
        Icons.Outlined.CollectionsBookmark
    ),
    BottomNavDestination(
        Screen.Profile,
        "Profile",
        Icons.Outlined.Person
    )
)

@Composable
fun WatchlistBottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    val navShape = RoundedCornerShape(30.dp)
    val selectedShape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = 20.dp,
                end = 20.dp,
                bottom = 12.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = navShape,
                    clip = false
                )
                .clip(navShape)
                .background(
                    MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.82f
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.12f
                    ),
                    shape = navShape
                )
                .padding(
                    horizontal = 7.dp,
                    vertical = 7.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavDestinations.forEach { destination ->

                val selected =
                    currentRoute == destination.screen.route

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(selectedShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.14f
                                )
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable(
                            indication = null,
                            interactionSource = null
                        ) {
                            if (!selected) {
                                onNavigate(destination.screen)
                            }
                        }
                        .padding(
                            horizontal = 8.dp,
                            vertical = 10.dp
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            modifier = Modifier.size(23.dp),
                            tint =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                        )

                        Spacer(
                            modifier = Modifier.size(3.dp)
                        )

                        Text(
                            text = destination.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight =
                                if (selected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Medium
                                },
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                        )
                    }
                }
            }
        }
    }
}
