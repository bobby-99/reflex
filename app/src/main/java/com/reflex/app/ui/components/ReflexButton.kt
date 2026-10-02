package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.ActionPillWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens

import com.reflex.app.ui.theme.outlineOutlined

enum class ReflexButtonVariant {
    PRIMARY,    // White pill action (GymMane-inspired clean high-contrast primary)
    SECONDARY,  // SurfaceInput container slightly darker than card with 1dp border
    OUTLINED,   // Transparent background with 28% black border (light) / hairline (dark)
    ACCENT,     // Warm copper pill action
    GHOST,      // Borderless transparent action
    DESTRUCTIVE // Red / error action
}

/**
 * Standard Reflex Button Component.
 * Pill-shaped, responsive press animation, clean typography, with support for white-pill primary actions.
 */
@Composable
fun ReflexButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: ReflexButtonVariant = ReflexButtonVariant.PRIMARY,
    shape: Shape = ReflexTokens.ShapeButton
) {
    val secondaryBg = MaterialTheme.colorScheme.secondaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val outlineOutlined = MaterialTheme.colorScheme.outlineOutlined
    val errorColor = MaterialTheme.colorScheme.error

    val containerBg = when (variant) {
        ReflexButtonVariant.PRIMARY -> if (enabled) ActionPillWhite else ActionPillWhite.copy(alpha = 0.4f)
        ReflexButtonVariant.SECONDARY -> if (enabled) secondaryBg else secondaryBg.copy(alpha = 0.5f)
        ReflexButtonVariant.OUTLINED -> Color.Transparent
        ReflexButtonVariant.ACCENT -> if (enabled) CopperPrimary else CopperPrimary.copy(alpha = 0.4f)
        ReflexButtonVariant.GHOST -> Color.Transparent
        ReflexButtonVariant.DESTRUCTIVE -> if (enabled) errorColor else errorColor.copy(alpha = 0.4f)
    }

    val contentColor = when (variant) {
        ReflexButtonVariant.PRIMARY -> if (enabled) ActionPillOnWhite else ActionPillOnWhite.copy(alpha = 0.5f)
        ReflexButtonVariant.SECONDARY -> if (enabled) onSurface else onSurface.copy(alpha = 0.4f)
        ReflexButtonVariant.OUTLINED -> if (enabled) onSurface else onSurface.copy(alpha = 0.4f)
        ReflexButtonVariant.ACCENT -> if (enabled) ActionPillOnWhite else ActionPillOnWhite.copy(alpha = 0.5f)
        ReflexButtonVariant.GHOST -> if (enabled) onSurface else onSurface.copy(alpha = 0.4f)
        ReflexButtonVariant.DESTRUCTIVE -> Color.White
    }

    val borderStroke = when (variant) {
        ReflexButtonVariant.SECONDARY -> BorderStroke(
            ReflexTokens.BorderThin,
            if (enabled) outline else outline.copy(alpha = 0.3f)
        )
        ReflexButtonVariant.OUTLINED -> BorderStroke(
            ReflexTokens.BorderThin,
            if (enabled) outlineOutlined else outlineOutlined.copy(alpha = 0.3f)
        )
        ReflexButtonVariant.GHOST -> null
        else -> null
    }

    Box(
        modifier = modifier
            .pressableBlock(
                onClick = if (enabled) onClick else null,
                enabled = enabled
            )
            .defaultMinSize(minWidth = ReflexTokens.ButtonMinWidth)
            .heightIn(min = ReflexTokens.ButtonHeight)
            .then(if (borderStroke != null) Modifier.border(borderStroke, shape) else Modifier)
            .clip(shape)
            .background(containerBg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(
                horizontal = ReflexTokens.SpaceXl,
                vertical = ReflexTokens.SpaceSm
            )
        )
    }
}
