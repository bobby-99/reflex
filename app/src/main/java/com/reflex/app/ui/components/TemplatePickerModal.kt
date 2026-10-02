package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reflex.app.data.RoutineIcon
import com.reflex.app.data.RoutineTemplate
import com.reflex.app.data.RoutineTemplates
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens

@Composable
fun TemplatePickerModal(
    onDismiss: () -> Unit,
    onSelectTemplate: (RoutineTemplate) -> Unit,
    onStartFromScratch: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ReflexTokens.ShapeDialog,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
        ) {
            Column(modifier = Modifier.padding(ReflexTokens.SpaceLg)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Choose template",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Start from pre-built steps or custom",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline), CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                // Templates List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                ) {
                    items(RoutineTemplates.templates) { template ->
                        TemplateRow(
                            template = template,
                            onClick = { onSelectTemplate(template) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                // Start from Scratch (GymMane White Pill)
                ReflexButton(
                    text = "Start from scratch",
                    onClick = onStartFromScratch,
                    variant = ReflexButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TemplateRow(
    template: RoutineTemplate,
    onClick: () -> Unit
) {
    val vectorIcon = RoutineIcon.fromKey(template.iconKey).icon
    val totalSeconds = template.steps.sumOf { it.durationSeconds ?: 60 }
    val estimatedMins = (totalSeconds / 60).coerceAtLeast(1)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ReflexTokens.ShapeInnerTile)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
                ReflexTokens.ShapeInnerTile
            )
            .clickable(onClick = onClick)
            .padding(ReflexTokens.SpaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(ReflexTokens.ShapeIconTile)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .border(
                        BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
                        ReflexTokens.ShapeIconTile
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = vectorIcon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = CopperPrimary
                )
            }

            Spacer(modifier = Modifier.width(ReflexTokens.SpaceMd))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${template.steps.size} steps • ~$estimatedMins mins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
