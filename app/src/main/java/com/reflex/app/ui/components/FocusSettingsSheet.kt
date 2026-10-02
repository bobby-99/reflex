package com.reflex.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.BlockingMode
import com.reflex.app.data.FocusSettings
import com.reflex.app.ui.theme.AppTheme
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.OnCopper
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.border
import com.reflex.app.util.ThemePreferenceRepository

/**
 * Focus settings sheet rebuilt according to DESIGN.md v3.0 / reflex-focus.html Section 4:
 * - 36dp top radius, surface background, border. Drag handle 40x5dp. Title "Focus settings" (19sp SemiBold).
 * - Three switch rows: Sound alerts, Vibration alert, Auto-start next phase (52x30dp ReflexSwitch).
 * - App blocking card with switch and 48dp SurfaceInput button "Configure app blocking".
 * - Appearance row with 44dp segmented control (System / Dark / Light).
 * - Immediate persistence without Save button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusSettingsSheet(
    settings: FocusSettings,
    onDismiss: () -> Unit,
    onUpdateSettings: (FocusSettings) -> Unit,
    onConfigureAppBlocking: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by ThemePreferenceRepository.currentTheme.collectAsState()

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
                    .background(MaterialTheme.colorScheme.border, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Focus settings",
                fontFamily = Lora,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Switch: Sound alerts
            SettingsSwitchRow(
                title = "Sound alerts",
                description = "Play notification tone on phase end",
                checked = settings.soundEnabled,
                onCheckedChange = { checked ->
                    onUpdateSettings(settings.copy(soundEnabled = checked))
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

            // Switch: Vibration alert
            SettingsSwitchRow(
                title = "Vibration alert",
                description = "Haptic feedback on phase end",
                checked = settings.vibrationEnabled,
                onCheckedChange = { checked ->
                    onUpdateSettings(settings.copy(vibrationEnabled = checked))
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

            // Switch: Auto-start next phase
            SettingsSwitchRow(
                title = "Auto-start next phase",
                description = "Continue without tapping start",
                checked = settings.autoStartNextPhase,
                onCheckedChange = { checked ->
                    onUpdateSettings(settings.copy(autoStartNextPhase = checked))
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

            // App Blocking Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "App blocking",
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Restrict distracting apps during focus phases. Pauses during breaks.",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    val isAppBlockingOn = settings.blockingMode != BlockingMode.OFF
                    ReflexSwitch(
                        checked = isAppBlockingOn,
                        onCheckedChange = { checked ->
                            onUpdateSettings(
                                settings.copy(
                                    blockingMode = if (checked) BlockingMode.BLOCK_LIST else BlockingMode.OFF
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Configure app blocking button (48dp, SurfaceInput)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onConfigureAppBlocking),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Configure app blocking",
                        fontFamily = Lora,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

            // Appearance Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = "Appearance",
                    fontFamily = Lora,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // 44dp Segmented control: System / Dark / Light
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val themes = listOf(
                        AppTheme.SYSTEM to "System",
                        AppTheme.DARK to "Dark",
                        AppTheme.LIGHT to "Light"
                    )
                    themes.forEach { (theme, label) ->
                        val isSelected = currentTheme == theme
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) CopperPrimary else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable {
                                    ThemePreferenceRepository.setTheme(context, theme)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontFamily = Lora,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isSelected) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                fontFamily = Lora,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontFamily = Lora,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ReflexSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
