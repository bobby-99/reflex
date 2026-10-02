package com.reflex.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.OnCopper
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.tertiaryText

/**
 * Custom 52x30dp switch pill replicating reflex-focus.html:
 * Off = SurfaceInput track with tertiary thumb.
 * On = CopperPrimary track with OnCopper thumb.
 * 22dp thumb inset 4dp, animated over 200ms.
 */
@Composable
fun ReflexSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackBg by animateColorAsState(
        targetValue = if (checked) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer,
        animationSpec = tween(ReflexTokens.AnimDurationNormal),
        label = "SwitchTrackBg"
    )
    val thumbBg by animateColorAsState(
        targetValue = if (checked) OnCopper else MaterialTheme.colorScheme.tertiaryText,
        animationSpec = tween(ReflexTokens.AnimDurationNormal),
        label = "SwitchThumbBg"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 26.dp else 4.dp,
        animationSpec = tween(ReflexTokens.AnimDurationNormal),
        label = "SwitchThumbOffset"
    )

    Box(
        modifier = modifier
            .width(ReflexTokens.FocusSwitchWidth)
            .height(ReflexTokens.FocusSwitchHeight)
            .clip(CircleShape)
            .background(trackBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) {
                onCheckedChange(!checked)
            }
            .semantics { role = Role.Switch },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(ReflexTokens.FocusSwitchThumbSize)
                .clip(CircleShape)
                .background(thumbBg)
        )
    }
}
