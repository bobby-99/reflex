package com.reflex.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.util.OpenMojiMapper

@Composable
fun OpenMojiImage(
    emoji: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    contentDescription: String? = null
) {
    if (emoji.isNullOrBlank()) return
    val context = LocalContext.current

    val resId = OpenMojiMapper.getDrawableResId(context, emoji)
    if (resId != 0) {
        Image(
            painter = painterResource(id = resId),
            contentDescription = contentDescription ?: emoji,
            modifier = modifier.size(size)
        )
    } else {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = (size.value * 0.75f).sp
            ),
            textAlign = TextAlign.Center,
            modifier = modifier
        )
    }
}
