package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.cardShadowColor

/**
 * Standard Reflex Card Container.
 * Soft 16.dp rounded corners, subtle hairline border, dark surface fill, soft light-mode shadow, and fluid press response.
 */
@Composable
fun ReflexCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = ReflexTokens.ShapeCard,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    contentPadding: PaddingValues = PaddingValues(ReflexTokens.SpaceLg),
    content: @Composable () -> Unit
) {
    val shadowColor = MaterialTheme.colorScheme.cardShadowColor
    val hasShadow = shadowColor.alpha > 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.pressableBlock(onClick = onClick)
                else Modifier
            )
            .then(
                if (hasShadow) {
                    Modifier.shadow(
                        elevation = 2.dp,
                        shape = shape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                } else Modifier
            )
            .border(BorderStroke(ReflexTokens.BorderHairline, borderColor), shape = shape)
            .clip(shape)
            .background(containerColor)
            .padding(contentPadding)
    ) {
        content()
    }
}
