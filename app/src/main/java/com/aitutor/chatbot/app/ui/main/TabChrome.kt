package com.aitutor.chatbot.app.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.OnHero
import com.aitutor.chatbot.app.ui.theme.SectionOverlineStyle
import com.aitutor.chatbot.app.ui.theme.appColors

/**
 * The chrome the four tabs share. Home predates it and keeps its own hero, because that one also
 * carries the composer and the suggestion rail; everything here is what Tools, History and Profile
 * repeat verbatim, and pulling it out is what keeps those three screens readable.
 */

/** Every tab's gutter. Narrower than [Dimens.screenPadding], which belongs to the setup flow. */
val TabSidePadding = 20.dp

/**
 * How much room the floating nav pill needs at the bottom of a scrolling tab. The system navigation
 * bar is not included: each tab insets its own scrolling viewport, so this only has to clear the
 * pill itself.
 */
val TabBottomInset = 124.dp

private val HeroShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
private val StatStripShape = RoundedCornerShape(20.dp)
private val PanelShape = RoundedCornerShape(22.dp)
private val ShadowTint = Color(0xFF080C28)

/**
 * The accent panel each tab opens with: a corner glow and two hairline rings that bleed off the
 * bottom-left. [bottomPadding] is deliberately generous — the card that follows overlaps it.
 */
@Composable
fun TabHero(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 68.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(colors.hero),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val glowCenter = Offset(size.width + 50.dp.toPx(), 30.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = glowCenter,
                    radius = 160.dp.toPx(),
                ),
                radius = 160.dp.toPx(),
                center = glowCenter,
            )
            val hairline = Stroke(width = 1.dp.toPx())
            val ringCenter = Offset(30.dp.toPx(), size.height + 30.dp.toPx())
            drawCircle(Color.White.copy(alpha = 0.12f), 180.dp.toPx(), ringCenter, style = hairline)
            drawCircle(Color.White.copy(alpha = 0.10f), 130.dp.toPx(), ringCenter, style = hairline)
        }
        Column(
            // The accent panel itself reaches the top of the display; only its contents move down.
            modifier = Modifier
                .statusBarsPadding()
                .padding(
                    start = TabSidePadding,
                    end = TabSidePadding,
                    top = 24.dp,
                    bottom = bottomPadding,
                ),
            content = content,
        )
    }
}

/** A tab's title and subtitle, with room for a trailing action. */
@Composable
fun TabHeroTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = colors.onAccentMuted,
                modifier = Modifier.padding(top = Dimens.spaceXxs),
            )
        }
        action?.invoke()
    }
}

/** A 44dp translucent circle for an action sitting on the hero. */
@Composable
fun HeroIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(AppShapes.Pill)
            .background(OnHero.FillSoft)
            .border(1.dp, OnHero.StrokeSoft, AppShapes.Pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Three figures side by side on the hero, hairline-divided. History and Profile both open with it. */
@Composable
fun HeroStatStrip(
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(StatStripShape)
            .background(OnHero.FillFaint)
            .border(1.dp, OnHero.StrokeSoft, StatStripShape)
            .padding(vertical = Dimens.spaceLg, horizontal = Dimens.spaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, (value, label) ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(OnHero.Stroke)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.66).sp,
                    ),
                    color = Color.White,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.onAccentMuted,
                )
            }
        }
    }
}

/**
 * The 56dp search pill. It is a static field, not a text input: nothing can be searched until the
 * data layer exists, so it reads as a button and says so to a screen reader.
 */
@Composable
fun TabSearchBar(
    hint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, AppShapes.Pill, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(AppShapes.Pill)
            .background(colors.surface)
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceXl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = AppIcons.Search,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.textTertiary,
        )
    }
}

/** The uppercase label above a group of rows. */
@Composable
fun SectionOverline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = SectionOverlineStyle,
        color = MaterialTheme.appColors.textTertiary,
        modifier = modifier,
    )
}

