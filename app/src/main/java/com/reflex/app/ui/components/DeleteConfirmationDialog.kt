package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.reflex.app.ui.theme.ReflexTokens

/**
 * Clean confirmation dialog with 20.dp rounded corners, hairline border, and pill action buttons.
 */
@Composable
fun DeleteConfirmationDialog(
    taskTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outline
    val shape = ReflexTokens.ShapeDialog

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(ReflexTokens.BorderHairline, borderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ReflexTokens.SpaceXl)
            ) {
                Text(
                    text = "Delete task?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                Text(
                    text = "Are you sure you want to delete \"$taskTitle\"? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)
                ) {
                    ReflexButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        variant = ReflexButtonVariant.SECONDARY
                    )

                    ReflexButton(
                        text = "Delete",
                        onClick = {
                            onConfirm()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        variant = ReflexButtonVariant.DESTRUCTIVE
                    )
                }
            }
        }
    }
}
