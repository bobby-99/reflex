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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.Step
import com.reflex.app.data.StepType
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepEditSheet(
    step: Step?,
    orderIndex: Int,
    routineId: Long,
    onDismiss: () -> Unit,
    onSave: (Step) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
        shape = ReflexTokens.ShapeModal,
        dragHandle = null
    ) {
        StepEditContent(
            initialStep = step,
            orderIndex = orderIndex,
            routineId = routineId,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@Composable
private fun StepEditContent(
    initialStep: Step?,
    orderIndex: Int,
    routineId: Long,
    onDismiss: () -> Unit,
    onSave: (Step) -> Unit
) {
    var name by remember { mutableStateOf(initialStep?.name ?: "") }
    var stepType by remember { mutableStateOf(initialStep?.stepType ?: StepType.TIMED) }

    val initialDuration = initialStep?.durationSeconds ?: 60
    var durationMinutes by remember { mutableIntStateOf(initialDuration / 60) }
    var durationSeconds by remember { mutableIntStateOf(initialDuration % 60) }

    var targetCount by remember { mutableIntStateOf(initialStep?.targetCount ?: 3) }
    val defaultRestSetting = com.reflex.app.util.SettingsRepository.getString("r_rest", "15s")
    val defaultRestSec = when (defaultRestSetting) {
        "10s" -> 10
        "15s" -> 15
        "20s" -> 20
        "30s" -> 30
        else -> 0
    }
    var restDurationSeconds by remember {
        mutableIntStateOf(initialStep?.restDurationSeconds ?: if (defaultRestSec > 0) defaultRestSec else 15)
    }
    var notes by remember { mutableStateOf(initialStep?.notes ?: "") }
    var nameError by remember { mutableStateOf(false) }

    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(ReflexTokens.SpaceLg)
            .verticalScroll(rememberScrollState())
    ) {
        // Drag Pill Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (initialStep == null) "New step" else "Edit step",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = textSecondary,
                modifier = Modifier
                    .clip(ReflexTokens.ShapeChip)
                    .clickable { onDismiss() }
                    .padding(ReflexTokens.SpaceXs)
            )
        }

        Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

        // Step Name Field
        ReflexTextField(
            value = name,
            onValueChange = {
                name = it
                if (it.isNotBlank()) nameError = false
            },
            label = "Step Name",
            placeholder = "e.g. Deep Breathing, Pushups, Stretch",
            modifier = Modifier.fillMaxWidth()
        )
        if (nameError) {
            Text(
                text = "Step name cannot be empty",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

        // Step Type Chips
        Text(
            text = "Step type",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textSecondary,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
        ) {
            StepType.entries.forEach { type ->
                StepTypeChip(
                    stepType = type,
                    isSelected = stepType == type,
                    onClick = { stepType = type }
                )
            }
        }

        Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

        // Type Specific Steppers
        when (stepType) {
            StepType.TIMED -> {
                Text(
                    text = "Step duration",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceLg)
                ) {
                    StepperControl(
                        label = "Minutes",
                        value = durationMinutes,
                        onValueChange = { durationMinutes = it },
                        onDecrement = { if (durationMinutes > 0) durationMinutes-- },
                        onIncrement = { if (durationMinutes < 99) durationMinutes++ }
                    )

                    StepperControl(
                        label = "Seconds",
                        value = durationSeconds,
                        onValueChange = { durationSeconds = it },
                        onDecrement = { if (durationSeconds >= 5) durationSeconds -= 5 else durationSeconds = 0 },
                        onIncrement = { if (durationSeconds <= 54) durationSeconds += 5 else durationSeconds = 59 },
                        maxVal = 59
                    )
                }
            }
            StepType.REPEAT_COUNT -> {
                Text(
                    text = "Sets & rest",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    letterSpacing = 0.sp
                )
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceLg)
                ) {
                    StepperControl(
                        label = "Sets",
                        value = targetCount,
                        onValueChange = { targetCount = it.coerceAtLeast(1) },
                        onDecrement = { if (targetCount > 1) targetCount-- },
                        onIncrement = { targetCount++ }
                    )
                    StepperControl(
                        label = "Rest (sec)",
                        value = restDurationSeconds,
                        onValueChange = { restDurationSeconds = it.coerceAtLeast(0) },
                        onDecrement = { if (restDurationSeconds >= 5) restDurationSeconds -= 5 else restDurationSeconds = 0 },
                        onIncrement = { if (restDurationSeconds <= 295) restDurationSeconds += 5 else restDurationSeconds = 300 },
                        maxVal = 300
                    )
                }
            }
            StepType.CHECK_OFF -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ReflexTokens.ShapeCard)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(ReflexTokens.SpaceMd)
                ) {
                    Text(
                        text = "Manual tap check-off during timer sequence (untimed step)",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

        // Notes Input
        ReflexTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes / Tips (Optional)",
            placeholder = "Tips or guidance visible during session...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReflexButton(
                text = "Cancel",
                onClick = onDismiss,
                variant = ReflexButtonVariant.SECONDARY
            )
            Spacer(modifier = Modifier.width(ReflexTokens.SpaceMd))
            ReflexButton(
                text = if (initialStep == null) "Add step" else "Save step",
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        val totalSeconds = (durationMinutes * 60) + durationSeconds
                        val finalStep = Step(
                            id = initialStep?.id ?: 0L,
                            routineId = routineId,
                            name = name.trim(),
                            orderIndex = initialStep?.orderIndex ?: orderIndex,
                            stepType = stepType,
                            durationSeconds = if (stepType == StepType.TIMED) totalSeconds else null,
                            targetCount = if (stepType == StepType.REPEAT_COUNT) targetCount else null,
                            restDurationSeconds = if (stepType == StepType.REPEAT_COUNT) restDurationSeconds else null,
                            notes = notes.trim().ifEmpty { null },
                            emoji = null
                        )
                        onSave(finalStep)
                        onDismiss()
                    }
                },
                variant = ReflexButtonVariant.PRIMARY
            )
        }
    }
}

/**
 * GymMane-style numeric stepper with pill capsule styling and rounded tap buttons.
 */
@Composable
private fun StepperControl(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    maxVal: Int = 99
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .clip(ReflexTokens.ShapeButton)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .border(
                    BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
                    ReflexTokens.ShapeButton
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDecrement),
                contentAlignment = Alignment.Center
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = value.toString(),
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.isEmpty()) {
                            onValueChange(0)
                        } else if (digits.length <= 3) {
                            val num = digits.toIntOrNull() ?: 0
                            onValueChange(num.coerceIn(0, maxVal))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = CopperPrimary,
                        textAlign = TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onIncrement),
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
