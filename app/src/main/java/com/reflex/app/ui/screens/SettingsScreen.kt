package com.reflex.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.room.withTransaction
import com.reflex.app.ReflexApplication
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.SettingItem
import com.reflex.app.data.SettingItemType
import com.reflex.app.data.SettingsSchema
import com.reflex.app.ui.components.AppPickerSheet
import com.reflex.app.ui.components.SettingsIcon
import com.reflex.app.ui.theme.AppTheme
import com.reflex.app.ui.theme.DarkSettingsColors
import com.reflex.app.ui.theme.LightSettingsColors
import com.reflex.app.ui.theme.LocalSettingsColors
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.SettingsColorTokens
import com.reflex.app.ui.theme.SettingsTheme
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.AppBlockPermissionHelper
import com.reflex.app.util.AppLog
import com.reflex.app.util.AppStorageStats
import com.reflex.app.util.CalendarPreferenceRepository
import com.reflex.app.util.CalendarProviderHelper
import com.reflex.app.util.DataExportImportManager
import com.reflex.app.util.DeviceCalendar
import com.reflex.app.util.OnboardingManager
import com.reflex.app.util.PermissionBadgeState
import com.reflex.app.util.PermissionDetail
import com.reflex.app.util.SettingsRepository
import com.reflex.app.util.ThemePreferenceRepository
import com.reflex.app.util.UserProfileRepository
import java.io.ByteArrayInputStream
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Pixel-faithful Jetpack Compose implementation of reflex-settings.html.
 * Single source of truth for design, layout, copy, behavior, and defaults.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    initialSubScreen: String? = null,
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToHelpGuide: (() -> Unit)? = null,
    onReplayOnboarding: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val activeTheme by ThemePreferenceRepository.currentTheme.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (activeTheme) {
        AppTheme.SYSTEM -> isSystemDark
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }

    SetStatusBarAppearance(isLightBackground = !isDark)

    val colors = if (isDark) DarkSettingsColors else LightSettingsColors

    // Live Settings state
    val settingsValues by SettingsRepository.values.collectAsState()
    val permissionStates by SettingsRepository.permissionStates.collectAsState()
    val permissionDetails by SettingsRepository.permissionDetails.collectAsState()

    // Navigation Stack: 'home' is root, sub-screens pushed on top
    val stack = remember { mutableStateListOf("home") }
    LaunchedEffect(initialSubScreen) {
        if (!initialSubScreen.isNullOrBlank() && SettingsSchema.SC.containsKey(initialSubScreen)) {
            if (stack.size == 1 && stack[0] == "home") {
                stack.add(initialSubScreen)
            }
        }
    }

    // Refresh permissions on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                SettingsRepository.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Back handling
    val currentScreenId = stack.lastOrNull() ?: "home"
    BackHandler(enabled = true) {
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else {
            onNavigateBack?.invoke()
        }
    }

    // Toast overlay state
    var toastMessage by remember { mutableStateOf<String?>(null) }
    fun showToast(msg: String) {
        toastMessage = msg
        coroutineScope.launch {
            delay(2300)
            if (toastMessage == msg) {
                toastMessage = null
            }
        }
    }

    // Confirmation & Action Dialog States
    var showResetDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showCalendarPickerSheet by remember { mutableStateOf(false) }
    var showAppPickerSheet by remember { mutableStateOf(false) }

    val storageStats by SettingsRepository.storageStats.collectAsState()
    LaunchedEffect(Unit) {
        SettingsRepository.refreshStorageStats(context)
    }

    // SAF Launchers for Export and Import
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val app = context.applicationContext as ReflexApplication
                val success = DataExportImportManager.exportDataToUri(context, app.repository, uri)
                showToast(if (success) "Backup saved successfully" else "Failed to export backup")
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val app = context.applicationContext as ReflexApplication
                val success = DataExportImportManager.importDataFromUri(context, app.repository, uri)
                if (success) {
                    SettingsRepository.refreshStorageStats(context)
                }
                showToast(if (success) "Backup restored successfully" else "Failed to restore backup")
            }
        }
    }

    val autoBackupFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                SettingsRepository.setAutoBackupUri(context, uri)
                SettingsRepository.setBoolean(context, "d_auto", true)

                val periodicWork = androidx.work.PeriodicWorkRequestBuilder<com.reflex.app.service.AutoBackupWorker>(
                    7, java.util.concurrent.TimeUnit.DAYS
                ).build()
                androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    com.reflex.app.service.AutoBackupWorker.UNIQUE_WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                    periodicWork
                )
                showToast("Weekly auto-backup enabled")
            } catch (e: Exception) {
                com.reflex.app.util.AppLog.e("SettingsScreen", "Failed to secure folder permission for auto backup", e)
                SettingsRepository.setBoolean(context, "d_auto", false)
                showToast("Failed to secure folder permission")
            }
        } else {
            SettingsRepository.setBoolean(context, "d_auto", false)
        }
    }

    var hasRequestedNotifPermissionOnce by remember { mutableStateOf(false) }
    var hasRequestedCalendarPermissionOnce by remember { mutableStateOf(false) }

    // Permission Request Launchers
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        SettingsRepository.refreshPermissions(context)
        if (!isGranted) {
            val activity = context.findActivity()
            if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
            ) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        SettingsRepository.refreshPermissions(context)
        val allGranted = results.values.all { it }
        if (!allGranted) {
            val activity = context.findActivity()
            val permanentlyDenied = activity != null && results.any { (perm, granted) ->
                !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }
            if (permanentlyDenied) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    android.widget.Toast.makeText(context, "Enable Calendar in app settings", android.widget.Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        SettingsRepository.refreshPermissions(context)
    }

    fun requestOrOpenPermission(key: String) {
        when (key) {
            "p_not" -> {
                val notifDetail = permissionDetails["p_not"]
                if (notifDetail?.state == PermissionBadgeState.PARTIAL && notifDetail.blockedChannelId != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                putExtra(Settings.EXTRA_CHANNEL_ID, notifDetail.blockedChannelId)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            openAppNotificationSettings(context)
                        }
                    } else {
                        openAppNotificationSettings(context)
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        if (perm != PackageManager.PERMISSION_GRANTED) {
                            val activity = context.findActivity()
                            val permanentlyDenied = activity != null &&
                                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS) &&
                                hasRequestedNotifPermissionOnce
                            if (permanentlyDenied) {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    openAppNotificationSettings(context)
                                }
                            } else {
                                hasRequestedNotifPermissionOnce = true
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            openAppNotificationSettings(context)
                        }
                    } else {
                        openAppNotificationSettings(context)
                    }
                }
            }
            "p_alm" -> {
                // Uses alarm clock scheduling, no permission needed
                showToast("Uses alarm clock scheduling, no permission needed")
            }
            "p_cal" -> {
                val hasRead = CalendarProviderHelper.hasReadPermission(context)
                val hasWrite = CalendarProviderHelper.hasWritePermission(context)
                val missing = mutableListOf<String>()
                if (!hasRead) missing.add(Manifest.permission.READ_CALENDAR)
                if (!hasWrite) missing.add(Manifest.permission.WRITE_CALENDAR)
                if (missing.isNotEmpty()) {
                    val activity = context.findActivity()
                    val permanentlyDenied = activity != null && missing.any { perm ->
                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm) && hasRequestedCalendarPermissionOnce
                    }
                    if (permanentlyDenied) {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            android.widget.Toast.makeText(context, "Enable Calendar in app settings", android.widget.Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            calendarPermissionLauncher.launch(missing.toTypedArray())
                        }
                    } else {
                        hasRequestedCalendarPermissionOnce = true
                        calendarPermissionLauncher.launch(missing.toTypedArray())
                    }
                }
            }
            "p_mic" -> {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
            "p_blk" -> {
                AppBlockPermissionHelper.openUsageAccessSettings(context)
            }
            "p_ovr" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(fallback)
                        } catch (_: Exception) {}
                    }
                }
            }
            "p_bat" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(fallback)
                        } catch (_: Exception) {
                            try {
                                val generalSettings = Intent(Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(generalSettings)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    // Insets
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val homeScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    CompositionLocalProvider(LocalSettingsColors provides colors) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.bg)
        ) {
            // Centered max-width 430dp container
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 430.dp)
                        .fillMaxSize()
                ) {
                    // Pinned Top Bar (56dp height + status bar inset)
                    SettingsTopBar(
                        title = if (currentScreenId == "home") "Settings" else SettingsSchema.SC[currentScreenId]?.title ?: "Settings",
                        onBackClick = {
                            if (stack.size > 1) {
                                stack.removeAt(stack.lastIndex)
                            } else {
                                onNavigateBack?.invoke()
                            }
                        }
                    )

                    // Animated screen view (22dp horizontal slide + fade, 280ms cubic-bezier)
                    val slideDistance = with(density) { 22.dp.roundToPx() }
                    val bezierEasing = remember { CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f) }

                    AnimatedContent(
                        targetState = currentScreenId,
                        transitionSpec = {
                            (slideInHorizontally(animationSpec = tween(280, easing = bezierEasing)) { slideDistance } +
                                    fadeIn(animationSpec = tween(280, easing = bezierEasing)))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = tween(280, easing = bezierEasing)) { -slideDistance } +
                                            fadeOut(animationSpec = tween(280, easing = bezierEasing))
                                )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        label = "SettingsScreenTransition"
                    ) { targetId ->
                        if (targetId == "home") {
                            SettingsHomeScreenContent(
                                colors = colors,
                                activeTheme = activeTheme,
                                onSelectTheme = { theme ->
                                    ThemePreferenceRepository.setTheme(context, theme)
                                    SettingsRepository.setString(
                                        context,
                                        "theme",
                                        when (theme) {
                                            AppTheme.DARK -> "dark"
                                            AppTheme.LIGHT -> "light"
                                            else -> "system"
                                        }
                                    )
                                },
                                settingsValues = settingsValues,
                                permissionStates = permissionStates,
                                onOpenScreen = { id -> stack.add(id) },
                                onShowToast = { showToast(it) },
                                onReplayTour = {
                                    if (onReplayOnboarding != null) {
                                        onReplayOnboarding.invoke()
                                    } else {
                                        showToast("App tour started")
                                    }
                                },
                                bottomPadding = 40.dp + navBarInset,
                                scrollState = homeScrollState
                            )
                        } else {
                            val screenDef = SettingsSchema.SC[targetId]
                            if (screenDef != null) {
                                SettingsGenericSubScreenContent(
                                    screenDef = screenDef,
                                    colors = colors,
                                    settingsValues = settingsValues,
                                    permissionStates = permissionStates,
                                    permissionDetails = permissionDetails,
                                    storageStats = storageStats,
                                    onToggle = { k, v ->
                                        if (k == "d_auto") {
                                            if (v) {
                                                autoBackupFolderLauncher.launch(null)
                                            } else {
                                                SettingsRepository.setBoolean(context, "d_auto", false)
                                                androidx.work.WorkManager.getInstance(context).cancelUniqueWork(
                                                    com.reflex.app.service.AutoBackupWorker.UNIQUE_WORK_NAME
                                                )
                                                showToast("Weekly auto-backup disabled")
                                            }
                                        } else {
                                            SettingsRepository.setBoolean(context, k, v)
                                        }
                                    },
                                    onSelectSegment = { k, v -> SettingsRepository.setString(context, k, v) },
                                    onStep = { k, d, min, max, step -> SettingsRepository.stepInt(context, k, d, min, max, step) },
                                    onPermissionAction = { k -> requestOrOpenPermission(k) },
                                    onLinkAction = { item ->
                                        when (item.key) {
                                            "export" -> exportLauncher.launch("reflex_backup.json")
                                            "import" -> importLauncher.launch(arrayOf("application/json", "*/*"))
                                            "backup_now" -> {
                                                val treeUri = SettingsRepository.getAutoBackupUri(context)
                                                if (treeUri == null) {
                                                    autoBackupFolderLauncher.launch(null)
                                                } else {
                                                    val oneTime = androidx.work.OneTimeWorkRequestBuilder<com.reflex.app.service.AutoBackupWorker>().build()
                                                    androidx.work.WorkManager.getInstance(context).enqueue(oneTime)
                                                    showToast("Backup initiated")
                                                }
                                            }
                                            "choose_calendars" -> showCalendarPickerSheet = true
                                            "configure_blocking" -> showAppPickerSheet = true
                                            "licenses" -> showLicensesDialog = true
                                            "feedback" -> sendFeedbackEmail(context)
                                            "github_issues" -> openBrowserUrl(context, com.reflex.app.util.AppConstants.GITHUB_ISSUES_URL)
                                            "source_code" -> openBrowserUrl(context, com.reflex.app.util.AppConstants.GITHUB_REPO_URL)
                                            else -> {
                                                if (item.toast.isNotBlank()) showToast(item.toast)
                                            }
                                        }
                                    },
                                    onDangerAction = {
                                        showResetDialog = true
                                    },
                                    bottomPadding = 40.dp + navBarInset
                                )
                            }
                        }
                    }
                }
            }

            // Fixed Toast Overlay (bottom-center, 30dp + navBarInset)
            val toastSlidePx = with(density) { 10.dp.roundToPx() }
            AnimatedVisibility(
                visible = toastMessage != null,
                enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { toastSlidePx },
                exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { toastSlidePx },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp + navBarInset)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.pill)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = toastMessage.orEmpty(),
                        fontFamily = Lora,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.pillFg,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Reset All Data Confirmation Dialog
    if (showResetDialog) {
        Dialog(onDismissRequest = { showResetDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(colors.surface)
                    .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Reset all data?",
                        fontFamily = Lora,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.red
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deletes all routines, tasks, habits, and focus history on this device. This cannot be undone.",
                        fontFamily = Lora,
                        fontSize = 14.sp,
                        color = colors.secondary,
                        lineHeight = 19.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Cancel button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(colors.input)
                                .clickable { showResetDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ink
                            )
                        }

                        // Reset button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(CircleShape)
                                .background(colors.red)
                                .clickable {
                                    showResetDialog = false
                                    coroutineScope.launch {
                                        val app = context.applicationContext as ReflexApplication
                                        AlarmScheduler.cancelAllAlarms(context)
                                        val db = ReflexDatabase.getDatabase(context)
                                        db.withTransaction {
                                            app.repository.wipeAllData()
                                        }
                                        showToast("All data wiped cleanly")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Reset",
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    // Open-Source Licenses Dialog
    if (showLicensesDialog) {
        Dialog(onDismissRequest = { showLicensesDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(colors.surface)
                    .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Open-source licenses",
                        fontFamily = Lora,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Reflex is free software licensed under GNU General Public License v3.0 or later (GPL-3.0-or-later).\n\n" +
                                "Third-party libraries & assets:\n\n" +
                                "• Android Jetpack & Compose — Apache 2.0\n" +
                                "• Kotlin & Kotlinx Coroutines — Apache 2.0\n" +
                                "• Room Persistence Library — Apache 2.0\n" +
                                "• AndroidX DataStore & WorkManager — Apache 2.0\n" +
                                "• Haze Blur Engine (Chris Banes) — Apache 2.0\n" +
                                "• Lora Typeface (Cyreal) — SIL Open Font License 1.1\n" +
                                "• OpenMoji (OpenMoji Community) — CC BY-SA 4.0\n" +
                                "• Material Design Icons (Google) — Apache 2.0\n\n" +
                                "Complete license texts and source code are available in the GitHub repository.",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = colors.secondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(colors.input)
                            .clickable { showLicensesDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Close",
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                    }
                }
            }
        }
    }

    // Choose Calendars Bottom Sheet
    if (showCalendarPickerSheet) {
        CalendarPickerSheet(
            colors = colors,
            onDismiss = { showCalendarPickerSheet = false }
        )
    }

    // Configure App Blocking Sheet
    val app = context.applicationContext as? ReflexApplication
    val focusSettingsState = remember { mutableStateOf<com.reflex.app.data.FocusSettings?>(null) }
    LaunchedEffect(showAppPickerSheet) {
        if (showAppPickerSheet && app != null) {
            focusSettingsState.value = app.repository.getFocusSettingsSync()
        }
    }

    if (showAppPickerSheet) {
        val currentFocus = focusSettingsState.value ?: com.reflex.app.data.FocusSettings()
        val pkgs = currentFocus.selectedPackages.split(",").filter { it.isNotBlank() }.toSet()
        AppPickerSheet(
            initialMode = currentFocus.blockingMode,
            initialPackages = pkgs,
            onDismiss = { showAppPickerSheet = false },
            onSave = { mode, newPkgs ->
                showAppPickerSheet = false
                coroutineScope.launch {
                    val updated = currentFocus.copy(
                        blockingMode = mode,
                        selectedPackages = newPkgs.joinToString(",")
                    )
                    app?.repository?.saveFocusSettings(updated)
                    SettingsRepository.setBoolean(context, "f_blk", mode != com.reflex.app.data.BlockingMode.OFF)
                }
            }
        )
    }
}

// =========================================================================
// TOP BAR
// =========================================================================

@Composable
private fun SettingsTopBar(
    title: String,
    onBackClick: () -> Unit
) {
    val colors = SettingsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .background(colors.bg)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back Button: 44dp circle with chevron-left icon (22dp, stroke 1.8, round caps)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBackClick)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center
        ) {
            SettingsIcon(
                name = "back",
                tint = colors.ink,
                size = 22.dp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            fontFamily = Lora,
            fontSize = 22.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.Bold,
            color = colors.ink
        )
    }
}

