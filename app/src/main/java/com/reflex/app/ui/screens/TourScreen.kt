package com.reflex.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.reflex.app.data.Priority
import com.reflex.app.ui.components.LiquidTimer
import com.reflex.app.ui.components.ReflexMark
import com.reflex.app.ui.components.SettingsIcon
import com.reflex.app.ui.theme.DarkSettingsColors
import com.reflex.app.ui.theme.LightSettingsColors
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.SettingsColorTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.viewmodel.TourViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DecelerateEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("BatteryLife")
@Composable
fun TourScreen(
    isReplay: Boolean = false,
    onFinishOnboarding: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TourViewModel = viewModel()
) {
    SetStatusBarAppearance()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) DarkSettingsColors else LightSettingsColors

    val totalPages = if (isReplay) 5 else 6
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // LifeCycle observer to refresh permissions when coming back from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Page-specific timers start/stop
    LaunchedEffect(pagerState.currentPage) {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        when (pagerState.currentPage) {
            1 -> {
                viewModel.stopTypewriter()
                viewModel.stopFocusDemo()
                viewModel.startRoutineDemo()
            }
            2 -> {
                viewModel.stopRoutineDemo()
                viewModel.stopFocusDemo()
                viewModel.startTypewriter()
            }
            3 -> {
                viewModel.stopRoutineDemo()
                viewModel.stopTypewriter()
                viewModel.stopFocusDemo()
                viewModel.resetHabitsDemo()
            }
            4 -> {
                viewModel.stopRoutineDemo()
                viewModel.stopTypewriter()
                viewModel.startFocusDemo()
            }
            5 -> {
                viewModel.stopRoutineDemo()
                viewModel.stopTypewriter()
                viewModel.stopFocusDemo()
                viewModel.refreshPermissions()
            }
            else -> {
                viewModel.stopRoutineDemo()
                viewModel.stopTypewriter()
                viewModel.stopFocusDemo()
            }
        }
    }

    // Permission launchers
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions()
    }

    val requestPermissionAction: (Int) -> Unit = { index ->
        when (index) {
            0 -> {
                // Notifications
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                }
            }
            1 -> {
                // Exact alarms
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
            2 -> {
                // Battery unrestricted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(fallback)
                    }
                }
            }
        }
    }

    BackHandler(enabled = true) {
        if (viewModel.showCompletionScreen) {
            if (isReplay) {
                onNavigateToSettings()
            } else {
                viewModel.replayTour()
            }
        } else if (pagerState.currentPage > 0) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        } else if (isReplay) {
            onNavigateToSettings()
        }
    }

    val isKeyboardOpen = WindowInsets.isImeVisible

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Row: Brand on left, Skip pill on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReflexMark(size = 26.dp, animated = false, tint = colors.copper)
                    Text(
                        text = "Reflex",
                        fontFamily = Lora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = colors.ink
                    )
                }

                // Skip pill (hidden on last page)
                val isLastPage = pagerState.currentPage == totalPages - 1
                AnimatedVisibility(
                    visible = !isLastPage && !viewModel.showCompletionScreen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(CircleShape)
                            .background(colors.input)
                            .clickable {
                                if (isReplay) {
                                    onNavigateToSettings()
                                } else {
                                    viewModel.showDoneOverlay()
                                }
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Stage: Horizontal Pager for the pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> TourWelcomePage(viewModel = viewModel, colors = colors)
                    1 -> TourRoutinesPage(viewModel = viewModel, colors = colors)
                    2 -> TourCapturePage(viewModel = viewModel, colors = colors)
                    3 -> TourHabitsPage(viewModel = viewModel, colors = colors)
                    4 -> TourFocusPage(viewModel = viewModel, colors = colors)
                    5 -> TourPermissionsPage(
                        viewModel = viewModel,
                        colors = colors,
                        onRequestPermission = requestPermissionAction
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dots row: 5 or 6 dots
            Row(
                modifier = Modifier
                    .padding(bottom = if (isKeyboardOpen) 6.dp else 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = tween(durationMillis = 350, easing = DecelerateEasing),
                        label = "DotWidth_$index"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.copper else colors.input)
                    )
                }
            }

            // Footer action buttons
            if (pagerState.currentPage < totalPages - 1) {
                // Pages before last: Back & Continue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button (invisible on page 0 unless isReplay)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isKeyboardOpen) 48.dp else 56.dp)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage > 0 || isReplay) colors.input else Color.Transparent)
                            .then(
                                if (pagerState.currentPage > 0) {
                                    Modifier.clickable {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                } else if (isReplay) {
                                    Modifier.clickable {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        onNavigateToSettings()
                                    }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (pagerState.currentPage > 0 || isReplay) {
                            Text(
                                text = "Back",
                                fontFamily = Lora,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                                color = colors.ink
                            )
                        }
                    }

                    // Continue button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(if (isKeyboardOpen) 48.dp else 56.dp)
                            .clip(CircleShape)
                            .background(colors.pill)
                            .clickable {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Continue",
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = colors.pillFg
                        )
                    }
                }
            } else if (isReplay) {
                // Replay Tour: Last page (Focus) -> Back or Finish (skip permissions, return to settings)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(CircleShape)
                            .background(colors.input)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Back",
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = colors.ink
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(CircleShape)
                            .background(colors.pill)
                            .clickable {
                                onNavigateToSettings()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Finish",
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = colors.pillFg
                        )
                    }
                }
            } else {
                // Page 5: Permissions
                val allAllowed = viewModel.permissionStatus.allAllowed
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!allAllowed) {
                        // "Set up now in settings" button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(colors.pill)
                            .clickable {
                                viewModel.simulateAllowAll {
                                    viewModel.showDoneOverlay()
                                }
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Set up now in settings",
                                fontFamily = Lora,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                                color = colors.pillFg
                            )
                        }

                        // "Start using Reflex" secondary button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(colors.input)
                                .clickable {
                                    viewModel.showDoneOverlay()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start using Reflex",
                                fontFamily = Lora,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                                color = colors.ink
                            )
                        }
                    } else {
                        // "Start using Reflex" primary button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(CircleShape)
                                .background(colors.pill)
                                .clickable {
                                    viewModel.showDoneOverlay()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start using Reflex",
                                fontFamily = Lora,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                                color = colors.pillFg
                            )
                        }
                    }
                }
            }
        }

        // Completion / Done Overlay
        AnimatedVisibility(
            visible = viewModel.showCompletionScreen,
            enter = fadeIn(tween(350)),
            exit = fadeOut(tween(250))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 430.dp)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReflexMark(size = 132.dp, animated = true, showGlowRings = false, tint = colors.copper)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = viewModel.completionGreeting,
                        fontFamily = Lora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = colors.ink,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your first routine is one tap away.",
                        fontFamily = Lora,
                        fontSize = 15.sp,
                        color = colors.secondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary: Start using Reflex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(colors.pill)
                            .clickable {
                                if (isReplay) {
                                    onNavigateToSettings()
                                } else {
                                    viewModel.completeOnboarding {
                                        onFinishOnboarding()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isReplay) "Done" else "Start using Reflex",
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = colors.pillFg
                        )
                    }

                    // Secondary: Replay tour
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(colors.input)
                            .clickable {
                                viewModel.replayTour()
                                coroutineScope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Replay tour",
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = colors.ink
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// PAGE 0: WELCOME
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TourWelcomePage(
    viewModel: TourViewModel,
    colors: SettingsColorTokens
) {
    val isKeyboardOpen = WindowInsets.isImeVisible
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val heroSize by animateDpAsState(
        targetValue = if (isKeyboardOpen) 72.dp else 210.dp,
        animationSpec = tween(durationMillis = 300, easing = DecelerateEasing),
        label = "heroSize"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isKeyboardOpen) Arrangement.Center else Arrangement.Top
    ) {
        if (!isKeyboardOpen) {
            // Visual Hero: 210dp ReflexMark with animated waves, glow rings & breathing halo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ReflexMark(
                    size = 210.dp,
                    animated = true,
                    showGlowRings = true,
                    tint = colors.copper
                )
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            ReflexMark(
                size = heroSize,
                animated = false,
                showGlowRings = false,
                tint = colors.copper
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Copy block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = if (isKeyboardOpen) 4.dp else 14.dp)
                .padding(bottom = if (isKeyboardOpen) 8.dp else 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Reflex",
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = if (isKeyboardOpen) 22.sp else 28.sp,
                lineHeight = if (isKeyboardOpen) 26.sp else 33.sp,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            if (!isKeyboardOpen) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Routines, tasks and focus in one calm space that never leaves your phone.",
                    fontFamily = Lora,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = colors.secondary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(if (isKeyboardOpen) 10.dp else 18.dp))

            // User Name Input Field
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.input)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (viewModel.userName.isEmpty()) {
                    Text(
                        text = "What should we call you?",
                        fontFamily = Lora,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        color = colors.tertiary
                    )
                }
                BasicTextField(
                    value = viewModel.userName,
                    onValueChange = { viewModel.updateUserName(it) },
                    textStyle = TextStyle(
                        fontFamily = Lora,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = colors.ink
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        }
                    ),
                    cursorBrush = SolidColor(colors.copper),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Live Greeting
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = viewModel.greetingText,
                fontFamily = Lora,
                fontSize = 14.sp,
                color = colors.copper,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// PAGE 1: ROUTINES
// =========================================================================

@Composable
private fun TourRoutinesPage(
    viewModel: TourViewModel,
    colors: SettingsColorTokens
) {
    val s = viewModel.routineStep
    val d1 = s >= 1
    val d2 = s >= 3
    val reps = when {
        s >= 6 -> 3
        s == 5 -> 2
        s == 4 -> 1
        else -> 0
    }
    val p2 = when {
        d2 -> 100f
        s == 2 -> 55f
        else -> 0f
    }
    val streak = if (s >= 6) 6 else 5

    val streakScale by animateFloatAsState(
        targetValue = if (viewModel.routineStreakPopped) 1.12f else 1.0f,
        animationSpec = tween(500),
        label = "RoutineStreakScale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TourCard(colors = colors) {
                // Routine Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TourTile(icon = "timer", colors = colors)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Morning power reset",
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                            color = colors.ink
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.scale(streakScale)
                        ) {
                            SettingsIcon(name = "fire", tint = colors.copper, size = 15.dp)
                            Text(
                                text = "$streak-day streak",
                                fontFamily = Lora,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.copper
                            )
                        }
                    }
                }

                // Step 1: Hydrate
                TourRoutineStepRow(
                    done = d1,
                    title = "Hydrate",
                    subtitle = "Check off",
                    pct = if (d1) 100f else 0f,
                    rightText = if (d1) "Done" else "Tap",
                    colors = colors
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.border.copy(alpha = 0.35f))
                )

                // Step 2: Stretch
                TourRoutineStepRow(
                    done = d2,
                    title = "Stretch",
                    subtitle = "Timer · 5:00",
                    pct = p2,
                    rightText = if (d2) "Done" else if (s == 2) "2:45" else "5:00",
                    colors = colors
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(colors.border.copy(alpha = 0.35f))
                )

                // Step 3: Squats
                TourRoutineStepRow(
                    done = reps == 3,
                    title = "Squats",
                    subtitle = "Reps · 3 × 10",
                    pct = reps / 3f * 100f,
                    rightText = "$reps/3",
                    colors = colors
                )
            }
        }

        // Copy block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Unbreakable routines",
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Chain timed steps, check-offs and reps. Rest gaps and streak protection run themselves.",
                fontFamily = Lora,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TourRoutineStepRow(
    done: Boolean,
    title: String,
    subtitle: String,
    pct: Float,
    rightText: String,
    colors: SettingsColorTokens
) {
    val barWidthPct by animateFloatAsState(
        targetValue = pct.coerceIn(0f, 100f) / 100f,
        animationSpec = tween(durationMillis = 600, easing = DecelerateEasing),
        label = "RoutineStepProgress_$title"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Circle indicator
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (done) colors.copper else Color.Transparent)
                .border(2.dp, if (done) colors.copper else colors.tertiary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                SettingsIcon(name = "check", tint = colors.onCopper, size = 16.dp)
            }
        }

        // Title + subtitle + progress bar
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = colors.ink
            )
            Text(
                text = subtitle,
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = colors.secondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Progress bar (5dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.input)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barWidthPct)
                        .height(5.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.copper)
                )
            }
        }

        // Right status text
        Text(
            text = rightText,
            fontFamily = Lora,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (done) colors.copper else colors.secondary
        )
    }
}

