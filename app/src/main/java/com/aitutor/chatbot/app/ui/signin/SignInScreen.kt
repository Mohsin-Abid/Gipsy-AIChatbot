package com.aitutor.chatbot.app.ui.signin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.components.BrandRow
import com.aitutor.chatbot.app.ui.components.LogoTile
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppInsets
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.GoogleBrand
import com.aitutor.chatbot.app.ui.theme.OnHero
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

/**
 * The account screen, between onboarding and profile setup.
 *
 * Stateless by design: it reports which button was pressed and nothing else. Signing in is the
 * caller's job, so this file has no notion of a token, an account or a network — which also means
 * the whole screen renders in a `@Preview` exactly as it does on a device.
 */
@Composable
fun SignInScreen(
    onContinueWithGoogle: () -> Unit,
    onSkip: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
    inProgress: Boolean = false,
) {
    val colors = MaterialTheme.appColors

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        // The accent hero runs under the status bar.
        SystemBarIcons(lightStatusBarIcons = true)

        SignInHero()

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.screenPadding)
                .padding(top = 28.dp, bottom = 26.dp),
        ) {
            Text(
                text = stringResource(R.string.signin_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.signin_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            GoogleSignInButton(onClick = onContinueWithGoogle, enabled = !inProgress)

            Box(
                modifier = Modifier
                    .padding(top = Dimens.spaceSm)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(AppShapes.Pill)
                    .clickable(enabled = !inProgress, onClick = onSkip),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.signin_skip),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.accentText,
                    textAlign = TextAlign.Center,
                )
            }

            LegalFootnote(
                onOpenTerms = onOpenTerms,
                onOpenPrivacy = onOpenPrivacy,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

// ---- Hero ----

/**
 * The accent panel: a glow and three hairline rings, all centred on the app mark.
 *
 * The rings are drawn rather than laid out as views because the widest one is 420dp across on a
 * 390dp screen — it has to bleed past both edges and be cut off by the panel's own corner radius.
 */
@Composable
private fun SignInHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Taller by the status bar so the accent reaches the display's top edge, rather than
            // the artwork inside it losing that much room.
            .height(HeroHeight + AppInsets.top)
            .clip(AppShapes.HeroPanel)
            .background(MaterialTheme.appColors.hero),
    ) {
        // Everything inside sits below the clock, which keeps the design's 390 x 476 coordinates.
        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centre = Offset(size.width / 2f, MarkCentreY.toPx())
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                        center = centre,
                        radius = GlowRadius.toPx(),
                    ),
                    radius = GlowRadius.toPx(),
                    center = centre,
                )
                val hairline = Stroke(width = 1.dp.toPx())
                drawCircle(
                    color = OnHero.RingFaint,
                    radius = 210.dp.toPx(),
                    center = centre,
                    style = hairline,
                )
                drawCircle(
                    color = OnHero.Fill,
                    radius = 150.dp.toPx(),
                    center = centre,
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(DashOn.toPx(), DashOff.toPx())
                        ),
                    ),
                )
                drawCircle(
                    color = OnHero.Stroke,
                    radius = 95.dp.toPx(),
                    center = centre,
                    style = hairline,
                )
            }

            BrandRow(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(Dimens.screenPadding)
            )

            AppMark(modifier = Modifier.align(Alignment.TopCenter).offset(y = MarkCentreY - MarkHalo / 2))

            // Absolute offsets, but each pill is anchored to the edge it sits nearest, so a screen
            // wider than the design's 390dp pushes them apart rather than stranding them inland.
            BenefitPill(
                icon = AppIcons.CloudCheck,
                text = stringResource(R.string.signin_benefit_sync),
                modifier = Modifier.align(Alignment.TopStart).offset(x = 20.dp, y = 104.dp),
            )
            BenefitPill(
                icon = AppIcons.ShieldCheck,
                text = stringResource(R.string.signin_benefit_backup),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-18).dp, y = 322.dp),
            )
            BenefitPill(
                icon = AppIcons.Devices,
                text = stringResource(R.string.signin_benefit_devices),
                modifier = Modifier.align(Alignment.TopStart).offset(x = 26.dp, y = 392.dp),
            )
        }
    }
}

