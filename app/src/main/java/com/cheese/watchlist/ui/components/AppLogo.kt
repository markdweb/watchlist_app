package com.cheese.watchlist.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's brand mark: a rounded film-strip square with a play triangle,
 * matching the launcher icon. Reused on the splash and onboarding screens.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val s = this.size.minDimension
            val holeSize = s * 0.09f
            val margin = s * 0.16f

            fun hole(xFraction: Float, yFraction: Float) {
                drawRect(
                    color = Color.White.copy(alpha = 0.9f),
                    topLeft = Offset(xFraction * s - holeSize / 2, yFraction * s - holeSize / 2),
                    size = Size(holeSize, holeSize)
                )
            }
            // sprocket holes, left + right columns
            listOf(0.32f, 0.5f, 0.68f).forEach { y ->
                hole(margin / s + 0.03f, y)
                hole(1f - margin / s - 0.03f, y)
            }

            // play triangle, centered
            val triangleWidth = s * 0.26f
            val triangleHeight = s * 0.32f
            val cx = s * 0.5f
            val cy = s * 0.5f
            val path = Path().apply {
                moveTo(cx - triangleWidth / 2, cy - triangleHeight / 2)
                lineTo(cx + triangleWidth / 2, cy)
                lineTo(cx - triangleWidth / 2, cy + triangleHeight / 2)
                close()
            }
            drawPath(path, color = Color.White, style = Fill)
        }
    }
}
