package com.reflex.app.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.ReflexTokens
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalHazeState = compositionLocalOf { HazeState() }

enum class NavTab {
    ROUTINES,
    CALENDAR,
    TASKS,
    HABITS,
    FOCUS
}

/**
 * Floating Frosted Glass Tab Bar (Reflex Design System v1.0 / reflex-focus(1).html).
 * - Geometry: height 72dp, stadium radius 36dp, width min(screen - 32dp, 398dp), centered, bottom = 12dp + navigationBarsInset.
 * - Backdrop blur (API 31+): Haze library blur on scrolling content with blurRadius 24dp, no noise, and 72% tint.
 *   Fallback below API 31: 94% near-opaque tint.
 * - Rim: 1dp border (dark: white @ 12%, light: black @ 8%).
 * - Soft shadow: 8dp blur, 2dp Y offset, ~12% black.
 * - Five tabs: Routines, Calendar, center action, Habits, Focus. Each tab slot is 64dp wide, with 22dp icon over 13sp Medium label (2dp gap).
 * - Active tab: copper (#D9A184 dark / #B5714F light), inactive: secondary text.
 * - Center action: 52dp circular copper button with morphing icon.
 */
@Composable
fun ReflexBottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    isQuickAddOpen: Boolean = false,
    hasDraftText: Boolean = false,
    onCenterActionClick: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val hazeState = LocalHazeState.current
    val isBlurAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var optimisticTab by androidx.compose.runtime.remember(selectedTab) { androidx.compose.runtime.mutableStateOf(selectedTab) }

    val handleTabSelected: (NavTab) -> Unit = { tab ->
        optimisticTab = tab
        onTabSelected(tab)
    }

    // Tint: dark rgba(28,26,23,0.72) = #B81C1A17; light rgba(255,255,255,0.72) = #B8FFFFFF
    // Fallback: dark #F01C1A17 / light #F0FFFFFF (94%)
    val glassTint = if (isBlurAvailable) {
        if (isDark) Color(0xB81C1A17) else Color(0xB8FFFFFF)
    } else {
        if (isDark) Color(0xF01C1A17) else Color(0xF0FFFFFF)
    }

    // Rim: 1dp border, dark white @ 12%, light black @ 8%
    val rimColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val barShape = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = ReflexTokens.BottomNavFloatingMarginV),
        contentAlignment = Alignment.Center
    ) {
        // Outer Shadow & Glass Background Plate
        Box(
            modifier = Modifier
                .widthIn(max = 398.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(72.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = barShape,
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(barShape)
                .background(glassTint)
                .then(
                    if (isBlurAvailable) {
                        Modifier.hazeEffect(
                            state = hazeState,
                            style = HazeStyle(
                                backgroundColor = MaterialTheme.colorScheme.background,
                                tint = HazeTint(glassTint),
                                blurRadius = 24.dp,
                                noiseFactor = 0f
                            )
                        )
                    } else {
                        Modifier
                    }
                )
                .border(BorderStroke(1.dp, rimColor), barShape)
        )

        // Foreground Tab Content Row - 64dp wide slots, 22dp icons, 2dp gap, 13sp Medium label
        Row(
            modifier = Modifier
                .widthIn(max = 398.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 1. Routines
            NavTabItem(
                tab = NavTab.ROUTINES,
                selectedTab = optimisticTab,
                icon = Icons.Default.Schedule,
                label = "Routines",
                isDark = isDark,
                onTabSelected = handleTabSelected
            )

            // 2. Calendar
            NavTabItem(
                tab = NavTab.CALENDAR,
                selectedTab = optimisticTab,
                icon = Icons.Default.CalendarMonth,
                label = "Calendar",
                isDark = isDark,
                onTabSelected = handleTabSelected
            )

            // 3. Center Tasks Button (52dp circular copper action button)
            Box(
                modifier = Modifier.width(64.dp),
                contentAlignment = Alignment.Center
            ) {
                val centerInteraction = remember { MutableInteractionSource() }
                val isCenterPressed by centerInteraction.collectIsPressedAsState()
                val centerScale by animateFloatAsState(
                    targetValue = if (isCenterPressed) 0.96f else 1.0f,
                    animationSpec = tween(120),
                    label = "center_fab_scale"
                )

                val copperColor = if (isDark) Color(0xFFD9A184) else Color(0xFFB5714F)

                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .graphicsLayer {
                            scaleX = centerScale
                            scaleY = centerScale
                        }
                        .semantics { contentDescription = "Tasks, scroll to top" }
                        .clip(CircleShape)
                        .background(copperColor)
                        .clickable(
                            interactionSource = centerInteraction,
                            indication = null,
                            onClick = onCenterActionClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = ActionPillOnWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // 4. Habits
            NavTabItem(
                tab = NavTab.HABITS,
                selectedTab = optimisticTab,
                icon = Icons.Default.LocalFireDepartment,
                label = "Habits",
                isDark = isDark,
                onTabSelected = handleTabSelected
            )

            // 5. Focus
            NavTabItem(
                tab = NavTab.FOCUS,
                selectedTab = optimisticTab,
                icon = Icons.Default.Adjust,
                label = "Focus",
                isDark = isDark,
                onTabSelected = handleTabSelected
            )
        }
    }
}

@Composable
private fun NavTabItem(
    tab: NavTab,
    selectedTab: NavTab,
    icon: ImageVector,
    label: String,
    isDark: Boolean,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = tab == selectedTab
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(120),
        label = "nav_tab_press_scale"
    )

    // Active copper: #D9A184 dark / #B5714F light. Inactive: secondary text.
    val activeCopper = if (isDark) Color(0xFFD9A184) else Color(0xFFB5714F)
    val inactiveSec = if (isDark) Color(0xFFA39E98) else Color(0xFF6B625B)

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) activeCopper else inactiveSec,
        animationSpec = tween(ReflexTokens.AnimDurationNormal),
        label = "nav_content_color"
    )

    Box(
        modifier = modifier
            .width(64.dp)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onTabSelected(tab) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(22.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontFamily = Lora,
                fontSize = 13.sp,
                lineHeight = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = contentColor,
                letterSpacing = 0.sp
            )
        }
    }
}
