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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.CopperSubtle
import com.reflex.app.ui.theme.DestructiveContainer
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.util.AppBlockPermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionOnboardingSheet(
    onDismiss: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val context = LocalContext.current
    val hasUsageAccess = AppBlockPermissionHelper.hasUsageAccessPermission(context)
    val hasOverlay = AppBlockPermissionHelper.hasOverlayPermission(context)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = ReflexTokens.ShapeModal
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ReflexTokens.SpaceLg)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "App blocking setup",
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

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            Text(
                text = "To block distracting apps during work phases, Reflex requires two special system permissions. No data ever leaves your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // 1. Usage Stats Permission Card
            ReflexCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                .size(36.dp)
                                .clip(ReflexTokens.ShapeChip)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QueryStats,
                                    contentDescription = null,
                                    tint = CopperPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(ReflexTokens.SpaceSm))
                            Text(
                                text = "1. Usage access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        StatusBadge(isGranted = hasUsageAccess)
                    }

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                    Text(
                        text = "Allows Reflex to detect when a blocked app is opened in the foreground during work sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hasUsageAccess) {
                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                        ReflexButton(
                            text = "Grant usage access",
                            onClick = { AppBlockPermissionHelper.openUsageAccessSettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ReflexButtonVariant.PRIMARY
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            // 2. Display Overlay Permission Card
            ReflexCard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                .size(36.dp)
                                .clip(ReflexTokens.ShapeChip)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = null,
                                    tint = CopperPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(ReflexTokens.SpaceSm))
                            Text(
                                text = "2. Display overlay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        StatusBadge(isGranted = hasOverlay)
                    }

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                    Text(
                        text = "Allows Reflex to display a focused blocking overlay screen on top of blocked apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!hasOverlay) {
                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                        ReflexButton(
                            text = "Grant overlay permission",
                            onClick = { AppBlockPermissionHelper.openOverlaySettings(context) },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ReflexButtonVariant.PRIMARY
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

            ReflexButton(
                text = if (hasUsageAccess && hasOverlay) "Continue to app selection" else "Done",
                onClick = {
                    if (hasUsageAccess && hasOverlay) {
                        onPermissionsGranted()
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                variant = if (hasUsageAccess && hasOverlay) ReflexButtonVariant.PRIMARY else ReflexButtonVariant.SECONDARY
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))
        }
    }
}

@Composable
private fun StatusBadge(isGranted: Boolean) {
    val errorColor = MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .clip(ReflexTokens.ShapeButton)
            .background(if (isGranted) CopperSubtle else DestructiveContainer)
            .border(
                BorderStroke(
                    0.75.dp,
                    if (isGranted) CopperPrimary.copy(alpha = 0.5f) else errorColor.copy(alpha = 0.5f)
                ),
                ReflexTokens.ShapeButton
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = if (isGranted) "Granted" else "Required",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isGranted) CopperPrimary else errorColor,
            fontSize = 13.sp,
            letterSpacing = 0.sp
        )
    }
}
