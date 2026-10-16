package com.askylix.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.askylix.app.R
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.theme.AppIconColors
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.OnHero
import com.askylix.app.ui.theme.Sunshine

/** The brand mark on a rounded tile — the same object at every size the design uses it. */
@Composable
fun LogoTile(
    background: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.logoSmall,
    shape: Shape = AppShapes.LogoSmall,
    iconSize: Dp = size * 0.56f,
    border: Color? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Logo,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Mark plus wordmark, as it appears in the onboarding hero header. */
@Composable
fun BrandRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LogoTile(
            background = OnHero.Fill,
            iconTint = OnHero.Text,
            border = OnHero.Stroke,
            iconSize = 19.dp,
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleSmall,
            color = OnHero.Text,
        )
    }
}

/** A translucent pill on the accent hero — the step counter and the little context chips. */
@Composable
fun HeroBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(AppShapes.Pill)
            .background(OnHero.FillSoft)
            .border(1.dp, OnHero.StrokeSoft, AppShapes.Pill)
            .padding(horizontal = if (icon != null) 12.dp else 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OnHero.Text,
                modifier = Modifier.size(15.dp),
            )
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = OnHero.Text)
    }
}

/**
 * The app icon as artwork: the blue tile, a chat bubble holding an open book, and the yellow spark.
 *
 * Drawn from the icon's own paths on its 512-unit square, so it stays sharp at any size and needs no
 * bitmap. The launcher's adaptive icon cannot be drawn inside the app, and the plain [LogoTile] mark
 * is only the book and spark, not the whole icon.
 */
@Composable
fun AppIconMark(
    modifier: Modifier = Modifier,
    tileSize: Dp = Dimens.logoLarge,
    shape: Shape = AppShapes.AppIconLarge,
) {
    val art = remember { AppIconPaths() }
    Canvas(modifier = modifier.size(tileSize).clip(shape)) {
        val unit = size.width / AppIconPaths.UNITS
        // Inside the scaled space the square is 512 units a side, whatever the tile's size in pixels.
        val square = Size(AppIconPaths.UNITS, AppIconPaths.UNITS)
        withTransform({ scale(unit, unit, pivot = Offset.Zero) }) {
            drawRect(
                size = square,
                brush = Brush.linearGradient(
                    colors = listOf(AppIconColors.BackgroundStart, AppIconColors.BackgroundEnd),
                    start = Offset.Zero,
                    end = Offset(AppIconPaths.UNITS, AppIconPaths.UNITS),
                ),
            )
            drawRect(
                size = square,
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(AppIconPaths.UNITS * 0.2f, AppIconPaths.UNITS * 0.16f),
                    radius = AppIconPaths.UNITS * 0.75f,
                ),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = 236f,
                center = Offset(256f, 256f),
                style = Stroke(width = 2f),
            )

            // The mark sits a little left of and below the square's centre, which is where the icon puts it.
            withTransform({ translate(left = -25f, top = 10f) }) {
                drawPath(art.bubble, color = Color.White)
                drawPath(art.pageLeft, color = AppIconColors.PageLeft)
                drawPath(art.pageRight, color = AppIconColors.PageRight)
                drawPath(art.spine, color = Color.White, style = Stroke(width = 7f, cap = StrokeCap.Round))
                val line = Stroke(width = 8f, cap = StrokeCap.Round)
                drawPath(art.leftLines, color = Color.White.copy(alpha = 0.75f), style = line)
                drawPath(art.rightLines, color = Color.White.copy(alpha = 0.75f), style = line)
                // The outline goes down first and the fill over it, so only the outer half of the stroke shows.
                drawPath(
                    art.spark,
                    color = AppIconColors.SparkOutline,
                    style = Stroke(width = 12f, join = StrokeJoin.Round),
                )
                drawPath(art.spark, color = Sunshine)
                drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 10f, center = Offset(326f, 92f))
            }
        }
    }
}

/** The icon's shapes, parsed once. Coordinates are the icon's own, on a 512-unit square. */
private class AppIconPaths {
    val bubble = path(
        "M150 132H362A56 56 0 0 1 418 188V316A56 56 0 0 1 362 372H238L176 418C168 424 158 418 161 409" +
            "L172 372H150A56 56 0 0 1 94 316V188A56 56 0 0 1 150 132Z"
    )
    val pageLeft = path("M256 206C232 192 200 187 168 193V302C200 296 232 300 256 314Z")
    val pageRight = path("M256 206C280 192 312 187 344 193V302C312 296 280 300 256 314Z")
    val spine = path("M256 204V316")
    val leftLines = path("M190 228C206 224 224 226 238 232M190 254C206 250 224 252 238 258M190 280C204 276 218 277 230 282")
    val rightLines = path("M274 232C288 226 306 224 322 228M274 258C288 252 306 250 322 254")
    val spark = path("M396 84C400 128 411 139 458 146C411 153 400 164 396 208C392 164 381 153 334 146C381 139 392 128 396 84Z")

    private fun path(data: String): Path = PathParser().parsePathString(data).toPath()

    companion object {
        const val UNITS = 512f
    }
}