// =========================================================================
// HOME SCREEN CONTENT
// =========================================================================

@Composable
private fun SettingsHomeScreenContent(
    colors: SettingsColorTokens,
    activeTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    settingsValues: Map<String, Any>,
    permissionStates: Map<String, Boolean>,
    onOpenScreen: (String) -> Unit,
    onShowToast: (String) -> Unit,
    onReplayTour: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    scrollState: ScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by UserProfileRepository.profile.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAvatarOptionsDialog by remember { mutableStateOf(false) }
    val pendCount = listOf("p_not", "p_cal", "p_mic", "p_blk", "p_ovr").count { !(permissionStates[it] ?: true) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    if (bytes != null) {
                        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (original != null) {
                            var orientation = ExifInterface.ORIENTATION_NORMAL
                            try {
                                val exif = ExifInterface(ByteArrayInputStream(bytes))
                                orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                            } catch (_: Exception) {}

                            val matrix = Matrix()
                            when (orientation) {
                                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                            }

                            val rotated = if (!matrix.isIdentity) {
                                Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                            } else {
                                original
                            }

                            val maxDim = 512
                            val (targetW, targetH) = if (rotated.width > maxDim || rotated.height > maxDim) {
                                val ratio = rotated.width.toFloat() / rotated.height.toFloat()
                                if (ratio > 1f) {
                                    maxDim to (maxDim / ratio).toInt().coerceAtLeast(1)
                                } else {
                                    (maxDim * ratio).toInt().coerceAtLeast(1) to maxDim
                                }
                            } else {
                                rotated.width to rotated.height
                            }

                            val scaled = if (targetW != rotated.width || targetH != rotated.height) {
                                Bitmap.createScaledBitmap(rotated, targetW, targetH, true)
                            } else {
                                rotated
                            }

                            UserProfileRepository.setAvatar(context, scaled)
                            onShowToast("Profile picture updated")
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e("SettingsScreen", "Failed to load profile avatar", e)
                    onShowToast("Could not load image")
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = bottomPadding)
    ) {
        // Search Bar: 52 high, radius 24, bg input, padding 0 18, gap 10, margin 6 top / 16 bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 16.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.input)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SettingsIcon(
                    name = "search",
                    tint = colors.secondary,
                    size = 22.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = Lora,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.ink
                    ),
                    cursorBrush = SolidColor(colors.copper),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search settings",
                                fontFamily = Lora,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal,
                                color = colors.tertiary
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Live Search Results
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            val hits = remember(q) {
                val list = mutableListOf<Triple<String, String, SettingItem>>()
                SettingsSchema.SC.forEach { (screenId, screenDef) ->
                    screenDef.groups.forEach { group ->
                        group.items.forEach { item ->
                            if ((item.label + " " + item.description).lowercase().contains(q)) {
                                list.add(Triple(screenId, "${screenDef.title} › ${group.heading}", item))
                            }
                        }
                    }
                }
                list
            }

            if (hits.isNotEmpty()) {
                SettingsCard {
                    hits.forEachIndexed { index, (screenId, crumb, item) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clickable { onOpenScreen(screenId) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.label,
                                    fontFamily = Lora,
                                    fontSize = 16.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Text(
                                    text = crumb,
                                    fontFamily = Lora,
                                    fontSize = 13.sp,
                                    lineHeight = 17.sp,
                                    color = colors.tertiary
                                )
                            }
                            SettingsIcon(
                                name = "chev",
                                tint = colors.tertiary,
                                size = 20.dp
                            )
                        }
                        if (index < hits.size - 1) {
                            SettingsDivider()
                        }
                    }
                }
            } else {
                Text(
                    text = "No settings match “$searchQuery”.",
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp, start = 8.dp, end = 8.dp)
                )
            }
        } else {
            // Attention Banner: if any of the 5 permissions is off
            if (pendCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(colors.chip)
                        .clickable { onOpenScreen("permissions") }
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tile: 44x44, radius 18, surface bg, copper shield icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            SettingsIcon(
                                name = "shield",
                                tint = colors.copper,
                                size = 22.dp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$pendCount permission${if (pendCount > 1) "s" else ""} need attention",
                                fontFamily = Lora,
                                fontSize = 16.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.copper
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Voice input and app blocking are off",
                                fontFamily = Lora,
                                fontSize = 13.sp,
                                lineHeight = 17.sp,
                                color = colors.secondary
                            )
                        }

                        SettingsIcon(
                            name = "chev",
                            tint = colors.copper,
                            size = 20.dp
                        )
                    }
                }
            }

            // --- 0. USER PROFILE GROUP ---
            SettingsGroupHeader(
                title = "Profile",
                subtitle = "Personalize your reflex workspace"
            )

            SettingsCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar container (64dp, unclipped so badge can sit half-way in, half-way out)
                    Box(
                        modifier = Modifier.size(64.dp)
                    ) {
                        // Avatar (64dp circle)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(colors.chip)
                                .clickable {
                                    if (profile.avatarPath != null) {
                                        showAvatarOptionsDialog = true
                                    } else {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val avatarBitmap = remember(profile.avatarPath, profile.avatarBase64) {
                                profile.avatarPath?.let { path ->
                                    try {
                                        val f = File(path)
                                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                                    } catch (_: Exception) { null }
                                }
                            }

                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Default Thunder vector icon in Reflex stroke design
                                SettingsIcon(
                                    name = "thunder",
                                    tint = colors.copper,
                                    size = 32.dp
                                )
                            }
                        }

                        // Camera badge indicator overlay: 48dp touch target with 24dp visual badge (half-in, half-out of profile ring)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 16.dp, y = 16.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Change profile photo"
                                ) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .semantics {
                                    contentDescription = "Change profile photo"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.surface)
                                    .border(1.5.dp, colors.border, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                SettingsIcon(
                                    name = "camera",
                                    tint = colors.secondary,
                                    size = 12.dp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name and Bio text block
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEditProfileDialog = true }
                    ) {
                        Text(
                            text = profile.name.ifBlank { "User" },
                            fontFamily = Lora,
                            fontSize = 18.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = profile.bio.ifBlank { "Add a short bio..." },
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            color = if (profile.bio.isBlank()) colors.tertiary else colors.secondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Edit button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.input)
                            .clickable { showEditProfileDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsIcon(
                            name = "edit",
                            tint = colors.secondary,
                            size = 18.dp
                        )
                    }
                }
            }

            // --- 1. APPEARANCE GROUP ---
            SettingsGroupHeader(
                title = "Appearance",
                subtitle = "Follows your phone by default"
            )

            SettingsThemeSegment(
                activeTheme = activeTheme,
                onSelectTheme = onSelectTheme
            )

            // --- 2. MODULES GROUP ---
            SettingsGroupHeader(
                title = "Modules",
                subtitle = "Tune each part of Reflex"
            )

            SettingsCard {
                SettingsNavigationRow(
                    icon = "routines",
                    title = "Routines",
                    summary = SettingsRepository.getSummary("routines"),
                    onClick = { onOpenScreen("routines") }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "calendar",
                    title = "Calendar",
                    summary = SettingsRepository.getSummary("calendar"),
                    onClick = { onOpenScreen("calendar") }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "tasks",
                    title = "Tasks",
                    summary = SettingsRepository.getSummary("tasks"),
                    onClick = { onOpenScreen("tasks") }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "habits",
                    title = "Habits",
                    summary = SettingsRepository.getSummary("habits"),
                    onClick = { onOpenScreen("habits") }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "focus",
                    title = "Focus",
                    summary = SettingsRepository.getSummary("focus"),
                    onClick = { onOpenScreen("focus") }
                )
            }

            // --- 3. GENERAL GROUP ---
            SettingsGroupHeader(
                title = "General"
            )

            SettingsCard {
                SettingsNavigationRow(
                    icon = "bell",
                    title = "Notifications",
                    summary = SettingsRepository.getSummary("notifications"),
                    onClick = { onOpenScreen("notifications") }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "shield",
                    title = "Permissions and access",
                    summary = if (pendCount > 0) "$pendCount not allowed" else "All set",
                    onClick = { onOpenScreen("permissions") }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "db",
                    title = "Backup and data",
                    summary = SettingsRepository.getSummary("data"),
                    onClick = { onOpenScreen("data") }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "tour",
                    title = "App tour",
                    summary = "Replay the welcome walkthrough",
                    onClick = onReplayTour
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "mail",
                    title = "Report bugs / feedback",
                    summary = "Send feedback or request features via email",
                    onClick = { sendFeedbackEmail(context) }
                )
                SettingsDivider()
                SettingsNavigationRow(
                    icon = "info",
                    title = "About Reflex",
                    summary = "Version ${com.reflex.app.BuildConfig.VERSION_NAME} (${com.reflex.app.BuildConfig.VERSION_CODE}) · works offline",
                    onClick = { onOpenScreen("about") }
                )
            }

            // Footer note
            Text(
                text = "Reflex works entirely offline.\nYour data never leaves this device.",
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.tertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 12.dp, start = 8.dp, end = 8.dp)
            )
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember(showEditProfileDialog) { mutableStateOf(profile.name) }
        var editBio by remember(showEditProfileDialog) { mutableStateOf(profile.bio) }

        Dialog(onDismissRequest = { showEditProfileDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(colors.surface)
                    .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Edit profile",
                        fontFamily = Lora,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Avatar Row in dialog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.chip)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val dialogAvatarBitmap = remember(profile.avatarPath, profile.avatarBase64) {
                                profile.avatarPath?.let { path ->
                                    try {
                                        val f = File(path)
                                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                                    } catch (_: Exception) { null }
                                }
                            }

                            if (dialogAvatarBitmap != null) {
                                Image(
                                    bitmap = dialogAvatarBitmap,
                                    contentDescription = "Profile picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                SettingsIcon(
                                    name = "thunder",
                                    tint = colors.copper,
                                    size = 28.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(colors.input)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SettingsIcon(
                                        name = "camera",
                                        tint = colors.copper,
                                        size = 16.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (profile.avatarPath != null) "Change photo" else "Upload photo",
                                        fontFamily = Lora,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.ink
                                    )
                                }
                            }

                            if (profile.avatarPath != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Remove photo",
                                    fontFamily = Lora,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.red,
                                    modifier = Modifier
                                        .clickable {
                                            UserProfileRepository.removeAvatar(context)
                                            onShowToast("Profile picture removed")
                                        }
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Name",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.input)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = Lora,
                                fontSize = 15.sp,
                                color = colors.ink
                            ),
                            cursorBrush = SolidColor(colors.copper),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Bio",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.secondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.input)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = editBio,
                            onValueChange = { if (it.length <= 160) editBio = it },
                            textStyle = TextStyle(
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                color = colors.ink
                            ),
                            cursorBrush = SolidColor(colors.copper),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (editBio.isEmpty()) {
                                    Text(
                                        text = "A short bio about your goals or focus...",
                                        fontFamily = Lora,
                                        fontSize = 14.sp,
                                        color = colors.tertiary
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(CircleShape)
                                .background(colors.input)
                                .clickable { showEditProfileDialog = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.ink
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(CircleShape)
                                .background(colors.copper)
                                .clickable {
                                    UserProfileRepository.updateNameAndBio(context, editName, editBio)
                                    showEditProfileDialog = false
                                    onShowToast("Profile updated")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Save",
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onCopper
                            )
                        }
                    }
                }
            }
        }
    }

    // Avatar Quick Options Dialog
    if (showAvatarOptionsDialog) {
        Dialog(onDismissRequest = { showAvatarOptionsDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .background(colors.surface)
                    .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(32.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Profile picture",
                        fontFamily = Lora,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.input)
                            .clickable {
                                showAvatarOptionsDialog = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIcon(name = "camera", tint = colors.copper, size = 20.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Choose new photo",
                            fontFamily = Lora,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.red.copy(alpha = 0.12f))
                            .clickable {
                                showAvatarOptionsDialog = false
                                UserProfileRepository.removeAvatar(context)
                                onShowToast("Profile picture removed")
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsIcon(name = "trash", tint = colors.red, size = 20.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Reset to default avatar",
                            fontFamily = Lora,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.red
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(colors.input)
                            .clickable { showAvatarOptionsDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cancel",
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.ink
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// GENERIC SUB-SCREEN RENDERER
// =========================================================================

@Composable
private fun SettingsGenericSubScreenContent(
    screenDef: com.reflex.app.data.SettingScreenDef,
    colors: SettingsColorTokens,
    settingsValues: Map<String, Any>,
    permissionStates: Map<String, Boolean>,
    permissionDetails: Map<String, PermissionDetail> = emptyMap(),
    storageStats: AppStorageStats? = null,
    onToggle: (String, Boolean) -> Unit,
    onSelectSegment: (String, String) -> Unit,
    onStep: (String, Int, Int, Int, Int) -> Unit,
    onPermissionAction: (String) -> Unit,
    onLinkAction: (SettingItem) -> Unit,
    onDangerAction: (SettingItem) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    val context = LocalContext.current
    val scrollState = rememberSaveable(screenDef.id, saver = ScrollState.Saver) { ScrollState(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        if (screenDef.id == "about") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    com.reflex.app.ui.components.ReflexMark(size = 80.dp, animated = true)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Reflex",
                        fontFamily = Lora,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.ink
                    )
                    Text(
                        text = "Routine, habit & task tracker",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        color = colors.secondary
                    )
                }
            }
        }

        screenDef.groups.forEach { group ->
            SettingsGroupHeader(
                title = group.heading,
                subtitle = group.caption
            )

            SettingsCard {
                group.items.forEachIndexed { index, item ->
                    when (item.type) {
                        SettingItemType.TG -> {
                            val isChecked = (settingsValues[item.key] as? Boolean) ?: item.defaultBool
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        fontFamily = Lora,
                                        fontSize = 16.sp,
                                        lineHeight = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.ink
                                    )
                                    val itemDesc = if (item.key == "d_auto") {
                                        val lastBackupTime = SettingsRepository.getLastBackupTime(context)
                                        val lastBackupStatus = SettingsRepository.getLastBackupStatus(context)
                                        when {
                                            lastBackupStatus != null -> "Status: $lastBackupStatus"
                                            lastBackupTime > 0L -> {
                                                val dt = java.time.Instant.ofEpochMilli(lastBackupTime).atZone(java.time.ZoneId.systemDefault())
                                                val formatted = dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                                                "Last backup: $formatted"
                                            }
                                            else -> item.description
                                        }
                                    } else {
                                        item.description
                                    }
                                    if (itemDesc.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = itemDesc,
                                            fontFamily = Lora,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            color = colors.secondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                SettingsCustomSwitch(
                                    checked = isChecked,
                                    onCheckedChange = { onToggle(item.key ?: "", it) }
                                )
                            }
                        }

                        SettingItemType.SG -> {
                            val selectedVal = (settingsValues[item.key] as? String) ?: item.defaultString
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp)
                            ) {
                                Text(
                                    text = item.label,
                                    fontFamily = Lora,
                                    fontSize = 16.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink
                                )
                                if (item.description.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = item.description,
                                        fontFamily = Lora,
                                        fontSize = 13.sp,
                                        lineHeight = 17.sp,
                                        color = colors.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                SettingsSegmentedPills(
                                    options = item.options,
                                    selected = selectedVal,
                                    onSelect = { onSelectSegment(item.key ?: "", it) }
                                )
                            }
                        }

                        SettingItemType.ST -> {
                            val currentVal = (settingsValues[item.key] as? Int) ?: item.defaultInt
                            val displayValue = if (item.fmt == "hr") {
                                SettingsSchema.formatHour(currentVal)
                            } else {
                                "$currentVal ${item.unit}"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        fontFamily = Lora,
                                        fontSize = 16.sp,
                                        lineHeight = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.ink
                                    )
                                    if (item.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = item.description,
                                            fontFamily = Lora,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            color = colors.secondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                SettingsStepper(
                                    valueText = displayValue,
                                    onDecrease = { onStep(item.key ?: "", -1, item.min, item.max, item.step) },
                                    onIncrease = { onStep(item.key ?: "", 1, item.min, item.max, item.step) }
                                )
                            }
                        }

                        SettingItemType.LN -> {
                            val dynamicValue = when (item.key) {
                                "choose_calendars" -> SettingsRepository.getEnabledCalendarsCount(context)
                                else -> item.value
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .clickable { onLinkAction(item) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        fontFamily = Lora,
                                        fontSize = 16.sp,
                                        lineHeight = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.ink
                                    )
                                    if (item.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = item.description,
                                            fontFamily = Lora,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            color = colors.secondary
                                        )
                                    }
                                }
                                if (dynamicValue.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dynamicValue,
                                        fontFamily = Lora,
                                        fontSize = 14.sp,
                                        color = colors.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SettingsIcon(
                                    name = "chev",
                                    tint = colors.tertiary,
                                    size = 20.dp
                                )
                            }
                        }

                        SettingItemType.IN -> {
                            val displayVal = when (item.key) {
                                "storage" -> storageStats?.formattedStorageSize ?: SettingsRepository.getDeviceStorageSize(context)
                                "db_stats" -> storageStats?.formattedRecordSummary ?: "Calculating..."
                                "version" -> "${com.reflex.app.BuildConfig.VERSION_NAME} (${com.reflex.app.BuildConfig.VERSION_CODE})"
                                "last_backup" -> {
                                    val lastTime = SettingsRepository.getLastBackupTime(context)
                                    val lastStatus = SettingsRepository.getLastBackupStatus(context)
                                    when {
                                        lastStatus != null -> "Error: $lastStatus"
                                        lastTime > 0L -> {
                                            val dt = java.time.Instant.ofEpochMilli(lastTime).atZone(java.time.ZoneId.systemDefault())
                                            dt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                                        }
                                        else -> "Never"
                                    }
                                }
                                else -> item.value
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.label,
                                    fontFamily = Lora,
                                    fontSize = 16.sp,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = displayVal,
                                    fontFamily = Lora,
                                    fontSize = 14.sp,
                                    color = colors.secondary
                                )
                            }
                        }

                        SettingItemType.PM -> {
                            val itemKey = item.key ?: ""
                            val detail = permissionDetails[itemKey]
                            val state = detail?.state ?: if (permissionStates[itemKey] == true) PermissionBadgeState.GRANTED else PermissionBadgeState.DENIED
                            val activeText = if (itemKey == "p_alm") "Active" else "Allowed"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        fontFamily = Lora,
                                        fontSize = 16.sp,
                                        lineHeight = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.ink
                                    )
                                    if (item.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = item.description,
                                            fontFamily = Lora,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            color = colors.secondary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                SettingsPermissionPill(
                                    state = state,
                                    activeText = activeText,
                                    onAllow = { onPermissionAction(item.key ?: "") }
                                )
                            }
                        }

                        SettingItemType.DB -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 64.dp)
                                    .clickable { onDangerAction(item) }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(colors.redBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SettingsIcon(
                                        name = "trash",
                                        tint = colors.red,
                                        size = 22.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.label,
                                        fontFamily = Lora,
                                        fontSize = 16.sp,
                                        lineHeight = 21.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.red
                                    )
                                    if (item.description.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = item.description,
                                            fontFamily = Lora,
                                            fontSize = 13.sp,
                                            lineHeight = 17.sp,
                                            color = colors.secondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (index < group.items.size - 1) {
                        SettingsDivider()
                    }
                }
            }
        }
    }
}

// =========================================================================
// REUSABLE COMPONENTS
// =========================================================================

@Composable
private fun SettingsGroupHeader(
    title: String,
    subtitle: String? = null
) {
    val colors = SettingsTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 22.dp, bottom = 10.dp)
    ) {
        Text(
            text = title,
            fontFamily = Lora,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.ink
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = colors.secondary
            )
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SettingsTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(colors.surface)
            .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(32.dp))
            .padding(horizontal = 18.dp, vertical = 4.dp),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    val colors = SettingsTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border)
    )
}

