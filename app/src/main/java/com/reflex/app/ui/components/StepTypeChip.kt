package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.reflex.app.data.StepType
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens

/**
 * Step Type Chip Component.
 * Soft 8.dp rounded chip with hairline borders and copper selection highlight.
 */
@Composable
fun StepTypeChip(
    stepType: StepType,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = ReflexTokens.ShapeChip

    val label = when (stepType) {
        StepType.TIMED -> "Timed"
        StepType.CHECK_OFF -> "Check-off"
        StepType.REPEAT_COUNT -> "Sets"
    }

    val (backgroundColor, textColor, borderColor) = if (isSelected) {
        Triple(CopperPrimary, ActionPillOnWhite, CopperPrimary)
    } else {
        Triple(MaterialTheme.colorScheme.secondaryContainer, onSurfaceVariant, outline)
    }

    PressableBlock(
        modifier = modifier,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .height(ReflexTokens.ChipHeight)
                .border(
                    width = ReflexTokens.BorderHairline,
                    color = borderColor,
                    shape = shape
                )
                .clip(shape)
                .background(backgroundColor)
                .padding(
                    horizontal = ReflexTokens.SpaceMd,
                    vertical = ReflexTokens.SpaceXs
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}
