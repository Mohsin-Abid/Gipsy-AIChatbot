package com.aitutor.chatbot.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.OverlineStyle
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.appColors

private val FieldRadius = 18.dp
private val FieldShape = RoundedCornerShape(FieldRadius)

/** How far the focus ring reaches beyond the field's edge. */
private val FocusRingWidth = 4.dp

/**
 * Step one. The preview card underneath updates as the name is typed, which turns a plain text
 * field into a demonstration of what the name is actually for.
 */
@Composable
fun SetupNameScreen(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    SetupScaffold(
        step = 1,
        title = stringResource(R.string.setup_name_title),
        subtitle = stringResource(R.string.setup_name_subtitle),
        ctaText = stringResource(R.string.setup_continue),
        onBack = onBack,
        onSkip = onSkip,
        onContinue = onContinue,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(top = Dimens.spaceXxs),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            FieldLabel(text = stringResource(R.string.setup_name_label))
            NameField(name = name, onNameChange = onNameChange)
            Text(
                text = stringResource(R.string.setup_name_helper),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.textTertiary,
            )
        }

        GreetingPreview(name = name, modifier = Modifier.padding(top = 28.dp))
    }
}

@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit) {
    val colors = MaterialTheme.appColors
    val focusManager = LocalFocusManager.current
    // Real focus, not "has some text": the ring is a focus indicator, so it has to show the moment
    // the field is tapped and go when it is left — and an empty field was never able to show it.
    var focused by remember { mutableStateOf(false) }
    val border = if (focused) colors.accent else colors.inputBorder
    val ring = colors.accentTint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The design's ring is a box-shadow: outside the field, taking no room. A `border` cannot
            // do that — it is drawn inside the bounds, where the field's own background then paints
            // over it, which is why the ring was never visible. So it is drawn behind the field,
            // larger than it, and left to show around the edge.
            .drawBehind {
                if (focused) {
                    val grow = FocusRingWidth.toPx()
                    drawRoundRect(
                        color = ring,
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2, size.height + grow * 2),
                        cornerRadius = CornerRadius(FieldRadius.toPx() + grow),
                    )
                }
            }
            .clip(FieldShape)
            .background(colors.surface)
            .border(2.dp, border, FieldShape)
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Icon(
            imageVector = AppIcons.Person,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(20.dp),
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (name.isEmpty()) {
                Text(
                    text = stringResource(R.string.setup_name_hint),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textTertiary,
                )
            }
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                // Done, because the keyboard now covers the Continue button while typing and this is
                // how it is put away: the key clears focus, which closes the keyboard.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                ),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
    }
}

/** What the dashboard will greet them with, shown before they ever reach it. */
@Composable
private fun GreetingPreview(name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val display = name.ifBlank { stringResource(R.string.setup_name_greeting_fallback) }
    val initial = display.trim().firstOrNull()?.uppercase() ?: "?"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.CardLarge)
            .background(colors.hero)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(Float.POSITIVE_INFINITY, 0f),
                    radius = 520f,
                )
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Text(
            text = stringResource(R.string.setup_name_preview).uppercase(),
            style = OverlineStyle,
            color = Color.White.copy(alpha = 0.75f),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(AppShapes.Pill)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = colors.accent,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = stringResource(R.string.setup_name_greeting),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                )
                Text(
                    text = display,
                    style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, lineHeight = 24.sp),
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = stringResource(R.string.setup_name_bubble, display),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                .background(Color.White.copy(alpha = 0.14f))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 6.dp))
                .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
        )
    }
}

@ScreenPreviews
@Composable
private fun SetupNameScreenPreview() {
    AITutorTheme {
        SetupNameScreen(name = "Alex", onNameChange = {}, onBack = {}, onSkip = {}, onContinue = {})
    }
}