// =========================================================================
// PAGE 2: TASK CAPTURE
// =========================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TourCapturePage(
    viewModel: TourViewModel,
    colors: SettingsColorTokens
) {
    val parsed = viewModel.parsedResult
    val isHigh = parsed.priority == Priority.HIGH
    val isMed = parsed.priority == Priority.MEDIUM

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual: Input Card + Task Row Preview + Hint
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TourCard(colors = colors) {
                // Input text field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.input)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (viewModel.captureInput.isEmpty()) {
                        Text(
                            text = "Try: call mom friday 6pm !",
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            color = colors.tertiary
                        )
                    }
                    BasicTextField(
                        value = viewModel.captureInput,
                        onValueChange = { viewModel.setCaptureText(it) },
                        textStyle = TextStyle(
                            fontFamily = Lora,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = colors.ink
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(colors.copper),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Extracted Chips
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.extractedChips.forEach { chipText ->
                        val isChipHigh = chipText.contains("High", ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(if (isChipHigh) colors.redBg else colors.chip)
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = chipText,
                                fontFamily = Lora,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (isChipHigh) colors.red else colors.copper
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Task Row Preview (26dp radius, Surface, 1dp border, min-height 72dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(26.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Circle checkbox
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            when {
                                isHigh -> colors.red
                                isMed -> colors.copper
                                else -> colors.tertiary
                            },
                            CircleShape
                        )
                )

                // Task Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parsed.title.ifBlank { "Untitled task" },
                        fontFamily = Lora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colors.ink
                    )
                    Text(
                        text = buildString {
                            val parts = mutableListOf<String>()
                            parsed.dueDate?.let { epochMillis ->
                                try {
                                    val date = java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                                    val today = java.time.LocalDate.now()
                                    val label = when {
                                        date == today -> "Today"
                                        date == today.plusDays(1) -> "Tomorrow"
                                        date == today.plusWeeks(1) -> "Next week"
                                        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE", java.util.Locale.ENGLISH))
                                    }
                                    parts.add(label)
                                } catch (_: Exception) {}
                            }
                            parsed.dueTime?.let { timeMillis ->
                                try {
                                    val time = java.time.Instant.ofEpochMilli(timeMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                                    parts.add(time.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a", java.util.Locale.ENGLISH)).lowercase())
                                } catch (_: Exception) {}
                            }
                            if (parts.isEmpty()) append("No date") else append(parts.joinToString(" · "))
                        },
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        color = colors.secondary
                    )
                }

                // Priority Badge Pill
                if (isHigh || isMed) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(if (isHigh) colors.redBg else colors.chip)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isHigh) "High" else "Medium",
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (isHigh) colors.red else colors.copper
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Edit the text and watch it parse",
                fontFamily = Lora,
                fontSize = 13.sp,
                color = colors.tertiary
            )
        }

        // Copy block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Lightning task capture",
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Type in plain English or speak. Dates, times, durations and priorities are picked out for you.",
                fontFamily = Lora,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// PAGE 3: HABITS
// =========================================================================

@Composable
private fun TourHabitsPage(
    viewModel: TourViewModel,
    colors: SettingsColorTokens
) {
    val doneCount = viewModel.habitDoneCount
    val streak = viewModel.habitStreakCount

    val streakScale by animateFloatAsState(
        targetValue = if (viewModel.habitStreakPopped) 1.12f else 1.0f,
        animationSpec = tween(500),
        label = "HabitStreakScale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual Card + Hint
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TourCard(colors = colors) {
                // Header: 72dp Ring + Streak + Weekdays
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 72dp progress ring
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 7.dp.toPx()
                            val arcRadius = (size.minDimension - strokeWidth) / 2f
                            val centerOffset = Offset(size.width / 2f, size.height / 2f)

                            // Track
                            drawCircle(
                                color = colors.input,
                                radius = arcRadius,
                                center = centerOffset,
                                style = Stroke(width = strokeWidth)
                            )

                            // Progress
                            val sweepAngle = 360f * (doneCount / 3f)
                            drawArc(
                                color = colors.copper,
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(centerOffset.x - arcRadius, centerOffset.y - arcRadius),
                                size = Size(arcRadius * 2, arcRadius * 2),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                        Text(
                            text = "$doneCount/3",
                            fontFamily = Lora,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.ink
                        )
                    }

                    // Streak count & Weekday dots
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.scale(streakScale)
                        ) {
                            Text(
                                text = "$streak",
                                fontFamily = Lora,
                                fontWeight = FontWeight.Bold,
                                fontSize = 34.sp,
                                color = colors.copper
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "day streak",
                                fontFamily = Lora,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.secondary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Weekday pills: M T W T F S S
                        val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            weekdays.forEachIndexed { idx, label ->
                                val isOn = idx < 5 || (idx == 5 && doneCount > 0)
                                val isToday = idx == 5
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(if (isOn) colors.copper else colors.input)
                                        .then(
                                            if (isToday) Modifier.border(2.dp, colors.copper, CircleShape) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontFamily = Lora,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isOn) colors.onCopper else colors.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                // Habit 1: Meditate (Check-off)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { viewModel.toggleMeditate() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val done = viewModel.habitMeditateDone
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (done) colors.copper else Color.Transparent)
                            .border(2.dp, if (done) colors.copper else colors.tertiary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            SettingsIcon(name = "check", tint = colors.onCopper, size = 16.dp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Meditate",
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = colors.ink
                        )
                        Text(
                            text = "Check off · 10 min",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = colors.secondary
                        )
                    }

                    Text(
                        text = if (done) "Done" else "Tap",
                        fontFamily = Lora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (done) colors.copper else colors.secondary
                    )
                }

                // Habit 2: Drink water (Measurable with steppers)
                val waterDone = viewModel.habitWaterGlasses >= 8
                val waterPct = (viewModel.habitWaterGlasses / 8f).coerceAtMost(1f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (waterDone) colors.copper else Color.Transparent)
                            .border(2.dp, if (waterDone) colors.copper else colors.tertiary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (waterDone) {
                            SettingsIcon(name = "check", tint = colors.onCopper, size = 16.dp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Drink water",
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = colors.ink
                        )
                        Text(
                            text = "${viewModel.habitWaterGlasses} / 8 glasses",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = colors.secondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.input)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(waterPct)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(colors.copper)
                            )
                        }
                    }

                    // Steppers - and +
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TourStepperButton(label = "−", onClick = { viewModel.adjustWater(-1) }, colors = colors)
                        TourStepperButton(label = "+", onClick = { viewModel.adjustWater(1) }, colors = colors)
                    }
                }

                // Habit 3: Screen time (Limit with steppers)
                val st = viewModel.habitScreenTimeHours
                val stDone = st > 0f && st <= 3.0f
                val stOver = st > 3.0f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (stDone) colors.copper else Color.Transparent)
                            .border(2.dp, if (stDone) colors.copper else colors.tertiary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (stDone) {
                            SettingsIcon(name = "check", tint = colors.onCopper, size = 16.dp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Screen time",
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = colors.ink
                        )
                        Text(
                            text = buildString {
                                if (st > 0f) {
                                    append("${String.format(Locale.ROOT, "%.1f", st)} h · max 3 h")
                                    if (stOver) append(" · over")
                                } else {
                                    append("Not logged · max 3 h")
                                }
                            },
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            color = if (stOver) colors.red else colors.secondary
                        )
                    }

                    // Steppers - and +
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TourStepperButton(label = "−", onClick = { viewModel.adjustScreenTime(-0.5f) }, colors = colors)
                        TourStepperButton(label = "+", onClick = { viewModel.adjustScreenTime(0.5f) }, colors = colors)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tap a habit or use the steppers",
                fontFamily = Lora,
                fontSize = 13.sp,
                color = colors.tertiary
            )
        }

        // Copy block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Habits that stick",
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Check-offs, measurable goals and limits, tracked with streaks and a monthly consistency view.",
                fontFamily = Lora,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TourStepperButton(
    label: String,
    onClick: () -> Unit,
    colors: SettingsColorTokens
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(colors.input)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = Lora,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            color = colors.ink
        )
    }
}