@Composable
private fun SettingsNavigationRow(
    icon: String,
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    val colors = SettingsTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tile: 44x44, radius 18, bg chip, copper icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.chip),
            contentAlignment = Alignment.Center
        ) {
            SettingsIcon(
                name = icon,
                tint = colors.copper,
                size = 22.dp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = Lora,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = summary,
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = colors.secondary
            )
        }

        SettingsIcon(
            name = "chev",
            tint = colors.tertiary,
            size = 20.dp
        )
    }
}

/**
 * Custom Switch: 52x30, radius 999.
 * Track: input bg off, copper on.
 * Thumb: 22x22 circle, 4dp inset, tertiary off, onCopper on.
 * 200ms slide transition.
 */
@Composable
private fun SettingsCustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = SettingsTheme.colors
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 26.dp else 4.dp,
        animationSpec = tween(200),
        label = "SwitchThumbOffset"
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

/**
 * Segmented Pills: 44 high, full pill, bg input, 14sp Medium.
 * Selected: bg copper, fg onCopper, SemiBold.
 * 8dp gaps, wraps if needed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsSegmentedPills(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val colors = SettingsTheme.colors
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEach { opt ->
            val isSelected = opt == selected
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .widthIn(min = 64.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.copper else colors.input)
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt,
                    fontFamily = Lora,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.onCopper else colors.ink
                )
            }
        }
    }
}

/**
 * Theme 3-Way Segmented Control: System, Dark, Light.
 * Smooth sliding copper pill indicator with fluid bezier animation.
 */