/** A section title with the count badge the Tools screen sets beside it. */
@Composable
fun SectionTitleWithCount(title: String, count: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 19.sp,
                lineHeight = 24.sp,
                letterSpacing = (-0.48).sp,
            ),
            color = colors.textPrimary,
        )
        Box(
            modifier = Modifier
                .height(22.dp)
                .clip(AppShapes.Pill)
                .background(colors.accentTint)
                .padding(horizontal = 7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = colors.accentText,
            )
        }
    }
}

/**
 * The grouped-rows card: one surface, rows stacked inside it, hairlines between them. The 4dp of
 * vertical padding is what stops the first and last row from touching the rounded corners.
 */
@Composable
fun ListPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, PanelShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(PanelShape)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, PanelShape)
            .padding(vertical = Dimens.spaceXxs),
        content = content,
    )
}

/**
 * The hairline between two panel rows. It starts under the text rather than at the card edge, so
 * the icon column reads as one unbroken strip — [inset] is where that column ends.
 */
@Composable
fun PanelDivider(inset: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = inset, end = Dimens.spaceLg + 2.dp)
            .height(1.dp)
            .background(MaterialTheme.appColors.cardBorder)
    )
}

/** Inserts a [PanelDivider] between items, but not above the first or below the last. */
@Composable
fun <T> ColumnScope.dividedRows(
    items: List<T>,
    inset: Dp,
    row: @Composable (T) -> Unit,
) {
    items.forEachIndexed { index, item ->
        if (index > 0) PanelDivider(inset = inset)
        row(item)
    }
}

/** The filter chips. On the hero they invert; on the page they sit as outlined surfaces. */
@Composable
fun FilterChipRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onHero: Boolean = false,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                onHero = onHero,
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onHero: Boolean,
) {
    val colors = MaterialTheme.appColors
    val background = when {
        onHero && selected -> Color.White
        onHero -> OnHero.FillSoft
        selected -> colors.accent
        else -> colors.surface
    }
    val foreground = when {
        onHero && selected -> colors.accent
        onHero -> Color.White
        selected -> colors.onAccent
        else -> colors.textPrimary
    }

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(AppShapes.Pill)
            .background(background)
            .then(
                when {
                    onHero && !selected -> Modifier.border(1.dp, OnHero.StrokeSoft, AppShapes.Pill)
                    !onHero && !selected -> Modifier.border(1.dp, colors.cardBorder, AppShapes.Pill)
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.spaceMd + Dimens.spaceXxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = foreground,
            maxLines = 1,
        )
    }
}

/** The small accent-tinted chevron that closes a tappable row. */
@Composable
fun RowChevron(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(AppShapes.Pill)
            .background(colors.accentTint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** A rounded accent-tinted tile holding a tool or setting glyph. */
@Composable
fun GlyphTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    cornerRadius: Dp = 16.dp,
    filled: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (filled) colors.accent else colors.accentTint)
            .then(if (filled) Modifier else Modifier.border(1.dp, colors.accentRing, shape)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) colors.onAccent else colors.accentText,
            modifier = Modifier.size(size * 0.46f),
        )
    }
}

/** The pill that marks a tool as new. */
@Composable
fun NewBadge(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .height(20.dp)
            .clip(AppShapes.Pill)
            .background(colors.accent)
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.21.sp,
            ),
            color = colors.onAccent,
        )
    }
}

/** A title-and-subtitle pair that truncates rather than wraps, as every panel row in the design does. */
@Composable
fun RowLabels(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            badge?.invoke()
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The oversized ring that bleeds out of a featured card's bottom-right corner. Drawn rather than
 * bordered because it has to be clipped by the card it overflows.
 */
@Composable
fun BoxScope.CardCornerRing() {
    Canvas(modifier = Modifier.matchParentSize()) {
        drawCircle(
            color = Color.White.copy(alpha = 0.07f),
            radius = 66.dp.toPx(),
            center = Offset(size.width + 35.dp.toPx(), size.height + 15.dp.toPx()),
            style = Stroke(width = 18.dp.toPx()),
        )
    }
}
