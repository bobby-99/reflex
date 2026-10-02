package com.reflex.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTextField
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.CopperSubtle
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance

data class GuideSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val items: List<GuideItem>
)

data class GuideItem(
    val heading: String,
    val content: String,
    val codeSnippet: String? = null,
    val actionText: String? = null,
    val onAction: (() -> Unit)? = null
)

@Composable
fun HelpGuideScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    SetStatusBarAppearance(isLightBackground = false)

    var searchQuery by remember { mutableStateOf("") }
    val expandedSections = remember { mutableStateMapOf<String, Boolean>() }

    val sections = listOf(
        GuideSection(
            id = "getting_started",
            title = "1. GETTING STARTED",
            subtitle = "Core navigation and philosophy behind Reflex",
            items = listOf(
                GuideItem(
                    heading = "What is Reflex?",
                    content = "Reflex is a high-velocity habit, task, and focus system built for zero friction. It uses dark-first ergonomics and strict execution models to ensure you build unbreakable routines and eliminate digital distractions."
                ),
                GuideItem(
                    heading = "The Four Core Spaces",
                    content = "• ROUTINES — Sequence habit steps with exact timers, rest breaks, streaks, and scheduled reminders.\n• TASKS — Capture quick to-dos using natural language parsing, voice input, recurrence, and agenda timelines.\n• FOCUS — Execute Pomodoro or Zen flow sessions with automatic Android app blocking and distraction analytics.\n• CALENDAR — Unified chronological timeline blending routines, tasks, and system calendar events."
                )
            )
        ),
        GuideSection(
            id = "routines",
            title = "2. ROUTINES & EXECUTION",
            subtitle = "Building, scheduling, step types, streaks, and timer controls",
            items = listOf(
                GuideItem(
                    heading = "Creating & Reordering Steps",
                    content = "Build routines using pre-built templates or from scratch. Reorder steps in the routine editor using the position controls to adapt your sequence smoothly."
                ),
                GuideItem(
                    heading = "Step Types & Rest Breaks",
                    content = "• TIMED — Runs a countdown timer (e.g. 5 min Meditation).\n• CHECK OFF — Manual completion check (e.g. Drink 500ml Water).\n• REPEAT COUNT — Multi-rep counter (e.g. 20 Pushups).\nEnable Rest Between Steps in the editor to automatically pause and recover between sequence steps."
                ),
                GuideItem(
                    heading = "Streaks & Grace Days",
                    content = "Completing a routine on scheduled days increments your active streak. If you miss a scheduled day, Reflex automatically protects your consistency with built-in grace logic!"
                ),
                GuideItem(
                    heading = "Live Timer Execution Controls",
                    content = "While running a routine, use live execution controls:\n• PAUSE / RESUME — Hold or resume countdown.\n• +10 SEC / +1 MIN — Extend active step on the fly.\n• PREVIOUS / SKIP — Navigate backward or jump ahead in the sequence."
                )
            )
        ),
        GuideSection(
            id = "tasks",
            title = "3. TASKS, VOICE & NATURAL LANGUAGE",
            subtitle = "Smart capture, NLP syntax, voice input, and agenda timeline",
            items = listOf(
                GuideItem(
                    heading = "Natural Language Parsing (NLP)",
                    content = "Type tasks in plain English. Reflex automatically detects dates, times, durations, recurrence, and priority shorthand:",
                    codeSnippet = "Examples:\n• team sync tmrw 10am 45m !!\n• submit expense report every mon\n• gym workout friday 6pm !\n• buy groceries next tuesday"
                ),
                GuideItem(
                    heading = "Voice Input",
                    content = "Tap the microphone icon in the task input bar. Speak your task naturally—Reflex converts speech to text and parses dates, times, and priorities automatically."
                ),
                GuideItem(
                    heading = "Unified Calendar Matrix",
                    content = "Navigate to the Calendar tab to view your integrated agenda. See routines, tasks, and native device events together on an interactive week strip and month grid."
                ),
                GuideItem(
                    heading = "Swipe Actions",
                    content = "Swipe right on a task row to immediately complete it. Swipe left to edit or delete."
                )
            )
        ),
        GuideSection(
            id = "focus",
            title = "4. FOCUS, POMODORO & APP BLOCKING",
            subtitle = "Classic Pomodoro, Zen flow, app blocking, and analytics",
            items = listOf(
                GuideItem(
                    heading = "Focus Modes",
                    content = "• CLASSIC POMODORO — Structured 25m work / 5m break intervals.\n• TIMED FLOW — Custom duration focus session (e.g. 45 min).\n• OPEN FLOW — Uncapped Zen stopwatch for uninterrupted deep work."
                ),
                GuideItem(
                    heading = "Android App Blocking",
                    content = "Prevent phone distraction during focus sessions. Configure app blocking under Focus Settings:\n• BLOCK LIST — Restrict specific distracting apps (e.g. social media, games).\n• ALLOW LIST — Restrict all apps except work essentials (e.g. docs, communication)."
                ),
                GuideItem(
                    heading = "Focus Analytics & Heatmap",
                    content = "Tap the Analytics button on the Focus tab to view total focus hours, 12-week copper consistency matrix, session distribution, and day inspections."
                )
            )
        ),
        GuideSection(
            id = "qs_tile",
            title = "5. QUICK SETTINGS TILE",
            subtitle = "Floating quick task capture from Android system shade",
            items = listOf(
                GuideItem(
                    heading = "Adding the Reflex QS Tile",
                    content = "1. Swipe down twice from top of screen to open Android Quick Settings.\n2. Tap the Pencil / Edit icon.\n3. Drag 'Reflex Quick Task' into your active tile list.\nNow tap the tile from anywhere in Android to capture tasks over any running app without opening the full screen."
                )
            )
        ),
        GuideSection(
            id = "attribution",
            title = "6. OPEN-SOURCE ATTRIBUTION & LICENSES",
            subtitle = "OpenMoji and open-source credits",
            items = listOf(
                GuideItem(
                    heading = "OpenMoji Asset System",
                    content = "Vector icons and assets provided by OpenMoji under CC BY-SA 4.0 license.\nWebsite: https://openmoji.org"
                )
            )
        ),
        GuideSection(
            id = "tips",
            title = "7. POWER-USER TIPS",
            subtitle = "Pro shortcuts for maximum productivity velocity",
            items = listOf(
                GuideItem(
                    heading = "Keyboard & Focus Shortcuts",
                    content = "• Tap the floating center action button (+) from any tab to immediately trigger quick capture or session starts.\n• Use Settings Data Export to save regular JSON backups of all app data.\n• Exempt Reflex from battery optimizations in Settings to ensure alarms trigger with zero delay."
                )
            )
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReflexTopBar(
            title = "Help & guide",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ReflexTokens.SpaceLg)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            // Search Bar
            ReflexTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search topics (e.g. 'nlp', 'blocking', 'streak')...",
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = CopperPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            sections.forEach { section ->
                val queryLower = searchQuery.trim().lowercase()
                val matchesTitle = section.title.lowercase().contains(queryLower) || section.subtitle.lowercase().contains(queryLower)
                val matchingItems = if (queryLower.isBlank()) section.items else section.items.filter {
                    it.heading.lowercase().contains(queryLower) ||
                            it.content.lowercase().contains(queryLower) ||
                            (it.codeSnippet?.lowercase()?.contains(queryLower) == true)
                }

                val shouldDisplaySection = queryLower.isBlank() || matchesTitle || matchingItems.isNotEmpty()

                if (shouldDisplaySection) {
                    val itemsToRender = if (queryLower.isNotBlank() && !matchesTitle) matchingItems else section.items
                    val isExpanded = queryLower.isNotBlank() || (expandedSections[section.id] ?: (section.id == "getting_started"))

                    // Accordion Header Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ReflexTokens.ShapeCard)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                BorderStroke(
                                    ReflexTokens.BorderHairline,
                                    if (isExpanded) CopperPrimary else MaterialTheme.colorScheme.outline
                                ),
                                shape = ReflexTokens.ShapeCard
                            )
                            .clickable {
                                val current = expandedSections[section.id] ?: (section.id == "getting_started")
                                expandedSections[section.id] = !current
                            }
                            .padding(ReflexTokens.SpaceLg)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpanded) CopperPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = section.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = if (isExpanded) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm),
                            modifier = Modifier.padding(start = ReflexTokens.SpaceSm, end = ReflexTokens.SpaceSm)
                        ) {
                            itemsToRender.forEach { item ->
                                ReflexCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = item.heading,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
                                        Text(
                                            text = item.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
                                        )

                                        if (!item.codeSnippet.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(ReflexTokens.ShapeChip)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                                    .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant), ReflexTokens.ShapeChip)
                                                .padding(ReflexTokens.SpaceSm)
                                            ) {
                                                Text(
                                                    text = item.codeSnippet,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = CopperPrimary,
                                                    lineHeight = 18.sp
                                                )
                                            }
                                        }

                                        if (item.actionText != null && item.onAction != null) {
                                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                                            ReflexButton(
                                                text = item.actionText,
                                                onClick = item.onAction,
                                                variant = ReflexButtonVariant.PRIMARY,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
