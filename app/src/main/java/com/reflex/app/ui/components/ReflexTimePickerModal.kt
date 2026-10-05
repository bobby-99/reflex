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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.reflex.app.ui.theme.ReflexTokens

@Composable
fun ReflexTimePickerModal(
    initialTime: String? = null,
    title: String = "Set reminder time",
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val initialParts = try {
        if (!initialTime.isNullOrBlank()) {
            val p = initialTime.split(":")
            val h24 = p[0].toInt()
            val m = p[1].toInt()
            val pm = h24 >= 12
            val h12 = when {
                h24 == 0 -> 12
                h24 > 12 -> h24 - 12
                else -> h24
            }
            Triple(h12.toString(), String.format("%02d", m), pm)
        } else {
            Triple("8", "00", false)
        }
    } catch (e: Exception) {
        Triple("8", "00", false)
    }

    var hourInput by remember { mutableStateOf(initialParts.first) }
    var minInput by remember { mutableStateOf(initialParts.second) }
    var isPm by remember { mutableStateOf(initialParts.third) }

    val ink = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surface

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ReflexTokens.ShapeDialog,
            color = surface,
            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ReflexTokens.SpaceLg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ink
                )

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Input
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelSmall, color = ink)
                        Spacer(modifier = Modifier.height(4.dp))
                        ReflexTextField(
                            value = hourInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || (input.toIntOrNull() != null && input.toInt() in 1..12)) {
                                    hourInput = input
                                }
                            },
                            modifier = Modifier.width(70.dp)
                        )
                    }

                    Text(":", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = ink)

                    // Minute Input
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelSmall, color = ink)
                        Spacer(modifier = Modifier.height(4.dp))
                        ReflexTextField(
                            value = minInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || (input.toIntOrNull() != null && input.toInt() in 0..59)) {
                                    minInput = input
                                }
                            },
                            modifier = Modifier.width(70.dp)
                        )
                    }

                    // AM/PM Toggle
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Period", style = MaterialTheme.typography.labelSmall, color = ink)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(ReflexTokens.ShapeChip)
                                .border(BorderStroke(ReflexTokens.BorderHairline, ink.copy(alpha = 0.3f)), ReflexTokens.ShapeChip)
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .background(if (!isPm) primary else surface)
                                    .clickable { isPm = false }
                                    .padding(horizontal = ReflexTokens.SpaceMd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("AM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (!isPm) onPrimary else ink)
                            }
                            Box(
                                modifier = Modifier
                                    .height(44.dp)
                                    .background(if (isPm) primary else surface)
                                    .clickable { isPm = true }
                                    .padding(horizontal = ReflexTokens.SpaceMd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("PM", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = if (isPm) onPrimary else ink)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)
                ) {
                    ReflexButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        variant = ReflexButtonVariant.SECONDARY
                    )

                    ReflexButton(
                        text = "Set time",
                        onClick = {
                            val h12 = hourInput.toIntOrNull() ?: 8
                            val m = minInput.toIntOrNull() ?: 0
                            var h24 = if (h12 == 12) 0 else h12
                            if (isPm) h24 += 12
                            val formatted = String.format("%02d:%02d", h24, m)
                            onTimeSelected(formatted)
                        },
                        modifier = Modifier.weight(1f),
                        variant = ReflexButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}
