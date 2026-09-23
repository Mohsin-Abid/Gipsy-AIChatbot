package com.aitutor.chatbot.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.home.ProBadge
import com.aitutor.chatbot.app.ui.theme.PremiumGold
import com.aitutor.chatbot.app.ui.theme.spacing

enum class AttachmentOption { Scan, Gallery, Pdf, Document }

/**
 * What a student can hand the tutor. Camera and gallery run through on-device OCR today; PDF and
 * .docx parsing are the next phase, so they're marked rather than silently broken.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    isPremium: Boolean,
    onOptionSelected: (AttachmentOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MaterialTheme.spacing

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = spacing.xxl)) {
            Text(
                text = "Add to your question",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.md)
            )

            AttachmentRow(
                icon = Icons.Outlined.DocumentScanner,
                title = "Scan homework",
                detail = "Point your camera at a question or worked page",
                locked = !isPremium,
                onClick = { onOptionSelected(AttachmentOption.Scan) },
            )
            AttachmentRow(
                icon = Icons.Outlined.Image,
                title = "Photo library",
                detail = "Pick a picture of notes, a book page or a worksheet",
                locked = !isPremium,
                onClick = { onOptionSelected(AttachmentOption.Gallery) },
            )
            AttachmentRow(
                icon = Icons.Outlined.PictureAsPdf,
                title = "PDF document",
                detail = "Coming in the next update",
                locked = !isPremium,
                enabled = false,
                onClick = { onOptionSelected(AttachmentOption.Pdf) },
            )
            AttachmentRow(
                icon = Icons.Outlined.Description,
                title = "Word document",
                detail = "Coming in the next update",
                locked = !isPremium,
                enabled = false,
                onClick = { onOptionSelected(AttachmentOption.Document) },
            )

            Text(
                text = "Scans are read on your device — your pages are never uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = spacing.xl, vertical = spacing.md)
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    icon: ImageVector,
    title: String,
    detail: String,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val spacing = MaterialTheme.spacing
    val contentAlpha = if (enabled) 1f else 0.55f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.xl, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f * contentAlpha),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (locked && enabled) ProBadge()
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (locked && enabled) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = PremiumGold,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
