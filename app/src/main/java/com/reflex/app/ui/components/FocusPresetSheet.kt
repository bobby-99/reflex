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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.FocusPreset
import com.reflex.app.data.FocusSettings
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.OnCopper
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.sage
import com.reflex.app.util.FocusPresetRepository
import java.util.UUID

/**
 * FocusPresetSheet allows managing, creating, editing, and deleting focus presets.
 * Fully supports 0-min breaks (removes short/long break from cycle) and arbitrary custom focus durations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusPresetSheet(
    currentSettings: FocusSettings,
    onDismiss: () -> Unit,
    onApplyPreset: (FocusPreset) -> Unit
) {
    val context = LocalContext.current
    val presets by FocusPresetRepository.presets.collectAsState()
    val activePreset by FocusPresetRepository.activePreset.collectAsState()

    var editingPreset by remember { mutableStateOf<FocusPreset?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 14.dp)
                    .size(width = ReflexTokens.FocusSheetDragHandleWidth, height = ReflexTokens.FocusSheetDragHandleHeight)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Focus Presets",
                        fontFamily = Lora,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Custom cycles and break durations",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Add Preset Button
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CopperContainer)
                        .clickable {
                            editingPreset = FocusPreset(
                                id = UUID.randomUUID().toString(),
                                name = "Custom Preset",
                                description = "",
                                workDurationMin = currentSettings.workDurationMin,
                                shortBreakMin = currentSettings.shortBreakMin,
                                longBreakMin = currentSettings.longBreakMin,
                                sessionsBeforeLongBreak = currentSettings.sessionsBeforeLongBreak,
                                autoStartNextPhase = currentSettings.autoStartNextPhase,
                                isDefault = false
                            )
                            isCreatingNew = true
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CopperPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "New",
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CopperPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Preset List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(presets, key = { it.id }) { preset ->
                    val isActive = activePreset.id == preset.id ||
                            (currentSettings.workDurationMin == preset.workDurationMin &&
                                    currentSettings.shortBreakMin == preset.shortBreakMin &&
                                    currentSettings.longBreakMin == preset.longBreakMin &&
                                    currentSettings.sessionsBeforeLongBreak == preset.sessionsBeforeLongBreak)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) CopperContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(
                                BorderStroke(
                                    if (isActive) 1.5.dp else 1.dp,
                                    if (isActive) CopperPrimary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                FocusPresetRepository.selectPreset(context, preset.id)
                                onApplyPreset(preset)
                            }
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = preset.name,
                                        fontFamily = Lora,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isActive) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(CopperPrimary)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Active",
                                                fontFamily = Lora,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = OnCopper
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Edit button
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                editingPreset = preset
                                                isCreatingNew = false
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit preset",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Delete button (for non-default or if more than 1)
                                    if (!preset.isDefault && presets.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                            .clickable {
                                                FocusPresetRepository.deletePreset(context, preset.id)
                                            },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete preset",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Breakdown chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val sBreakText = if (preset.shortBreakMin > 0) "${preset.shortBreakMin}m break" else "No short break"
                                val lBreakText = if (preset.longBreakMin > 0) "${preset.longBreakMin}m long" else "No long break"

                                Text(
                                    text = "${preset.workDurationMin}m focus · $sBreakText · $lBreakText · ${preset.sessionsBeforeLongBreak} cycles",
                                    fontFamily = Lora,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Button: Save Current Configuration as Preset
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable {
                        editingPreset = FocusPreset(
                            id = UUID.randomUUID().toString(),
                            name = "My ${currentSettings.workDurationMin}m Preset",
                            description = "",
                            workDurationMin = currentSettings.workDurationMin,
                            shortBreakMin = currentSettings.shortBreakMin,
                            longBreakMin = currentSettings.longBreakMin,
                            sessionsBeforeLongBreak = currentSettings.sessionsBeforeLongBreak,
                            autoStartNextPhase = currentSettings.autoStartNextPhase,
                            isDefault = false
                        )
                        isCreatingNew = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save current settings as preset",
                    fontFamily = Lora,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Preset Editor Dialog
    editingPreset?.let { p ->
        PresetEditorDialog(
            preset = p,
            isNew = isCreatingNew,
            onDismiss = { editingPreset = null },
            onSave = { savedPreset ->
                FocusPresetRepository.savePreset(context, savedPreset)
                onApplyPreset(savedPreset)
                editingPreset = null
            }
        )
    }
}

/**
 * Editor dialog for customizing any aspect of a Focus preset,
 * explicitly allowing 0 minutes for short/long breaks.
 */
