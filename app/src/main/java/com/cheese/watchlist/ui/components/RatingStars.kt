package com.cheese.watchlist.ui.components

import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheese.watchlist.ui.theme.StarYellow

@Composable
fun RatingStars(
    rating: Int,
    modifier: Modifier = Modifier,
    starSize: androidx.compose.ui.unit.Dp = 18.dp,
    onRatingChange: ((Int) -> Unit)? = null
) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            val filled = i <= rating
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = "Star $i",
                tint = if (filled) StarYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(starSize)
                    .then(
                        if (onRatingChange != null) Modifier.clickable { onRatingChange(i) }
                        else Modifier
                    )
            )
        }
    }
}
