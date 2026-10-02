package com.reflex.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.R
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.tertiaryText
import kotlinx.coroutines.delay

@Composable
fun FocusQuote(active: Boolean, modifier: Modifier = Modifier) {
    val quotes = stringArrayResource(R.array.focus_quotes)
    var current by rememberSaveable { mutableIntStateOf(-1) }
    val bag = remember { ArrayDeque<Int>() }
    val ran = remember { longArrayOf(0L) } // running time since the last change

    fun next(): Int {
        if (bag.isEmpty()) {
            val order = quotes.indices.shuffled().toMutableList()
            if (order.size > 1 && order.first() == current) {
                order.add(order.removeAt(0))
            }
            bag.addAll(order)
        }
        return if (bag.isNotEmpty()) bag.removeFirst() else 0
    }

    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        if (current < 0) current = next()
        while (true) {
            delay(1_000)
            ran[0] += 1_000
            if (ran[0] >= 45_000) {
                ran[0] = 0
                current = next()
            }
        }
    }

    AnimatedContent(
        targetState = current,
        modifier = modifier.heightIn(min = 36.dp),
        transitionSpec = { fadeIn(tween(600, delayMillis = 600)) togetherWith fadeOut(tween(600)) },
        label = "quote"
    ) { i ->
        if (i in quotes.indices) {
            Text(
                text = quotes[i],
                textAlign = TextAlign.Center,
                fontFamily = Lora,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.tertiaryText,
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clearAndSetSemantics {}
            )
        }
    }
}
