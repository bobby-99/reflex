package com.reflex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ReflexApplication
import com.reflex.app.data.BlockingMode
import com.reflex.app.data.FocusMode
import com.reflex.app.data.Task
import com.reflex.app.ui.components.AppPickerSheet
import com.reflex.app.ui.components.FocusPresetSheet
import com.reflex.app.ui.components.FocusSettingsSheet
import com.reflex.app.ui.components.LiquidTimer
import com.reflex.app.ui.components.LocalHazeState
import com.reflex.app.ui.components.PermissionOnboardingSheet
import com.reflex.app.ui.components.ReflexSwitch
import com.reflex.app.util.FocusPresetRepository
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.OnCopper
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.actionPill
import com.reflex.app.ui.theme.border
import com.reflex.app.ui.theme.onActionPill
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.ui.theme.sage
import com.reflex.app.util.AppBlockPermissionHelper
import com.reflex.app.viewmodel.FocusHomeViewModel
import dev.chrisbanes.haze.hazeSource

/**
 * Setup Screen for Focus tab, replicating reflex-focus(1).html and DESIGN.md v3.0:
 * FIX 1:
 * - Root is Box(fillMaxSize()) with:
 *   1. Column(verticalScroll) holding content with bottom padding 172dp + navBarInset, wired to Haze blur source.
 *   2. Bottom gradient fade (196dp + navBarInset), non-interactive.
 *   3. Pinned Start button (56dp full pill, actionPill, 17sp Medium, press scale .98), bottom padding 96dp + navBarInset.
 * - Tightened layout: hero diameter clamp(24% screen height, 150dp, 230dp) & <= 56% width, config rows min 54dp, task-link row 52dp.
 */
