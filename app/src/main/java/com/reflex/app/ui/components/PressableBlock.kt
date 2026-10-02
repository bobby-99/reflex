package com.reflex.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reflex.app.ui.theme.ReflexTokens
import kotlinx.coroutines.delay

/**
 * Modern fluid press-state modifier providing subtle scale and alpha feedback on tap.
 * Replaces old brutalist hard-offset drop shadows with a clean, responsive tactile feel.
 */
fun Modifier.pressableBlock(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shadowOffset: Dp = 0.dp, // Maintained for backward compatibility; offset shadow eliminated
    shadowColor: Color = Color.Transparent
): Modifier = composed {
    if (onClick == null || !enabled) return@composed this

    var isPressed by remember { mutableStateOf(false) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            isPressed = false
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) ReflexTokens.PressedScale else 1f,
        animationSpec = tween(
            durationMillis = ReflexTokens.AnimDurationFast,
            easing = FastOutSlowInEasing
        ),
        label = "PressScale"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = tween(
            durationMillis = ReflexTokens.AnimDurationFast,
            easing = FastOutSlowInEasing
        ),
        label = "PressAlpha"
    )

    val currentOnClick by androidx.compose.runtime.rememberUpdatedState(onClick)
    val currentEnabled by androidx.compose.runtime.rememberUpdatedState(enabled)

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
            alpha = animatedAlpha
        }
        .pointerInput(Unit) {
            if (!currentEnabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        awaitRelease()
                    } finally {
                        delay(60)
                        isPressed = false
                    }
                },
                onTap = {
                    currentOnClick?.invoke()
                }
            )
        }
}

/**
 * Composable wrapper for pressable content.
 */
@Composable
fun PressableBlock(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    shadowOffset: Dp = 0.dp,
    shadowColor: Color? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.pressableBlock(
            onClick = onClick,
            enabled = enabled,
            shadowOffset = shadowOffset,
            shadowColor = shadowColor ?: Color.Transparent
        ),
        propagateMinConstraints = true
    ) {
        content()
    }
}
