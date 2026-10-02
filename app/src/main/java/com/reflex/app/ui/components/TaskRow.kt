package com.reflex.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.Task
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.tertiaryText
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    isNew: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outline
    val tertiary = MaterialTheme.colorScheme.tertiaryText

    // Pop animation: fade + 8dp rise + scale .98 -> 1 over 350ms
    var animationPlayed by remember { mutableStateOf(!isNew) }
    val animProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "task_pop"
    )
    LaunchedEffect(isNew) {
        if (isNew) {
            animationPlayed = true
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onClick()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .graphicsLayer {
                if (isNew) {
                    alpha = animProgress
                    scaleX = 0.98f + (0.02f * animProgress)
                    scaleY = 0.98f + (0.02f * animProgress)
                    translationY = -8.dp.toPx() * (1f - animProgress)
                }
            }
            .clip(ReflexTokens.ShapeInnerTile),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val (bgColor, iconVal, alignment) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(
                    MaterialTheme.colorScheme.primary,
                    Icons.Default.Edit,
                    Alignment.CenterStart
                )
                SwipeToDismissBoxValue.EndToStart -> Triple(
                    MaterialTheme.colorScheme.error,
                    Icons.Default.Delete,
                    Alignment.CenterEnd
                )
                else -> Triple(Color.Transparent, null, Alignment.Center)
            }

            if (iconVal != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(horizontal = ReflexTokens.SpaceLg),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = iconVal,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = ActionPillOnWhite
                    )
                }
            }
        }
    ) {
        val completedAlpha by animateFloatAsState(
            targetValue = if (task.isCompleted) 0.50f else 1.0f,
            animationSpec = spring(stiffness = Spring.StiffnessHigh),
            label = "task_row_alpha"
        )

        val metaText = remember(task.dueDate, task.dueTime, task.recurrenceFrequency, task.priority) {
            formatTaskMeta(task)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (!isNew) {
                        alpha = completedAlpha
                    }
                }
                .clickable(onClick = onClick),
            shape = ReflexTokens.ShapeInnerTile,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, outline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox: 24dp circle with 2dp tertiary border. Done state: copper-filled with OnCopper check.
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(
                            border = BorderStroke(
                                2.dp,
                                if (task.isCompleted) MaterialTheme.colorScheme.primary else tertiary
                            ),
                            shape = CircleShape
                        )
                        .clickable(onClick = onToggleComplete),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title and meta line
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = textPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (!task.notes.isNullOrBlank()) {
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = textSecondary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 13.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = textSecondary
                    )
                }
            }
        }
    }
}

private fun formatTaskMeta(task: Task): String {
    val parts = mutableListOf<String>()

    if (task.dueDate != null) {
        val date = Instant.ofEpochMilli(task.dueDate).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val dateStr = when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.plusDays(7) -> "Next week"
            else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        }
        parts.add(dateStr)
    }

    if (task.dueTime != null) {
        val time = Instant.ofEpochMilli(task.dueTime).atZone(ZoneId.systemDefault()).toLocalTime()
        parts.add(time.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase())
    }

    if (task.recurrenceFrequency != RecurrenceFrequency.NONE) {
        val repeatStr = when (task.recurrenceFrequency) {
            RecurrenceFrequency.DAILY -> "Every day"
            RecurrenceFrequency.WEEKLY -> "Every week"
            RecurrenceFrequency.MONTHLY -> "Every month"
            RecurrenceFrequency.YEARLY -> "Every year"
            RecurrenceFrequency.CUSTOM -> "Repeats custom"
            RecurrenceFrequency.NONE -> ""
        }
        if (repeatStr.isNotEmpty()) parts.add(repeatStr)
    }

    if (task.priority != Priority.NONE) {
        val priStr = when (task.priority) {
            Priority.HIGH -> "High priority"
            Priority.MEDIUM -> "Medium priority"
            Priority.LOW -> "Low priority"
            Priority.NONE -> ""
        }
        if (priStr.isNotEmpty()) parts.add(priStr)
    }

    return if (parts.isEmpty()) "No date" else parts.joinToString(" · ")
}