@Composable
private fun SettingsThemeSegment(
    activeTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit
) {
    val colors = SettingsTheme.colors
    val options = listOf(
        Triple(AppTheme.SYSTEM, "System", "phone"),
        Triple(AppTheme.DARK, "Dark", "moon"),
        Triple(AppTheme.LIGHT, "Light", "sun")
    )

    val selectedIndex = when (activeTheme) {
        AppTheme.SYSTEM -> 0
        AppTheme.DARK -> 1
        AppTheme.LIGHT -> 2
    }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.input)
            .padding(4.dp)
    ) {
        val segmentWidth = maxWidth / 3
        val animatedOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(
                durationMillis = 240,
                easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
            ),
            label = "ThemePillOffset"
        )

        // Animated sliding copper background pill
        Box(
            modifier = Modifier
                .padding(start = animatedOffset)
                .width(segmentWidth)
                .height(44.dp)
                .clip(CircleShape)
                .background(colors.copper)
        )

        // Segment touch targets and labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            options.forEach { (theme, label, icon) ->
                val isSelected = activeTheme == theme
                val contentColor by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) colors.onCopper else colors.secondary,
                    animationSpec = tween(200),
                    label = "ThemeContentColor"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onSelectTheme(theme)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsIcon(
                            name = icon,
                            tint = contentColor,
                            size = 20.dp
                        )
                        Text(
                            text = label,
                            fontFamily = Lora,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stepper: circular '-' button (40dp), value text (min width 62, center, 15sp SemiBold), circular '+' button (40dp).
 */
@Composable
private fun SettingsStepper(
    valueText: String,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val colors = SettingsTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Decrease button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.input)
                .clickable(onClick = onDecrease),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                fontFamily = Lora,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink
            )
        }

        // Value text
        Text(
            text = valueText,
            fontFamily = Lora,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 62.dp)
        )

        // Increase button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.input)
                .clickable(onClick = onIncrease),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                fontFamily = Lora,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink
            )
        }
    }
}