// =========================================================================
// PAGE 4: FOCUS & APP BLOCKING
// =========================================================================

@Composable
private fun TourFocusPage(
    viewModel: TourViewModel,
    colors: SettingsColorTokens
) {
    val block = viewModel.appBlockingEnabled

    // Ultra-smooth, mathematically constant liquid draining animation (no stepped layers)
    val cycleMillis = 25000 // 25s for 25:00 at 60x speed
    val transition = rememberInfiniteTransition(label = "TourFocusTransition")
    val liquidProgress by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TourFocusProgress"
    )

    val currentSeconds = (liquidProgress * 1500).toInt().coerceIn(0, 1500)
    val displayMins = currentSeconds / 60
    val timeText = "$displayMins:00"

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual: LiquidTimer (178dp) + App blocking card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LiquidTimer 178dp with constant continuous fluid level
            LiquidTimer(
                ratio = { liquidProgress },
                timeText = timeText,
                label = "Focus",
                status = "",
                isBreak = false,
                isActive = true,
                modifier = Modifier.size(178.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // App blocking card
            TourCard(colors = colors) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TourTile(icon = "block", colors = colors)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App blocking",
                            fontFamily = Lora,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            lineHeight = 21.sp,
                            color = colors.ink
                        )
                        Text(
                            text = if (block) "Restricted until $timeText ends" else "Every app stays available",
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            color = colors.secondary
                        )
                    }

                    // Switch 52x30
                    TourSwitch(
                        checked = block,
                        onCheckedChange = { viewModel.toggleAppBlocking() },
                        colors = colors
                    )
                }

                // App chips
                val apps = listOf("Instagram", "YouTube", "Reddit", "X")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    apps.forEach { appName ->
                        Row(
                            modifier = Modifier
                                .height(34.dp)
                                .clip(CircleShape)
                                .background(colors.input)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (block) {
                                SettingsIcon(name = "lock", tint = colors.copper, size = 14.dp)
                            }
                            Text(
                                text = appName,
                                fontFamily = Lora,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (block) colors.tertiary else colors.ink,
                                textDecoration = if (block) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }

        // Copy block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Focus and app blocking",
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Run classic Pomodoro or open-ended flow while Reflex holds back the apps that pull you away.",
                fontFamily = Lora,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =========================================================================
// PAGE 5: PERMISSIONS
// =========================================================================

@Composable
private fun TourPermissionsPage(
    viewModel: TourViewModel,
    colors: SettingsColorTokens,
    onRequestPermission: (Int) -> Unit
) {
    val status = viewModel.permissionStatus
    val allowedCount = status.allowedCount
    val allAllowed = status.allAllowed

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual: 108dp Circular ring + Hint + Card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero progress ring 108dp
            Box(
                modifier = Modifier.size(108.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    val arcRadius = (size.minDimension - strokeWidth) / 2f
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)

                    // Track
                    drawCircle(
                        color = colors.input,
                        radius = arcRadius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidth)
                    )

                    // Progress
                    val sweepAngle = 360f * (allowedCount / 3f)
                    drawArc(
                        color = colors.copper,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(centerOffset.x - arcRadius, centerOffset.y - arcRadius),
                        size = Size(arcRadius * 2, arcRadius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                SettingsIcon(
                    name = if (allAllowed) "check" else "shield",
                    tint = colors.copper,
                    size = 40.dp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (allAllowed) "All set" else "$allowedCount of 3 allowed",
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.secondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Card: 3 permissions rows
            TourCard(colors = colors) {
                // Permission 0: Notifications
                TourPermissionRow(
                    title = "Notifications",
                    subtitle = "Routine, task and focus alerts",
                    icon = "bell",
                    isAllowed = status.notifAllowed,
                    onAllow = { onRequestPermission(0) },
                    colors = colors
                )

                // Permission 1: Exact alarms
                TourPermissionRow(
                    title = "Exact alarms",
                    subtitle = "Fire at the exact minute",
                    icon = "alarm",
                    isAllowed = status.exactAlarmAllowed,
                    onAllow = { onRequestPermission(1) },
                    colors = colors
                )

                // Permission 2: Battery unrestricted
                TourPermissionRow(
                    title = "Battery unrestricted",
                    subtitle = "Keeps timers alive in the background",
                    icon = "bat",
                    isAllowed = status.batUnrestricted,
                    onAllow = { onRequestPermission(2) },
                    colors = colors
                )
            }
        }

        // Copy block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reliability and permissions",
                fontFamily = Lora,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                lineHeight = 33.sp,
                color = colors.ink,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "So alarms and reminders fire on time, even through Android Doze. Everything stays on this device.",
                fontFamily = Lora,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.secondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TourPermissionRow(
    title: String,
    subtitle: String,
    icon: String,
    isAllowed: Boolean,
    onAllow: () -> Unit,
    colors: SettingsColorTokens
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TourTile(icon = icon, colors = colors)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Lora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = colors.ink
            )
            Text(
                text = subtitle,
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = colors.secondary
            )
        }

        if (isAllowed) {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(colors.okBg)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Allowed",
                    fontFamily = Lora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.ok
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(colors.chip)
                    .clickable(onClick = onAllow)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Allow",
                    fontFamily = Lora,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.copper
                )
            }
        }
    }
}

// =========================================================================
// REUSABLE TOUR UI COMPONENTS
// =========================================================================

@Composable
private fun TourCard(
    colors: SettingsColorTokens,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(32.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        content()
    }
}

@Composable
private fun TourTile(
    icon: String,
    colors: SettingsColorTokens
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.chip),
        contentAlignment = Alignment.Center
    ) {
        SettingsIcon(name = icon, tint = colors.copper, size = 22.dp)
    }
}

@Composable
private fun TourSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: SettingsColorTokens
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 26.dp else 4.dp,
        animationSpec = tween(200),
        label = "TourSwitchOffset"
    )

    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(if (checked) colors.copper else colors.input)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch
            ) {
                onCheckedChange(!checked)
            }
            .semantics {
                contentDescription = if (checked) "On" else "Off"
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) colors.onCopper else colors.tertiary)
        )
    }
}