@Composable
private fun PresetEditorDialog(
    preset: FocusPreset,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (FocusPreset) -> Unit
) {
    var name by remember { mutableStateOf(preset.name) }
    var workMin by remember { mutableIntStateOf(preset.workDurationMin) }
    var shortMin by remember { mutableIntStateOf(preset.shortBreakMin) }
    var longMin by remember { mutableIntStateOf(preset.longBreakMin) }
    var cycles by remember { mutableIntStateOf(preset.sessionsBeforeLongBreak) }
    var autoNext by remember { mutableStateOf(preset.autoStartNextPhase) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isNew) "New Preset" else "Edit Preset",
                fontFamily = Lora,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset name", fontFamily = Lora) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CopperPrimary,
                        cursorColor = CopperPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Work duration stepper (1 to 180m)
                InlineStepper(
                    label = "Focus length",
                    value = workMin,
                    unit = "min",
                    min = 1,
                    max = 180,
                    step = 5,
                    onValueChange = { workMin = it }
                )

                // Short break stepper (0 to 60m, 0 means Off)
                InlineStepper(
                    label = "Short break",
                    value = shortMin,
                    unit = "min",
                    min = 0,
                    max = 60,
                    step = 1,
                    zeroLabel = "0m (Off)",
                    onValueChange = { shortMin = it }
                )

                // Long break stepper (0 to 120m, 0 means Off)
                InlineStepper(
                    label = "Long break",
                    value = longMin,
                    unit = "min",
                    min = 0,
                    max = 120,
                    step = 1,
                    zeroLabel = "0m (Off)",
                    onValueChange = { longMin = it }
                )

                // Cycles count (1 to 16)
                InlineStepper(
                    label = "Sessions per cycle",
                    value = cycles,
                    unit = "",
                    min = 1,
                    max = 16,
                    step = 1,
                    onValueChange = { cycles = it }
                )

                // Auto-start next phase switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Auto-start next phase",
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Continue without manual tap",
                            fontFamily = Lora,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    ReflexSwitch(
                        checked = autoNext,
                        onCheckedChange = { autoNext = it }
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(CopperPrimary)
                    .clickable {
                        onSave(
                            preset.copy(
                                name = name.ifBlank { "Custom Preset" },
                                workDurationMin = workMin.coerceAtLeast(1),
                                shortBreakMin = shortMin.coerceAtLeast(0),
                                longBreakMin = longMin.coerceAtLeast(0),
                                sessionsBeforeLongBreak = cycles.coerceIn(1, 16),
                                autoStartNextPhase = autoNext
                            )
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Save",
                    fontFamily = Lora,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnCopper
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontFamily = Lora,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun InlineStepper(
    label: String,
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    step: Int,
    zeroLabel: String = "Off",
    onValueChange: (Int) -> Unit
) {
    var showDirectInput by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = Lora,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (min == 0) {
                Text(
                    text = "Set to 0 to remove",
                    fontFamily = Lora,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Minus
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = value > min) {
                        onValueChange((value - step).coerceAtLeast(min))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = if (value > min) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Clickable value
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { showDirectInput = true }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val display = if (value == 0 && min == 0) zeroLabel else if (unit.isNotEmpty()) "$value $unit" else "$value"
                Text(
                    text = display,
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value == 0 && min == 0) CopperPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Plus
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = value < max) {
                        onValueChange((value + step).coerceAtMost(max))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = if (value < max) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }

    if (showDirectInput) {
        var textInput by remember { mutableStateOf(value.toString()) }
        AlertDialog(
            onDismissRequest = { showDirectInput = false },
            title = { Text(text = "Set $label", fontFamily = Lora, fontSize = 16.sp) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Value ($min - $max)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CopperPrimary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val num = textInput.toIntOrNull() ?: value
                        onValueChange(num.coerceIn(min, max))
                        showDirectInput = false
                    }
                ) {
                    Text("OK", color = CopperPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectInput = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