/**
 * The brand mark on its tile, inside a translucent halo.
 *
 * The halo is a larger rounded box behind the tile rather than a border on it: the design puts the
 * ring *outside* the 96dp mark, and a border would eat into it instead.
 */
@Composable
private fun AppMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .size(MarkHalo)
            .clip(MarkHaloShape)
            .background(OnHero.RingStrong),
        contentAlignment = Alignment.Center,
    ) {
        LogoTile(
            background = colors.surface,
            iconTint = colors.accentText,
            size = MarkSize,
            shape = MarkShape,
            iconSize = 50.dp,
            modifier = Modifier.shadow(20.dp, MarkShape, ambientColor = ShadowTint, spotColor = ShadowTint),
        )
    }
}

/** One of the three claims floating over the hero. */
@Composable
private fun BenefitPill(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(AppShapes.Pill)
            .background(OnHero.FillSoft)
            .border(1.dp, OnHero.StrokeMedium, AppShapes.Pill)
            .padding(start = Dimens.spaceXs, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(AppShapes.Pill)
                .background(OnHero.Text),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = OnHero.Text,
        )
    }
}

// ---- Actions ----

/**
 * The Google button, in Google's colours rather than the app's.
 *
 * Fill, stroke, label and mark are all fixed by Google's sign-in branding guidelines, so they come
 * from [GoogleBrand] instead of the theme — this is the one control in the app that must *not*
 * follow a palette change. See the note on [GoogleBrand].
 */
@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val isDark = MaterialTheme.appColors.isDark
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ctaHeight)
            .shadow(1.dp, AppShapes.Button, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(AppShapes.Button)
            .background(GoogleBrand.fill(isDark))
            .border(1.dp, GoogleBrand.stroke(isDark), AppShapes.Button)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Dimens.spaceXxl),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google_g),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(Dimens.spaceMd))
        Text(
            text = stringResource(R.string.signin_google),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = GoogleBrand.label(isDark),
        )
    }
}

/**
 * The terms line, with both policies tappable.
 *
 * Built as an annotated string over the formatted sentence rather than as three `Text`s in a row:
 * the link positions have to come out of the translated string, since a language that reorders the
 * clause would otherwise underline the wrong words.
 */
@Composable
private fun LegalFootnote(
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val terms = stringResource(R.string.signin_terms)
    val privacy = stringResource(R.string.signin_privacy)
    val sentence = stringResource(R.string.signin_legal, terms, privacy)
    val later = stringResource(R.string.signin_legal_later)

    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = colors.accentText, fontWeight = FontWeight.SemiBold)
    )

    val text = buildAnnotatedString {
        append(sentence)
        sentence.indexOf(terms).takeIf { it >= 0 }?.let { start ->
            addLink(
                LinkAnnotation.Clickable("terms", linkStyles) { onOpenTerms() },
                start,
                start + terms.length,
            )
        }
        sentence.indexOf(privacy).takeIf { it >= 0 }?.let { start ->
            addLink(
                LinkAnnotation.Clickable("privacy", linkStyles) { onOpenPrivacy() },
                start,
                start + privacy.length,
            )
        }
        append("\n")
        append(later)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Normal,
            lineHeight = 17.sp,
        ),
        color = colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

// ---- Geometry, from the design's 390 x 844 frame ----

private val HeroHeight = 476.dp

/** Where the mark, the rings and the glow all share a centre: 52% down the hero, as the design has it. */
private val MarkCentreY = 248.dp
private val MarkSize = 96.dp

/** The mark plus the 8dp translucent ring the design draws outside it. */
private val MarkHalo = 112.dp
private val MarkShape = RoundedCornerShape(30.dp)
private val MarkHaloShape = RoundedCornerShape(38.dp)

private val GlowRadius = 150.dp

/** An approximation of CSS `1px dashed` on a circle this size. */
private val DashOn = 4.dp
private val DashOff = 4.dp

private val ShadowTint = Color(0xFF080C28)

@ScreenPreviews
@Composable
private fun SignInScreenPreview() {
    AITutorTheme {
        SignInScreen(
            onContinueWithGoogle = {},
            onSkip = {},
            onOpenTerms = {},
            onOpenPrivacy = {},
        )
    }
}
