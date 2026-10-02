package com.reflex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.reflexStatusBarPadding

/**
 * Standardized Top Bar composable adhering to Reflex Design System v1.0:
 * - Proper status-bar inset handling via reflexStatusBarPadding()
 * - Standardized 48dp action bar height (44dp visual buttons with 48dp touch targets)
 * - 6dp gap between action buttons
 * - Standard back button via TopBarIconButton("back")
 * - Settings gear button always rightmost
 */
@Composable
fun ReflexTopBar(
    title: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    onBackClick: (() -> Unit)? = null,
    eyebrow: String? = null,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .reflexStatusBarPadding()
            .padding(horizontal = ReflexTokens.SpaceLg, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (onBackClick != null) {
                    TopBarIconButton(
                        iconName = "back",
                        contentDescriptionText = "Back",
                        onClick = onBackClick
                    )
                } else if (navigationIcon != null) {
                    navigationIcon()
                }

                Column {
                    if (eyebrow != null) {
                        Text(
                            text = eyebrow,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CopperPrimary,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = title,
                        fontFamily = Lora,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    actions()
                }
            }
        }

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}