/**
 * Permission status pill: 34dp high, full pill, 13sp SemiBold.
 * Allowed = bg ok@16%, text ok.
 * Partial = bg chip, text copper, label 'Partial'.
 * Not allowed = button with bg chip, text copper, label 'Allow'.
 */
@Composable
private fun SettingsPermissionPill(
    state: PermissionBadgeState,
    onAllow: () -> Unit,
    activeText: String = "Allowed"
) {
    val colors = SettingsTheme.colors
    when (state) {
        PermissionBadgeState.GRANTED -> {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(colors.okBg)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeText,
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ok
                )
            }
        }
        PermissionBadgeState.PARTIAL -> {
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
                    text = "Partial",
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.copper
                )
            }
        }
        PermissionBadgeState.DENIED -> {
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.copper
                )
            }
        }
    }
}

@Composable
private fun SettingsPermissionPill(
    isAllowed: Boolean,
    onAllow: () -> Unit
) {
    SettingsPermissionPill(
        state = if (isAllowed) PermissionBadgeState.GRANTED else PermissionBadgeState.DENIED,
        onAllow = onAllow
    )
}

/**
 * Calendar Picker Dialog for "Choose calendars" in Calendar settings.
 */
@Composable
private fun CalendarPickerSheet(
    colors: SettingsColorTokens,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendars = remember {
        CalendarProviderHelper.getAvailableCalendars(context)
    }
    val calPrefs by CalendarPreferenceRepository.preferences.collectAsState()
    val enabledIds = calPrefs.enabledCalendarIds

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(32.dp))
                .background(colors.surface)
                .border(BorderStroke(1.dp, colors.border), RoundedCornerShape(32.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Choose calendars",
                    fontFamily = Lora,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.ink
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Events from enabled calendars appear on your timeline.",
                    fontFamily = Lora,
                    fontSize = 13.sp,
                    color = colors.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (calendars.isEmpty()) {
                    Text(
                        text = "No device calendars found or permission not granted.",
                        fontFamily = Lora,
                        fontSize = 14.sp,
                        color = colors.secondary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    calendars.forEach { cal ->
                        val isEnabled = enabledIds == null || enabledIds.contains(cal.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val currentSet = enabledIds?.toMutableSet() ?: calendars.map { it.id }.toMutableSet()
                                    if (isEnabled) {
                                        currentSet.remove(cal.id)
                                    } else {
                                        currentSet.add(cal.id)
                                    }
                                    CalendarPreferenceRepository.setEnabledCalendarIds(context, currentSet)
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color circle
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(cal.color.let { if (it == 0) 0xFFB5714F.toInt() else it }))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = cal.name,
                                    fontFamily = Lora,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.ink
                                )
                                if (!cal.accountName.isNullOrBlank()) {
                                    Text(
                                        text = cal.accountName,
                                        fontFamily = Lora,
                                        fontSize = 12.sp,
                                        color = colors.tertiary
                                    )
                                }
                            }
                            SettingsCustomSwitch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    val currentSet = enabledIds?.toMutableSet() ?: calendars.map { it.id }.toMutableSet()
                                    if (checked) currentSet.add(cal.id) else currentSet.remove(cal.id)
                                    CalendarPreferenceRepository.setEnabledCalendarIds(context, currentSet)
                                }
                            )
                        }
                        SettingsDivider()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(colors.input)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Done",
                        fontFamily = Lora,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.ink
                    )
                }
            }
        }
    }
}

