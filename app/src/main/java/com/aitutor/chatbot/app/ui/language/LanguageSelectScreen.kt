package com.aitutor.chatbot.app.ui.language

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.core.ext.elevatedCardStyle
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.ui.components.heroBrush
import com.aitutor.chatbot.app.ui.components.heroSheen
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LanguageSelectRoute(
    onDone: () -> Unit,
    viewModel: LanguageViewModel = koinViewModel(),
) {
    val selected by viewModel.selectedLanguage.collectAsLifecycleAwareState()

    LanguageSelectScreen(
        selected = selected,
        onLanguageClick = { viewModel.onLanguageSelected(it, onDone) }
    )
}

/**
 * Screen-local presentation for each locale: the glyph on its card, a caption that adds information
 * instead of repeating the label, and the handful of interface strings the preview renders.
 *
 * These live here, not in `strings.xml`, because the preview has to show all five locales at once —
 * resource lookup can only ever give the currently-active one.
 */
private data class LocalePreview(
    val glyph: String,
    val caption: String,
    val rtl: Boolean,
    val greeting: String,
    val askPlaceholder: String,
    val firstTool: String,
    val secondTool: String,
    val navHome: String,
    val navChats: String,
    val navSettings: String,
)

private val AppLanguage.preview: LocalePreview
    get() = when (this) {
        AppLanguage.English -> LocalePreview(
            glyph = "Aa",
            caption = "Default",
            rtl = false,
            greeting = "Good evening",
            askPlaceholder = "Ask anything…",
            firstTool = "Homework Help",
            secondTool = "Essay Writer",
            navHome = "Home",
            navChats = "Chats",
            navSettings = "Settings",
        )
        AppLanguage.Urdu -> LocalePreview(
            glyph = "ا",
            caption = "Urdu · right-to-left",
            rtl = true,
            greeting = "خوش آمدید",
            askPlaceholder = "کچھ بھی پوچھیں…",
            firstTool = "ہوم ورک مدد",
            secondTool = "مضمون نویسی",
            navHome = "ہوم",
            navChats = "چیٹس",
            navSettings = "ترتیبات",
        )
        AppLanguage.RomanUrdu -> LocalePreview(
            glyph = "Ur",
            caption = "Urdu in Latin script",
            rtl = false,
            greeting = "Khush aamdeed",
            askPlaceholder = "Kuch bhi poochein…",
            firstTool = "Homework Madad",
            secondTool = "Essay Likhein",
            navHome = "Home",
            navChats = "Chats",
            navSettings = "Settings",
        )
        AppLanguage.Arabic -> LocalePreview(
            glyph = "ع",
            caption = "Arabic · right-to-left",
            rtl = true,
            greeting = "أهلًا بك",
            askPlaceholder = "اسأل أي شيء…",
            firstTool = "مساعدة الواجبات",
            secondTool = "كتابة المقالات",
            navHome = "الرئيسية",
            navChats = "المحادثات",
            navSettings = "الإعدادات",
        )
        AppLanguage.Hindi -> LocalePreview(
            glyph = "अ",
            caption = "Hindi",
            rtl = false,
            greeting = "आपका स्वागत है",
            askPlaceholder = "कुछ भी पूछें…",
            firstTool = "होमवर्क मदद",
            secondTool = "निबंध लेखन",
            navHome = "होम",
            navChats = "चैट",
            navSettings = "सेटिंग्स",
        )
    }

/**
 * Sets the app's display locale. A locale picker is the least interesting screen in any app — five
 * near-identical rows — so the choice is made visible instead: the hero holds a miniature of the
 * app's own interface, rendered in the highlighted locale and mirrored for right-to-left scripts.
 * Nothing commits until Continue, otherwise the preview would never be seen.
 */
@Composable
fun LanguageSelectScreen(
    selected: AppLanguage?,
    onLanguageClick: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    var choice by remember { mutableStateOf(selected) }
    LaunchedEffect(selected) { if (choice == null) choice = selected }
    val active = choice ?: AppLanguage.English

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = spacing.xl, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            item {
                Reveal(modifier = Modifier.fillMaxWidth()) { LanguageHeader() }
            }
            item {
                Reveal(delayMillis = 110, modifier = Modifier.fillMaxWidth()) {
                    InterfacePreview(
                        language = active,
                        modifier = Modifier.padding(top = spacing.xl, bottom = spacing.lg)
                    )
                }
            }
            item {
                Text(
                    text = "AVAILABLE LANGUAGES",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = spacing.xs),
                )
            }
            items(AppLanguage.entries, key = { it.name }) { language ->
                LanguageCard(
                    language = language,
                    selected = language == active,
                    onClick = { choice = language },
                )
            }
            item { Spacer(modifier = Modifier.height(spacing.sm)) }
        }

        ContinueBar(
            language = active,
            onContinue = { onLanguageClick(active) },
            modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.lg),
        )
    }
}

