package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.CopperSubtle
import com.reflex.app.ui.theme.ReflexTokens

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPickerModal(
    selectedIconKey: String,
    onSelectIcon: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ReflexTokens.ShapeDialog,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ReflexTokens.SpaceXl)
            ) {
                Text(
                    text = "Select icon",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.sp
                )

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm),
                    verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                ) {
                    RoutineIcon.entries.forEach { iconItem ->
                        val isSelected = iconItem.key.equals(selectedIconKey, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(ReflexTokens.ShapeChip)
                                .background(if (isSelected) CopperSubtle else MaterialTheme.colorScheme.secondaryContainer)
                                .border(
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else ReflexTokens.BorderHairline,
                                        if (isSelected) CopperPrimary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    shape = ReflexTokens.ShapeChip
                                )
                                .clickable {
                                    onSelectIcon(iconItem.key)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconItem.icon,
                                contentDescription = iconItem.label,
                                modifier = Modifier.size(24.dp),
                                tint = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    ReflexButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        variant = ReflexButtonVariant.SECONDARY
                    )
                }
            }
        }
    }
}
