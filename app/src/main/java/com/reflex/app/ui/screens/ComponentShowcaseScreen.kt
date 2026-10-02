package com.reflex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.reflex.app.data.StepType
import com.reflex.app.ui.components.BigNumberDisplay
import com.reflex.app.ui.components.EmptyState
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTextField
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.SectionHeader
import com.reflex.app.ui.components.StepTypeChip
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance

/**
 * Dev-only component showcase screen.
 * Displays every design system component and typography style for visual verification.
 * Temporarily set as start destination during development; easy to remove later.
 */
@Composable
fun ComponentShowcaseScreen(
    onNavigateBack: () -> Unit
) {
    SetStatusBarAppearance()

    val ink = MaterialTheme.colorScheme.onBackground
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val surface = MaterialTheme.colorScheme.surface

    val scrollState = rememberScrollState()
    var textFieldValue by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        ReflexTopBar(
            title = "Component Showcase",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = ReflexTokens.SpaceLg)
        ) {
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // ============================================================
            // SECTION: Typography Scale
            // ============================================================
            SectionHeader(title = "Typography Scale", subtitle = "Bebas Neue + IBM Plex Sans")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            Text("Display Large", style = MaterialTheme.typography.displayLarge, color = ink)
            Text("Display Medium", style = MaterialTheme.typography.displayMedium, color = ink)
            Text("Display Small", style = MaterialTheme.typography.displaySmall, color = ink)

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            Text("HEADLINE LARGE", style = MaterialTheme.typography.headlineLarge, color = ink)
            Text("HEADLINE MEDIUM", style = MaterialTheme.typography.headlineMedium, color = ink)
            Text("HEADLINE SMALL", style = MaterialTheme.typography.headlineSmall, color = ink)

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            Text("TITLE LARGE (BEBAS)", style = MaterialTheme.typography.titleLarge, color = ink)
            Text("Title Medium (IBM Plex Sans Bold)", style = MaterialTheme.typography.titleMedium, color = ink)
            Text("Title Small (IBM Plex Sans SemiBold)", style = MaterialTheme.typography.titleSmall, color = ink)

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            Text("Body Large — The quick brown fox jumps over the lazy dog.", style = MaterialTheme.typography.bodyLarge, color = ink)
            Text("Body Medium — The quick brown fox jumps over the lazy dog.", style = MaterialTheme.typography.bodyMedium, color = ink)
            Text("Body Small — The quick brown fox jumps over the lazy dog.", style = MaterialTheme.typography.bodySmall, color = ink)

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            Text("LABEL LARGE", style = MaterialTheme.typography.labelLarge, color = ink)
            Text("LABEL MEDIUM", style = MaterialTheme.typography.labelMedium, color = ink)
            Text("LABEL SMALL", style = MaterialTheme.typography.labelSmall, color = ink)

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Big Number Display
            // ============================================================
            SectionHeader(title = "Big Number Display", subtitle = "Timer countdown & stats")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primary)
                    .padding(ReflexTokens.SpaceXl)
            ) {
                BigNumberDisplay(
                    value = "02:45",
                    label = "Remaining"
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BigNumberDisplay(value = "5", label = "Steps")
                BigNumberDisplay(value = "12", label = "Streak")
                BigNumberDisplay(value = "98%", label = "Rate")
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Buttons
            // ============================================================
            SectionHeader(title = "Buttons", subtitle = "Primary & secondary variants")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            ReflexButton(
                text = "Primary Button",
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
            ReflexButton(
                text = "Secondary Button",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = ReflexButtonVariant.SECONDARY
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
            ReflexButton(
                text = "Disabled Primary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
            ReflexButton(
                text = "Disabled Secondary",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = ReflexButtonVariant.SECONDARY,
                enabled = false
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Cards
            // ============================================================
            SectionHeader(title = "Cards", subtitle = "Flat block containers")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            ReflexCard {
                Column {
                    Text(
                        text = "MORNING ROUTINE",
                        style = MaterialTheme.typography.titleLarge,
                        color = ink
                    )
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
                    Text(
                        text = "5 steps \u2022 ~12 min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            ReflexCard(onClick = {}) {
                Column {
                    Text(
                        text = "CLICKABLE CARD",
                        style = MaterialTheme.typography.titleLarge,
                        color = ink
                    )
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
                    Text(
                        text = "This card responds to taps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ink.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Step Type Chips
            // ============================================================
            SectionHeader(title = "Step Type Chips", subtitle = "Visual step type indicators")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            Row(
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)
            ) {
                StepTypeChip(stepType = StepType.TIMED)
                StepTypeChip(stepType = StepType.CHECK_OFF)
                StepTypeChip(stepType = StepType.REPEAT_COUNT)
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Text Field
            // ============================================================
            SectionHeader(title = "Text Input", subtitle = "Hard-edge text field")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            ReflexTextField(
                value = textFieldValue,
                onValueChange = { textFieldValue = it },
                label = "Routine Name",
                placeholder = "Enter routine name..."
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Empty State
            // ============================================================
            SectionHeader(title = "Empty State", subtitle = "For screens with no data")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            ReflexCard {
                EmptyState(
                    icon = Icons.Default.FlashOn,
                    message = "No routines yet",
                    actionLabel = "Create First Routine",
                    onAction = {}
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Top Bar Variants
            // ============================================================
            SectionHeader(title = "Top Bar", subtitle = "With and without back button")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
            ) {
                ReflexTopBar(title = "Screen Title")
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(surface)
            ) {
                ReflexTopBar(
                    title = "With Back",
                    onBackClick = {}
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // ============================================================
            // SECTION: Color Palette
            // ============================================================
            SectionHeader(title = "Color Palette", subtitle = "Brand colors")

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                ColorSwatch(
                    color = primary,
                    name = "Primary / Accent",
                    modifier = Modifier.weight(1f),
                    textColor = ink
                )
                ColorSwatch(
                    color = ink,
                    name = "OnBackground / Ink",
                    modifier = Modifier.weight(1f),
                    textColor = surface
                )
                ColorSwatch(
                    color = surface,
                    name = "Surface",
                    modifier = Modifier.weight(1f),
                    textColor = ink
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxxl))
        }
    }
}

@Composable
private fun ColorSwatch(
    color: androidx.compose.ui.graphics.Color,
    name: String,
    modifier: Modifier = Modifier,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.8f)
        )
    }
}
