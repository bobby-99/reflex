package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reflex.app.ui.theme.ReflexTokens

/**
 * Standardized top-bar icon button across all Reflex screens:
 * - 48dp outer touch-target boundary
 * - 44dp circular visual surface
 * - 20dp pixel-faithful vector icon via SettingsIcon
 * - Accessible content description and button role semantics
 */
@Composable
fun TopBarIconButton(
    iconName: String,
    contentDescriptionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    iconSize: Dp = 20.dp,
    isActive: Boolean = false,
    activeContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    activeTint: Color = MaterialTheme.colorScheme.primary
) {
    val bg = if (isActive) activeContainerColor else containerColor
    val fg = if (isActive) activeTint else tint

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                onClick = onClick,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .semantics {
                role = Role.Button
                contentDescription = contentDescriptionText
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            shape = CircleShape,
            color = bg,
            border = BorderStroke(ReflexTokens.BorderHairline, borderColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                SettingsIcon(
                    name = iconName,
                    tint = fg,
                    size = iconSize
                )
            }
        }
    }
}

/**
 * Universal 44dp circular Settings button (48dp touch target) used across all tabs
 * with Settings always rendered with the "gear" vector icon.
 */
@Composable
fun SettingsTopBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescriptionText: String = "Settings"
) {
    TopBarIconButton(
        iconName = "gear",
        contentDescriptionText = contentDescriptionText,
        onClick = onClick,
        modifier = modifier
    )
}
