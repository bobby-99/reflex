package com.reflex.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pixel-faithful vector icons reproducing the exact SVG `IC` map from reflex-settings.html.
 * All paths rendered stroke-only at 1.8 stroke width with round caps and round joins.
 */
object SettingsIconPaths {
    val paths = mapOf(
        "back" to "M15 5l-7 7 7 7",
        "chev" to "M9 5l7 7-7 7",
        "search" to "M11 4.5a6.5 6.5 0 1 0 0 13a6.5 6.5 0 1 0 0 -13M16 16l4 4",
        "routines" to "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0 -16M12 8v4l3 2",
        "calendar" to "M7 5h10a3 3 0 0 1 3 3v9a3 3 0 0 1 -3 3h-10a3 3 0 0 1 -3 -3v-9a3 3 0 0 1 3 -3zM8 3v4M16 3v4M4 10h16",
        "tasks" to "M5 7l2 2 3-3M5 15l2 2 3-3M13 8h6M13 16h6",
        "habits" to "M12 3c1 3 5 5 5 10a5 5 0 0 1-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-3-1-6 1-9z",
        "focus" to "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0 -16M12 8.5a3.5 3.5 0 1 0 0 7a3.5 3.5 0 1 0 0 -7",
        "bell" to "M6 17V11a6 6 0 0 1 12 0v6l1.5 2h-15zM10 21h4",
        "shield" to "M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6zM9 12l2 2 4-4",
        "db" to "M12 3c3.86 0 7 1.34 7 3s-3.14 3-7 3s-7-1.34-7-3s3.14-3 7-3zM5 6v6c0 1.7 3.1 3 7 3s7-1.3 7-3V6M5 12v6c0 1.7 3.1 3 7 3s7-1.3 7-3v-6",
        "info" to "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0 -17M12 11v5M12 8v.01",
        "mic" to "M12 3h0a3 3 0 0 1 3 3v5a3 3 0 0 1 -3 3h0a3 3 0 0 1 -3 -3v-5a3 3 0 0 1 3 -3zM5 11a7 7 0 0 0 14 0M12 18v3",
        "tour" to "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0 -17M15.5 8.5l-2 5-5 2 2-5z",
        "phone" to "M9.5 3h5a2.5 2.5 0 0 1 2.5 2.5v13a2.5 2.5 0 0 1 -2.5 2.5h-5a2.5 2.5 0 0 1 -2.5 -2.5v-13a2.5 2.5 0 0 1 2.5 -2.5zM11 18h2",
        "moon" to "M20 14.5A8 8 0 0 1 9.5 4 8 8 0 1 0 20 14.5z",
        "sun" to "M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0 -8M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M18.4 5.6L17 7M7 17l-1.4 1.4",
        "alarm" to "M12 6a7 7 0 1 0 0 14a7 7 0 1 0 0 -14M12 9v4l2 2M5 4L3 6M19 4l2 2",
        "block" to "M12 3.5a8.5 8.5 0 1 0 0 17a8.5 8.5 0 1 0 0 -17M6 6l12 12",
        "trash" to "M5 7h14M9 7V4h6v3M7 7l1 13h8l1-13",
        "check" to "M5 12.5l4.5 4.5L19 7.5",
        "flame" to "M12 3c1 3 5 5 5 10a5 5 0 0 1-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-3-1-6 1-9z",
        "fire" to "M12 3c1 3 5 5 5 10a5 5 0 0 1-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-3-1-6 1-9z",
        "timer" to "M12 6a7 7 0 1 0 0 14a7 7 0 1 0 0 -14M12 9v4l2 2M9 3h6",
        "lock" to "M8 11V8a4 4 0 0 1 8 0v3M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2v-7a2 2 0 0 1 2 -2z",
        "bat" to "M3 8h16a3 3 0 0 1 3 3v2a3 3 0 0 1 -3 3H3a3 3 0 0 1 -3 -3v-2a3 3 0 0 1 3 -3zM22 11v3M8 12.5h4",
        "line-graph" to "M4 4v16h16M8 15l3-4 3 3 5-7",
        "runner" to "M13 3.5a1.8 1.8 0 1 0 0 -3.6a1.8 1.8 0 0 0 0 3.6z M7 21l3.5 -4l1.5 -2l-1.5 -3.5l3.5 -2.5l3 2.5l3.5 -1 M9.5 12.5l-2.5 3.5l-3.5 -1 M13.5 9.5l1.5 3.5l4 2",
        "camera" to "M4 7h4l2 -3h4l2 3h4a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2H4a2 2 0 0 1 -2 -2V9a2 2 0 0 1 2 -2z M12 10a3 3 0 1 0 0 6a3 3 0 0 0 0 -6z",
        "edit" to "M11 4H4a2 2 0 0 0 -2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2 -2v-7 M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1l1 -4l9.5 -9.5z",
        "hist" to "M4 12a8 8 0 1 0 3 -6.2 M4 4v4h4 M12 8v4l3 2",
        "gear" to "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z M12 9a3 3 0 1 0 0 6a3 3 0 0 0 0 -6z",
        "book" to "M5 5h6a2 2 0 0 1 2 2v13a2 2 0 0 0 -2 -2H5z M19 5h-6 M19 5v13h-6",
        "gym" to "M3 10v4 M6 8v8 M18 8v8 M21 10v4 M6 12h12",
        "dumbbell" to "M3 10v4 M6 8v8 M18 8v8 M21 10v4 M6 12h12",
        "mail" to "M3 7a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V7z M3 7l9 6l9 -6",
        "feedback" to "M3 7a2 2 0 0 1 2 -2h14a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2H5a2 2 0 0 1 -2 -2V7z M3 7l9 6l9 -6",
        "play" to "M8 5l11 7l-11 7z",
        "pause" to "M8 5v14 M16 5v14",
        "prev" to "M6 5v14 M19 5l-9 7l9 7z",
        "next" to "M18 5v14 M5 5l9 7l-9 7z",
        "stop" to "M12 3a9 9 0 1 0 0 18a9 9 0 0 0 0 -18z M9 9h6v6H9z",
        "x" to "M6 6l12 12 M18 6L6 18",
        "close" to "M6 6l12 12 M18 6L6 18",
        "clock" to "M12 4a8 8 0 1 0 0 16a8 8 0 0 0 0 -16z M12 8v4l3 2",
        "chev-down" to "M6 10l6 6l6 -6",
        "thunder" to "M13 2.5l-7.5 9.5h6l-1.5 9.5 9-11.5h-6z",
        "bolt" to "M13 2.5l-7.5 9.5h6l-1.5 9.5 9-11.5h-6z"
    )

    private val parsedCache = mutableMapOf<String, Path>()

    fun getPath(name: String): Path? {
        return parsedCache.getOrPut(name) {
            val d = paths[name] ?: return null
            PathParser().parsePathString(d).toPath()
        }
    }
}

@Composable
fun SettingsIcon(
    name: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp
) {
    val path = remember(name) { SettingsIconPaths.getPath(name) }
    Canvas(modifier = modifier.size(size)) {
        if (path != null) {
            val scale = this.size.minDimension / 24f
            withTransform({
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                drawPath(
                    path = path,
                    color = tint,
                    style = Stroke(
                        width = 1.8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}