@Composable
fun FocusHomeScreen(
    onStartPomodoro: (String?, String?, Long?) -> Unit,
    onStartTimedFlow: (Int, String?, String?, Long?) -> Unit,
    onStartOpenFlow: (String?, String?, Long?) -> Unit,
    onSettingsClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    viewModel: FocusHomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = FocusHomeViewModel.Factory(
            (LocalContext.current.applicationContext as ReflexApplication).repository
        )
    )
) {
    SetStatusBarAppearance()
    val context = LocalContext.current
    val repository = remember { (context.applicationContext as ReflexApplication).repository }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val state by viewModel.uiState.collectAsState()
    val incompleteTasks by remember { repository.getIncompleteTasks() }.collectAsState(initial = emptyList())
    val allTags by viewModel.allTags.collectAsState(initial = emptyList())

    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var selectedTag by remember { mutableStateOf<com.reflex.app.data.FocusTag?>(null) }
    var showTaskPickerSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAppPickerSheet by remember { mutableStateOf(false) }
    var showPermissionSheet by remember { mutableStateOf(false) }
    var showPresetSheet by remember { mutableStateOf(false) }
    val focusPresets by FocusPresetRepository.presets.collectAsState()
    val activePreset by FocusPresetRepository.activePreset.collectAsState()
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun checkPermissionsAndStart(action: () -> Unit) {
        if (state.settings.blockingMode != BlockingMode.OFF) {
            val hasUsage = AppBlockPermissionHelper.hasUsageAccessPermission(context)
            val hasOverlay = AppBlockPermissionHelper.hasOverlayPermission(context)
            if (!hasUsage || !hasOverlay) {
                pendingAction = action
                showPermissionSheet = true
                return
            }
        }
        action()
    }

    val isPomodoro = state.selectedMode == FocusMode.CLASSIC_POMODORO
    val isTimedFlow = state.selectedMode == FocusMode.FLOW_TIMED
    val isOpenFlow = state.selectedMode == FocusMode.FLOW_OPEN

    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp

    // Hero sphere diameter: clamp(24% of screen height, 150dp, 230dp), and <= 56% of screen width
    val heroDiameter = (screenHeight * 0.24f).coerceIn(150.dp, 230.dp).coerceAtMost(screenWidth * 0.56f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fixed Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .reflexStatusBarPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Focus",
                fontFamily = Lora,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 26.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Blocking icon button (44dp visual, 48dp touch target, circle-with-slash)
                val isBlockingOn = state.settings.blockingMode != BlockingMode.OFF
                com.reflex.app.ui.components.TopBarIconButton(
                    iconName = "block",
                    contentDescriptionText = "App blocking",
                    onClick = { showAppPickerSheet = true },
                    isActive = isBlockingOn,
                    activeContainerColor = CopperContainer,
                    activeTint = CopperPrimary
                )

                // History icon button (44dp visual, 48dp touch target, clock-arrow)
                com.reflex.app.ui.components.TopBarIconButton(
                    iconName = "hist",
                    contentDescriptionText = "Focus history",
                    onClick = onAnalyticsClick
                )

                // Focus Settings icon button (44dp visual, 48dp touch target, gear)
                com.reflex.app.ui.components.SettingsTopBarButton(
                    onClick = onSettingsClick,
                    contentDescriptionText = "Focus settings"
                )
            }
        }

        // Screen content & Pinned Start Button
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 1. Scrolling Column holding everything except the pinned Start button
            val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)
            ) {
                // Mode Toggle (44dp segments: Pomodoro vs Flow)
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pomodoro segment
                val pomoSelected = isPomodoro
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(if (pomoSelected) CopperPrimary else Color.Transparent)
                        .clickable { viewModel.selectMode(FocusMode.CLASSIC_POMODORO) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = if (pomoSelected) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Pomodoro",
                            fontFamily = Lora,
                            fontSize = 15.sp,
                            fontWeight = if (pomoSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (pomoSelected) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Flow segment
                val flowSelected = !isPomodoro
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(if (flowSelected) CopperPrimary else Color.Transparent)
                        .clickable {
                            if (isPomodoro) {
                                viewModel.selectMode(FocusMode.FLOW_TIMED)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Waves,
                            contentDescription = null,
                            tint = if (flowSelected) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Flow",
                            fontFamily = Lora,
                            fontSize = 15.sp,
                            fontWeight = if (flowSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (flowSelected) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sub-mode toggle under Flow: Timed flow / Open flow (38dp)
            if (!isPomodoro) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(if (isTimedFlow) CopperPrimary else Color.Transparent)
                            .clickable { viewModel.selectMode(FocusMode.FLOW_TIMED) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Timed flow",
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = if (isTimedFlow) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isTimedFlow) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(CircleShape)
                            .background(if (isOpenFlow) CopperPrimary else Color.Transparent)
                            .clickable { viewModel.selectMode(FocusMode.FLOW_OPEN) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Open flow",
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = if (isOpenFlow) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isOpenFlow) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Pomodoro Presets Bar (Quick switch & Edit)
            if (isPomodoro) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Presets",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { showPresetSheet = true }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit presets",
                            tint = CopperPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Edit / Manage",
                            fontFamily = Lora,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CopperPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(focusPresets, key = { it.id }) { preset ->
                        val isPresetActive = (state.settings.workDurationMin == preset.workDurationMin &&
                                state.settings.shortBreakMin == preset.shortBreakMin &&
                                state.settings.longBreakMin == preset.longBreakMin &&
                                state.settings.sessionsBeforeLongBreak == preset.sessionsBeforeLongBreak)

                        val chipBg = if (isPresetActive) CopperContainer else MaterialTheme.colorScheme.secondaryContainer
                        val chipFg = if (isPresetActive) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        val borderStroke = if (isPresetActive) BorderStroke(1.dp, CopperPrimary) else null

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .then(if (borderStroke != null) Modifier.border(borderStroke, CircleShape) else Modifier)
                                .background(chipBg)
                                .clickable {
                                    FocusPresetRepository.selectPreset(context, preset.id)
                                    viewModel.saveSettings(
                                        state.settings.copy(
                                            workDurationMin = preset.workDurationMin,
                                            shortBreakMin = preset.shortBreakMin,
                                            longBreakMin = preset.longBreakMin,
                                            sessionsBeforeLongBreak = preset.sessionsBeforeLongBreak,
                                            autoStartNextPhase = preset.autoStartNextPhase
                                        )
                                    )
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    fontFamily = Lora,
                                    fontSize = 13.sp,
                                    fontWeight = if (isPresetActive) FontWeight.SemiBold else FontWeight.Medium,
                                    color = chipFg
                                )
                                Text(
                                    text = "${preset.workDurationMin}/${if (preset.shortBreakMin > 0) "${preset.shortBreakMin}m" else "Off"}",
                                    fontFamily = Lora,
                                    fontSize = 11.sp,
                                    color = chipFg.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    // Add Custom Preset chip
                    item {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                .clickable { showPresetSheet = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Custom",
                                    fontFamily = Lora,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CopperPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Hero LiquidTimer (10dp gap above, 2dp below)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                val heroTimeText = when {
                    isPomodoro -> formatHeroDuration(state.settings.workDurationMin)
                    isTimedFlow -> formatHeroDuration(state.timedFlowDurationMin)
                    else -> "00:00"
                }
                val heroLabel = when {
                    isPomodoro -> "Focus"
                    isTimedFlow -> "Timed flow"
                    else -> "Open flow"
                }

                LiquidTimer(
                    ratio = { if (isOpenFlow) 0.55f else 1.0f },
                    timeText = heroTimeText,
                    label = heroLabel,
                    status = "Ready",
                    isBreak = false,
                    isActive = false,
                    modifier = Modifier.size(heroDiameter)
                )
            }

            // Pomodoro Cycle Strip / Flow Captions (10dp above, 6dp below, 12dp below caption)
            if (isPomodoro) {
                Spacer(modifier = Modifier.height(10.dp))
                val nSessions = state.settings.sessionsBeforeLongBreak.coerceIn(1, 16)
                val fMin = state.settings.workDurationMin
                val sMin = state.settings.shortBreakMin
                val lMin = state.settings.longBreakMin

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in 0 until nSessions) {
                        // Focus segment
                        Box(
                            modifier = Modifier
                                .weight(fMin.toFloat().coerceAtLeast(1f))
                                .height(12.dp)
                                .clip(CircleShape)
                                .background(CopperPrimary)
                        )
                        // Break segment (only if duration > 0)
                        val isLongBreak = (i == nSessions - 1)
                        val bWeight = if (isLongBreak) lMin else sMin
                        if (bWeight > 0) {
                            val bColor = if (isLongBreak) MaterialTheme.colorScheme.sage else MaterialTheme.colorScheme.sage.copy(alpha = 0.6f)
                            Box(
                                modifier = Modifier
                                    .weight(bWeight.toFloat())
                                    .height(12.dp)
                                    .clip(CircleShape)
                                    .background(bColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                val totalMinutes = nSessions * fMin + (if (sMin > 0) (nSessions - 1) * sMin else 0) + (if (lMin > 0) lMin else 0)
                val breakDetail = when {
                    sMin == 0 && lMin == 0 -> "no breaks"
                    lMin == 0 -> "${sMin}m short · no long break"
                    sMin == 0 -> "no short break · ${lMin}m long"
                    else -> "${sMin}m short · ${lMin}m long"
                }
                Text(
                    text = "$nSessions × $fMin min focus ($breakDetail) · ${formatCycleTotal(totalMinutes)} per cycle",
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            } else if (isTimedFlow) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "One uninterrupted block, no breaks",
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Counts up, no target",
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }

            // Config Card (32dp radius, surface, 1dp border, min height 60dp rows)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.border, RoundedCornerShape(32.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (isPomodoro) {
                        // Focus length: 1-180 step 5
                        FocusStepperRow(
                            label = "Focus length",
                            value = state.settings.workDurationMin,
                            unit = "min",
                            min = 1,
                            max = 180,
                            step = 5,
                            onValueChange = { newVal ->
                                viewModel.saveSettings(state.settings.copy(workDurationMin = newVal))
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

                        // Short break: 0-60 step 1 (0 = Off)
                        FocusStepperRow(
                            label = "Short break",
                            value = state.settings.shortBreakMin,
                            unit = "min",
                            min = 0,
                            max = 60,
                            step = 1,
                            zeroLabel = "0m (Off)",
                            onValueChange = { newVal ->
                                viewModel.saveSettings(state.settings.copy(shortBreakMin = newVal))
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

                        // Long break: 0-120 step 1 (0 = Off)
                        FocusStepperRow(
                            label = "Long break",
                            value = state.settings.longBreakMin,
                            unit = "min",
                            min = 0,
                            max = 120,
                            step = 1,
                            zeroLabel = "0m (Off)",
                            onValueChange = { newVal ->
                                viewModel.saveSettings(state.settings.copy(longBreakMin = newVal))
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

                        // Sessions per cycle: 1-16 step 1
                        FocusStepperRow(
                            label = "Sessions per cycle",
                            value = state.settings.sessionsBeforeLongBreak,
                            unit = "",
                            min = 1,
                            max = 16,
                            step = 1,
                            onValueChange = { newVal ->
                                viewModel.saveSettings(state.settings.copy(sessionsBeforeLongBreak = newVal))
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.border, thickness = 1.dp)

                        // Auto-start next phase switch row (min height 60dp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Auto-start next phase",
                                    fontFamily = Lora,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Continue without tapping start",
                                    fontFamily = Lora,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ReflexSwitch(
                                checked = state.settings.autoStartNextPhase,
                                onCheckedChange = { checked ->
                                    viewModel.saveSettings(state.settings.copy(autoStartNextPhase = checked))
                                }
                            )
                        }
                    } else if (isTimedFlow) {
                        // Flow duration: 1-360 step 5
                        FocusStepperRow(
                            label = "Flow duration",
                            value = state.timedFlowDurationMin,
                            unit = "min",
                            min = 1,
                            max = 360,
                            step = 5,
                            onValueChange = { newVal ->
                                viewModel.updateTimedFlowDuration(newVal)
                            }
                        )

                        // Five quick chips (15, 30, 45, 60, 90)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp, top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presetMinutes = listOf(15, 30, 45, 60, 90)
                            presetMinutes.forEach { mins ->
                                val isChipActive = state.timedFlowDurationMin == mins
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isChipActive) CopperContainer else MaterialTheme.colorScheme.secondaryContainer)
                                        .clickable { viewModel.updateTimedFlowDuration(mins) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mins}m",
                                        fontFamily = Lora,
                                        fontSize = 14.sp,
                                        fontWeight = if (isChipActive) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isChipActive) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Open flow text block
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = "Open flow (zen mode)",
                                fontFamily = Lora,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "No target countdown timer. Focus continuously until you decide to complete.",
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Task & Tag Linking Row (72dp height, 32dp radius, surface, 1dp border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.border, RoundedCornerShape(32.dp))
                    .clickable { showTaskPickerSheet = true }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 40dp Icon tile
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CopperContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                        contentDescription = null,
                        tint = CopperPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val taskTitle = selectedTask?.title
                    val tagName = selectedTag?.name

                    if (taskTitle != null && tagName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = taskTitle,
                                fontFamily = Lora,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CopperContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tagName,
                                    fontFamily = Lora,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CopperPrimary
                                )
                            }
                        }
                        Text(
                            text = "Linked task & tag, tap to change",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (taskTitle != null) {
                        Text(
                            text = taskTitle,
                            fontFamily = Lora,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Linked task, tap to change",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (tagName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CopperContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tagName,
                                    fontFamily = Lora,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CopperPrimary
                                )
                            }
                        }
                        Text(
                            text = "Linked tag, tap to change",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Link a task or tag",
                            fontFamily = Lora,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Optional, tap to choose",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Bottom clearance for the pinned Start button and floating tab bar (184dp + navBarInset)
            Spacer(modifier = Modifier.height(184.dp + navBarInset))
        }

        // 2. Bottom Gradient Fade: 208dp + navBarInset, non-interactive
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(208.dp + navBarInset)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )

        // 3. Pinned Primary Start Button (56dp, ActionPillWhite, labelLarge 17sp Medium, press scale .98)
        val startInteraction = remember { MutableInteractionSource() }
        val isStartPressed by startInteraction.collectIsPressedAsState()
        val startScale by animateFloatAsState(
            targetValue = if (isStartPressed) 0.98f else 1.0f,
            animationSpec = tween(120),
            label = "StartBtnScale"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 398.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 108.dp + navBarInset)
                .height(56.dp)
                .graphicsLayer {
                    scaleX = startScale
                    scaleY = startScale
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.actionPill)
                .clickable(
                    interactionSource = startInteraction,
                    indication = null,
                    onClick = {
                        checkPermissionsAndStart {
                            when {
                                isPomodoro -> onStartPomodoro(selectedTask?.title, null, selectedTag?.id)
                                isTimedFlow -> onStartTimedFlow(state.timedFlowDurationMin, selectedTask?.title, null, selectedTag?.id)
                                else -> onStartOpenFlow(selectedTask?.title, null, selectedTag?.id)
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val buttonText = when {
                isPomodoro -> "Start pomodoro session"
                isTimedFlow -> "Start timed flow (${state.timedFlowDurationMin}m)"
                else -> "Start open flow session"
            }
            Text(
                text = buttonText,
                fontFamily = Lora,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onActionPill
            )
        }
    }
}

    // Task & Tag Picker Sheet
    if (showTaskPickerSheet) {
        TaskAndTagPickerSheet(
            tasks = incompleteTasks,
            selectedTaskId = selectedTask?.id,
            onSelectTask = { task ->
                selectedTask = task
            },
            tags = allTags,
            selectedTagId = selectedTag?.id,
            onSelectTag = { tag ->
                selectedTag = tag
            },
            onCreateTag = { name ->
                coroutineScope.launch {
                    val result = viewModel.createTag(name)
                    result.onSuccess { newTag ->
                        selectedTag = newTag
                    }
                }
            },
            onRenameTag = { tag, newName ->
                coroutineScope.launch {
                    val result = viewModel.renameTag(tag, newName)
                    result.onSuccess {
                        if (selectedTag?.id == tag.id) {
                            selectedTag = tag.copy(name = newName)
                        }
                    }
                }
            },
            onDeleteTag = { tagId ->
                if (selectedTag?.id == tagId) {
                    selectedTag = null
                }
                viewModel.deleteTag(tagId)
            },
            onDismiss = { showTaskPickerSheet = false }
        )
    }

    // Focus Settings Sheet
    if (showSettingsSheet) {
        FocusSettingsSheet(
            settings = state.settings,
            onDismiss = { showSettingsSheet = false },
            onUpdateSettings = { updated ->
                viewModel.saveSettings(updated)
            },
            onConfigureAppBlocking = {
                showSettingsSheet = false
                showAppPickerSheet = true
            }
        )
    }

    // App Picker Sheet
    if (showAppPickerSheet) {
        AppPickerSheet(
            initialMode = state.settings.blockingMode,
            initialPackages = state.settings.selectedPackagesSet,
            onDismiss = { showAppPickerSheet = false },
            onSave = { mode, pkgs ->
                val updated = state.settings.copy(
                    blockingMode = mode,
                    selectedPackages = pkgs.joinToString(",")
                )
                viewModel.saveSettings(updated)
                showAppPickerSheet = false
            }
        )
    }

    // App Block Permission Onboarding Sheet
    if (showPermissionSheet) {
        PermissionOnboardingSheet(
            onDismiss = { showPermissionSheet = false },
            onPermissionsGranted = {
                showPermissionSheet = false
                pendingAction?.invoke()
                pendingAction = null
            }
        )
    }

    // Focus Presets Sheet
    if (showPresetSheet) {
        FocusPresetSheet(
            currentSettings = state.settings,
            onDismiss = { showPresetSheet = false },
            onApplyPreset = { preset ->
                viewModel.saveSettings(
                    state.settings.copy(
                        workDurationMin = preset.workDurationMin,
                        shortBreakMin = preset.shortBreakMin,
                        longBreakMin = preset.longBreakMin,
                        sessionsBeforeLongBreak = preset.sessionsBeforeLongBreak,
                        autoStartNextPhase = preset.autoStartNextPhase
                    )
                )
                showPresetSheet = false
            }
        )
    }
}

/**
 * 54dp Stepper row with label on left, 40dp SurfaceInput buttons and 72dp centered value on right.
 * Tapping value opens a direct numeric entry dialog. Supports 0 min (Off) for removing breaks.
 */
@Composable
private fun FocusStepperRow(
    label: String,
    value: Int,
    unit: String,
    min: Int,
    max: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    zeroLabel: String = "Off"
) {
    var showDirectInput by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontFamily = Lora,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (min == 0) {
                Text(
                    text = "Tap value or decrement to 0 to remove",
                    fontFamily = Lora,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Minus button (40dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = value > min) {
                        onValueChange((value - step).coerceAtLeast(min))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease $label",
                    tint = if (value > min) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Value text (72dp centered, tap to edit directly)
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showDirectInput = true }
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val displayText = if (value == 0 && min == 0) zeroLabel else if (unit.isNotEmpty()) "$value $unit" else "$value"
                Text(
                    text = displayText,
                    fontFamily = Lora,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value == 0 && min == 0) CopperPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // Plus button (40dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = value < max) {
                        onValueChange((value + step).coerceAtMost(max))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase $label",
                    tint = if (value < max) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDirectInput) {
        var textInput by remember { mutableStateOf(value.toString()) }
        AlertDialog(
            onDismissRequest = { showDirectInput = false },
            title = {
                Text(
                    text = "Set $label",
                    fontFamily = Lora,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Minutes ($min - $max)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CopperPrimary,
                        cursorColor = CopperPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
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

/**
 * Task picker bottom sheet allowing selection of existing incomplete tasks or "No task".
 */
/**
 * Task and Tag picker bottom sheet allowing selection of tasks and focus tags.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun TaskAndTagPickerSheet(
    tasks: List<Task>,
    selectedTaskId: Long?,
    onSelectTask: (Task?) -> Unit,
    tags: List<com.reflex.app.data.FocusTag>,
    selectedTagId: Long?,
    onSelectTag: (com.reflex.app.data.FocusTag?) -> Unit,
    onCreateTag: (String) -> Unit,
    onRenameTag: (com.reflex.app.data.FocusTag, String) -> Unit,
    onDeleteTag: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var tagToRename by remember { mutableStateOf<com.reflex.app.data.FocusTag?>(null) }
    var tagToDelete by remember { mutableStateOf<com.reflex.app.data.FocusTag?>(null) }

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
        ) {
            Text(
                text = "Link task or tag",
                fontFamily = Lora,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // --- Tags Section ---
            Text(
                text = "Tag",
                fontFamily = Lora,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "No tag" Chip
                val isNoneSelected = selectedTagId == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isNoneSelected) CopperContainer else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isNoneSelected) CopperPrimary else MaterialTheme.colorScheme.border, RoundedCornerShape(16.dp))
                        .clickable { onSelectTag(null) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "No tag",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = if (isNoneSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isNoneSelected) CopperPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }

                tags.forEach { tag ->
                    val isSelected = tag.id == selectedTagId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) CopperContainer else MaterialTheme.colorScheme.surface)
                            .border(1.dp, if (isSelected) CopperPrimary else MaterialTheme.colorScheme.border, RoundedCornerShape(16.dp))
                            .clickable {
                                if (isSelected) onSelectTag(null) else onSelectTag(tag)
                            }
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tag.name,
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(4.dp))
                        // Rename button
                        IconButton(
                            onClick = { tagToRename = tag },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename ${tag.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        // Delete button
                        IconButton(
                            onClick = { tagToDelete = tag },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete ${tag.name}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // "+ New tag" chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, CopperPrimary, RoundedCornerShape(16.dp))
                        .clickable { showCreateTagDialog = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CopperPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "New tag",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CopperPrimary
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.border,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // --- Tasks Section ---
            Text(
                text = "Task",
                fontFamily = Lora,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // "No task" item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectTask(null) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "No task",
                        fontFamily = Lora,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Clear linked task",
                        fontFamily = Lora,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedTaskId == null) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = CopperPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active tasks to link",
                        fontFamily = Lora,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        val isSelected = task.id == selectedTaskId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectTask(if (isSelected) null else task) }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = task.title,
                                fontFamily = Lora,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = CopperPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Tag Dialog
    if (showCreateTagDialog) {
        var inputName by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = {
                Text(
                    text = "New tag",
                    fontFamily = Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = {
                            if (it.length <= 24) {
                                inputName = it
                                errorMessage = null
                            }
                        },
                        placeholder = { Text("Study, Focus, Reading, Work", fontFamily = Lora, fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontFamily = Lora
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = inputName.trim()
                        when {
                            trimmed.isEmpty() -> errorMessage = "Name cannot be empty"
                            tags.any { it.name.equals(trimmed, ignoreCase = true) } -> errorMessage = "Tag already exists"
                            else -> {
                                onCreateTag(trimmed)
                                showCreateTagDialog = false
                            }
                        }
                    }
                ) {
                    Text("Create", fontFamily = Lora, fontWeight = FontWeight.SemiBold, color = CopperPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateTagDialog = false }) {
                    Text("Cancel", fontFamily = Lora)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Rename Tag Dialog
    if (tagToRename != null) {
        val currentTag = tagToRename!!
        var inputName by remember { mutableStateOf(currentTag.name) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { tagToRename = null },
            title = {
                Text(
                    text = "Rename tag",
                    fontFamily = Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = {
                            if (it.length <= 24) {
                                inputName = it
                                errorMessage = null
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontFamily = Lora
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = inputName.trim()
                        when {
                            trimmed.isEmpty() -> errorMessage = "Name cannot be empty"
                            tags.any { it.id != currentTag.id && it.name.equals(trimmed, ignoreCase = true) } -> errorMessage = "Tag already exists"
                            else -> {
                                onRenameTag(currentTag, trimmed)
                                tagToRename = null
                            }
                        }
                    }
                ) {
                    Text("Save", fontFamily = Lora, fontWeight = FontWeight.SemiBold, color = CopperPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToRename = null }) {
                    Text("Cancel", fontFamily = Lora)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Delete Tag Dialog
    if (tagToDelete != null) {
        val currentTag = tagToDelete!!
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = {
                Text(
                    text = "Delete tag?",
                    fontFamily = Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Sessions keep their time but become untagged.",
                    fontFamily = Lora,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTag(currentTag.id)
                        tagToDelete = null
                    }
                ) {
                    Text("Delete", fontFamily = Lora, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tagToDelete = null }) {
                    Text("Cancel", fontFamily = Lora)
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

private fun formatHeroDuration(minutes: Int): String {
    val totalSeconds = minutes * 60
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}

private fun formatCycleTotal(totalMinutes: Int): String {
    return if (totalMinutes >= 60) {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        if (m == 0) "${h}h" else "${h}h ${m}m"
    } else {
        "${totalMinutes}m"
    }
}
