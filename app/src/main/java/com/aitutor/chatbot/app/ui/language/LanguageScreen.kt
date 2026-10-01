package com.aitutor.chatbot.app.ui.language

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.ui.components.PrimaryCta
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppInsets
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.bottomSafePadding
import com.aitutor.chatbot.app.ui.theme.appColors

private val CardShape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
private val FieldShape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)

/**
 * Language picker. The list scrolls under a gradient fade rather than a hard divider, so the
 * bottom bar reads as floating over the content instead of cutting it off.
 */
@Composable
fun LanguageScreen(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.appColors
    var query by remember { mutableStateOf("") }
    val matches = remember(query) { AppLanguage.entries.filter { it.matches(query) } }
    // A plain surface sits under the status bar, so the icons follow the theme.
    SystemBarIcons(lightStatusBarIcons = colors.isDark)


    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            if (onBack != null) {
                // Reached from Profile, where the screen is a settings page rather than a step in
                // first-run setup: a back bar replaces the oversized welcome header.
                SettingsTopBar(onBack = onBack)
            } else {
                LanguageHeader(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 28.dp)
                )
            }
            SearchField(
                query = query,
                onQueryChange = { query = it },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                // Clears the floating bottom bar, which itself clears the navigation bar.
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 16.dp,
                    bottom = 150.dp + AppInsets.bottomWithKeyboard,
                ),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                items(matches, key = { it.name }) { language ->
                    LanguageCard(
                        language = language,
                        selected = language == selected,
                        onClick = { onSelect(language) },

                    )
                }
                if (matches.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.language_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = Dimens.spaceXxl),
                        )
                    }
                }
            }
        }

        BottomBar(
            selected = selected,
            onContinue = onContinue,
            ctaLabel = stringResource(
                if (onBack != null) R.string.language_apply else R.string.language_continue
            ),
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/** The settings-page chrome: a back affordance, a centred title and the same explanatory line. */
@Composable
private fun SettingsTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = Dimens.spaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppShapes.Pill)
                    .background(colors.accentTint)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.ChevronLeft,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = colors.accentText,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = stringResource(R.string.language_settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            // Balances the back button so the title stays optically centred.
            Spacer(modifier = Modifier.size(40.dp))
        }
        Text(
            text = stringResource(R.string.language_settings_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = Dimens.spaceXxs),
        )
    }
}

@Composable
private fun LanguageHeader(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(17.dp))
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Globe,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = stringResource(R.string.language_title),
            style = MaterialTheme.typography.displaySmall,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = stringResource(R.string.language_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 21.sp),
            color = colors.textSecondary,
            modifier = Modifier.padding(top = Dimens.spaceSm),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(FieldShape)
            .background(colors.surface)
            .border(1.dp, colors.fieldBorder, FieldShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = AppIcons.Search,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(19.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.language_search_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textTertiary,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textPrimary),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Selecting a card animates its fill, border and text rather than snapping between two states. */
@Composable
private fun LanguageCard(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val background = if (selected) colors.accent else colors.surface
    val border = if (selected) colors.accent else colors.fieldBorder
    val title = if (selected) colors.onAccent else colors.textPrimary
    val subtitle = if (selected) Color.White.copy(alpha = 0.82f) else colors.textTertiary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(CardShape)
            .background(background)
            .border(1.dp, border, CardShape)
            .clickable(onClick = onClick)
            .padding(start = 18.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.titleLarge,
                color = title,
                maxLines = 1,
            )
            Text(
                text = if (language.isDeviceDefault) {
                    stringResource(R.string.language_device_default)
                } else {
                    stringResource(language.englishNameRes)
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = subtitle,
                maxLines = 1,
            )
        }
        SelectionIndicator(selected = selected)
    }
}

@Composable
private fun SelectionIndicator(selected: Boolean) {
    val colors = MaterialTheme.appColors
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        if (!selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(AppShapes.Pill)
                    .border(2.dp, colors.radioOff, AppShapes.Pill)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(AppShapes.Pill)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    selected: AppLanguage,
    onContinue: () -> Unit,
    ctaLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to colors.pageTint.copy(alpha = 0f),
                    0.42f to colors.pageTint,
                )
            )
            .bottomSafePadding()
            .padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Spark,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = buildAnnotatedString {
                    append(stringResource(R.string.language_answers_in))
                    append(" ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary)) {
                        append(stringResource(selected.englishNameRes))
                    }
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.textSecondary,
                modifier = Modifier.padding(start = Dimens.spaceXs),
            )
        }
        PrimaryCta(text = ctaLabel, onClick = onContinue)
    }
}

/** Matches the native spelling as well as the English one, so "Spanish" and "Español" both work. */
private fun AppLanguage.matches(query: String): Boolean {
    if (query.isBlank()) return true
    return nativeName.contains(query, ignoreCase = true) || name.contains(query, ignoreCase = true)
}

@ScreenPreviews
@Composable
private fun LanguageScreenPreview() {
    AITutorTheme {
        LanguageScreen(selected = AppLanguage.English, onSelect = {}, onContinue = {})
    }
}

@ScreenPreviews
@Composable
private fun LanguageSettingsScreenPreview() {
    AITutorTheme {
        LanguageScreen(
            selected = AppLanguage.English,
            onSelect = {},
            onContinue = {},
            onBack = {},
        )
    }
}
