package com.reflex.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OpenMojiIcon(
    emoji: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    OpenMojiImage(
        emoji = emoji,
        modifier = modifier,
        size = size
    )
}
