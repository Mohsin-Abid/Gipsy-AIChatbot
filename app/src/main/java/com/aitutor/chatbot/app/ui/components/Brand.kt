package com.aitutor.chatbot.app.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.OnHero

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