@Composable
private fun LanguageHeader(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .elevatedCardStyle(RoundedCornerShape(18.dp), 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(heroBrush())
                .heroSheen(rings = false),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Translate,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = "APP LANGUAGE",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.8.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = spacing.xl),
        )
        Text(
            text = "Make the app yours",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = spacing.sm),
        )
        Text(
            text = "Menus, buttons and every screen will be shown in the language you pick.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.md),
        )
    }
}

/**
 * A miniature of the app's own interface — greeting, composer, tools, tab bar — redrawn in the
 * highlighted locale. For Arabic and Urdu the whole mock mirrors, because that, far more than the
 * script itself, is what "the app in my language" actually looks like.
 */
@Composable
private fun InterfacePreview(language: AppLanguage, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .elevatedCardStyle(shape = AppShapes.CardLarge, elevation = 16.dp)
            .clip(AppShapes.CardLarge)
            .background(heroBrush())
            .heroSheen(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(modifier = Modifier.weight(1f))
                AnimatedVisibility(
                    visible = language.preview.rtl,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(160)),
                ) {
                    Text(
                        text = "RTL",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                        color = Color.White,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                            .border(1.dp, Color.White.copy(alpha = 0.20f), CircleShape)
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(spacing.md))

            AnimatedContent(
                targetState = language,
                transitionSpec = {
                    (fadeIn(tween(280)) + scaleIn(tween(280), initialScale = 0.97f))
                        .togetherWith(fadeOut(tween(160)))
                },
                label = "interfacePreview",
            ) { target ->
                val strings = target.preview
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (strings.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    MockApp(strings = strings)
                }
            }
        }
    }
}

@Composable
private fun MockApp(strings: LocalePreview) {
    val spacing = MaterialTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings.greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        // The composer pill — the one element on Home everybody recognizes at a glance.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = spacing.md, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = strings.askPlaceholder,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = spacing.sm).weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            MockTool(Icons.AutoMirrored.Outlined.MenuBook, strings.firstTool, Modifier.weight(1f))
            MockTool(Icons.Outlined.EditNote, strings.secondTool, Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(spacing.md))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            MockNavItem(Icons.Outlined.Home, strings.navHome, active = true)
            MockNavItem(Icons.AutoMirrored.Outlined.Chat, strings.navChats, active = false)
            MockNavItem(Icons.Outlined.Settings, strings.navSettings, active = false)
        }
    }
}

@Composable
private fun MockTool(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

@Composable
private fun MockNavItem(icon: ImageVector, label: String, active: Boolean) {
    val tint = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun LanguageCard(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val strings = language.preview
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = tween(220),
        label = "languageBorder",
    )
    val fill by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.07f)
        } else {
            MaterialTheme.colorScheme.background
        },
        animationSpec = tween(220),
        label = "languageFill",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.Card,
        color = fill,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            GlyphTile(glyph = strings.glyph, selected = selected)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = strings.caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn(tween(220)) + fadeIn(tween(220)),
                exit = scaleOut(tween(160)) + fadeOut(tween(160)),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(heroBrush()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

/**
 * The card's leading mark, set in the locale's own script. Five rows of Latin text look identical at
 * a glance; five different glyphs are scannable instantly — and they tell a student in Urdu or Hindi
 * that their script is a first-class citizen here, before they've read a word.
 */
@Composable
private fun GlyphTile(glyph: String, selected: Boolean) {
    val shape = RoundedCornerShape(14.dp)
    val idleFill by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0f else 0.08f),
        animationSpec = tween(220),
        label = "glyphIdleFill",
    )
    val brush = heroBrush()
    val gradientAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(220),
        label = "glyphGradient",
    )
    val glyphColor by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.primary,
        animationSpec = tween(220),
        label = "glyphColor",
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(shape)
            .background(SolidColor(idleFill)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer { alpha = gradientAlpha }
                .background(brush)
        )
        Text(
            text = glyph,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = glyphColor,
        )
    }
}

/** Naming the language in the button turns a generic Continue into a confirmation of the choice. */
@Composable
private fun ContinueBar(
    language: AppLanguage,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .elevatedCardStyle(CircleShape, 12.dp)
                .clip(CircleShape)
                .background(heroBrush())
                .heroSheen(rings = false)
                .clickable(onClick = onContinue),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = language,
                transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(140))) },
                label = "continueLabel",
            ) { target ->
                Text(
                    text = "Continue in ${target.englishLabel}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
        Text(
            text = "You can change this anytime in Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = spacing.md),
        )
    }
}

/** Fades and lifts its content in on first composition — same entrance the onboarding pages use. */
@Composable
private fun Reveal(
    delayMillis: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong().milliseconds)
        progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 24.dp.toPx()
        }
    ) {
        content()
    }
}

@ThemePreviews
@Composable
private fun LanguageSelectScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LanguageSelectScreen(selected = AppLanguage.Urdu, onLanguageClick = {})
        }
    }
}