private fun openAppNotificationSettings(context: Context) {
    try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

private fun Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun sendFeedbackEmail(context: Context) {
    val email = com.reflex.app.util.AppConstants.FEEDBACK_EMAIL
    val subject = "[Reflex Feedback] v${com.reflex.app.BuildConfig.VERSION_NAME}"
    val body = """
        Hi Reflex Team,

        [Describe your feedback, bug report, or feature request here]


        ---
        App Version: ${com.reflex.app.BuildConfig.VERSION_NAME} (${com.reflex.app.BuildConfig.VERSION_CODE})
        Device: ${Build.MANUFACTURER} ${Build.MODEL}
        Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        context.startActivity(Intent.createChooser(intent, "Send feedback via..."))
    } catch (e: Exception) {
        com.reflex.app.util.AppLog.w("SettingsScreen", "Direct email chooser failed, trying mailto URI", e)
        try {
            val mailtoUri = Uri.parse("mailto:$email?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, mailtoUri))
        } catch (ex: Exception) {
            com.reflex.app.util.AppLog.e("SettingsScreen", "No email app found to send feedback", ex)
            android.widget.Toast.makeText(context, "No email app found to send feedback", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}

private fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        com.reflex.app.util.AppLog.e("SettingsScreen", "Failed to open URL $url", e)
        android.widget.Toast.makeText(context, "Could not open browser for: $url", android.widget.Toast.LENGTH_SHORT).show()
    }
}

