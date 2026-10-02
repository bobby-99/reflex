package com.reflex.app.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.BlockingMode
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.CopperSubtle
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.util.AppBlockPermissionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    initialMode: BlockingMode,
    initialPackages: Set<String>,
    onDismiss: () -> Unit,
    onSave: (BlockingMode, Set<String>) -> Unit
) {
    val context = LocalContext.current

    var blockingMode by remember { mutableStateOf(initialMode) }
    var selectedPackages by remember { mutableStateOf(initialPackages) }
    var searchQuery by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showPermissionSheet by remember { mutableStateOf(false) }

    fun selectModeWithPermissionCheck(mode: BlockingMode) {
        if (mode != BlockingMode.OFF) {
            val hasUsage = AppBlockPermissionHelper.hasUsageAccessPermission(context)
            val hasOverlay = AppBlockPermissionHelper.hasOverlayPermission(context)
            if (!hasUsage || !hasOverlay) {
                showPermissionSheet = true
                return
            }
        }
        blockingMode = mode
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val exempt = AppBlockPermissionHelper.getHardcodedExemptPackages(context)
            val pm = context.packageManager
            val appList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfos = pm.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
                resolveInfos.mapNotNull { resolveInfo ->
                    val pkg = resolveInfo.activityInfo.packageName
                    if (exempt.contains(pkg)) null
                    else {
                        val name = resolveInfo.loadLabel(pm).toString()
                        val icon = resolveInfo.loadIcon(pm)
                        InstalledAppItem(packageName = pkg, appName = name, icon = icon)
                    }
                }
            } else {
                val installedApplications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledApplications(0)
                }
                installedApplications.mapNotNull { appInfo ->
                    val pkg = appInfo.packageName
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent == null || exempt.contains(pkg)) null
                    else {
                        val name = appInfo.loadLabel(pm).toString()
                        val icon = appInfo.loadIcon(pm)
                        InstalledAppItem(packageName = pkg, appName = name, icon = icon)
                    }
                }
            }

            installedApps = appList
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }
            isLoading = false
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) || it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = ReflexTokens.ShapeModal
    ) {
        Column(
            modifier = (if (blockingMode != BlockingMode.OFF) {
                Modifier.fillMaxWidth().fillMaxHeight(0.9f)
            } else {
                Modifier.fillMaxWidth()
            }).padding(horizontal = ReflexTokens.SpaceLg)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App blocking rules",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.sp
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            // Mode Selector Chips
            Text(
                text = "Blocking mode",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                BlockingModeChip(
                    label = "Off",
                    isSelected = blockingMode == BlockingMode.OFF,
                    onClick = { blockingMode = BlockingMode.OFF },
                    modifier = Modifier.weight(1f)
                )
                BlockingModeChip(
                    label = "Block list",
                    isSelected = blockingMode == BlockingMode.BLOCK_LIST,
                    onClick = { selectModeWithPermissionCheck(BlockingMode.BLOCK_LIST) },
                    modifier = Modifier.weight(1f)
                )
                BlockingModeChip(
                    label = "Allow list",
                    isSelected = blockingMode == BlockingMode.ALLOW_LIST,
                    onClick = { selectModeWithPermissionCheck(BlockingMode.ALLOW_LIST) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            if (blockingMode != BlockingMode.OFF) {
                val countText = when (blockingMode) {
                    BlockingMode.BLOCK_LIST -> "${selectedPackages.size} apps blocked"
                    BlockingMode.ALLOW_LIST -> "${selectedPackages.size} apps allowed"
                    else -> ""
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CopperPrimary
                    )

                    if (selectedPackages.isNotEmpty()) {
                        Text(
                            text = "Clear all",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clip(ReflexTokens.ShapeButton)
                                .clickable { selectedPackages = emptySet() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                // Search field
                ReflexTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search installed apps...",
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = CopperPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading installed applications...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val isChecked = selectedPackages.contains(app.packageName)
                            AppRowItem(
                                app = app,
                                isChecked = isChecked,
                                onToggle = {
                                    selectedPackages = if (isChecked) {
                                        selectedPackages - app.packageName
                                    } else {
                                        selectedPackages + app.packageName
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                Text(
                    text = "App blocking is currently turned OFF. Select Block List or Allow List to restrict distracting apps during focus work phases.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = ReflexTokens.SpaceMd)
                )
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
            }

            // Fixed Bottom Save Action Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = ReflexTokens.SpaceMd, top = ReflexTokens.SpaceSm)
            ) {
                ReflexButton(
                    text = "Save blocking rules",
                    onClick = {
                        onSave(blockingMode, selectedPackages)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ReflexButtonVariant.PRIMARY
                )
            }
        }
    }

    if (showPermissionSheet) {
        PermissionOnboardingSheet(
            onDismiss = { showPermissionSheet = false },
            onPermissionsGranted = {
                showPermissionSheet = false
                blockingMode = if (initialMode != BlockingMode.OFF) initialMode else BlockingMode.BLOCK_LIST
            }
        )
    }
}

@Composable
private fun BlockingModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(ReflexTokens.ShapeChip)
            .background(if (isSelected) CopperSubtle else MaterialTheme.colorScheme.secondaryContainer)
            .border(
                BorderStroke(
                    if (isSelected) 1.dp else ReflexTokens.BorderHairline,
                    if (isSelected) CopperPrimary else MaterialTheme.colorScheme.outlineVariant
                ),
                ReflexTokens.ShapeChip
            )
            .clickable(onClick = onClick)
            .padding(vertical = ReflexTokens.SpaceSm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AppRowItem(
    app: InstalledAppItem,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ReflexTokens.ShapeInnerTile)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                BorderStroke(ReflexTokens.BorderHairline, if (isChecked) CopperPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant),
                ReflexTokens.ShapeInnerTile
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = ReflexTokens.SpaceMd, vertical = ReflexTokens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val imageBitmap = remember(app.icon) {
                app.icon?.let { drawableToBitmap(it)?.asImageBitmap() }
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        app.appName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CopperPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(ReflexTokens.SpaceMd))

            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        // Circular checkbox matching TaskRow
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isChecked) CopperPrimary else Color.Transparent)
                .border(
                    BorderStroke(1.5.dp, if (isChecked) CopperPrimary else MaterialTheme.colorScheme.outlineVariant),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isChecked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
    }
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
