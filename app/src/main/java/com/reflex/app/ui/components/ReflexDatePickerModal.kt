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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class DatePickerDayCell(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

@Composable
fun ReflexDatePickerModal(
    initialDate: LocalDate = LocalDate.now(),
    title: String = "Select date",
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    var tempSelectedDate by remember { mutableStateOf(initialDate) }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    val daysInGrid = remember(displayedMonth) {
        buildDatePickerGrid(displayedMonth)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = ReflexTokens.ShapeDialog,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ReflexTokens.SpaceLg)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                // Month Header (Chevrons + centered month name)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = ReflexTokens.SpaceXs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { displayedMonth = displayedMonth.minusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(ReflexTokens.IconSm)
                        )
                    }

                    Text(
                        text = displayedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = ReflexTokens.SpaceSm)
                    )

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { displayedMonth = displayedMonth.plusMonths(1) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next month",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(ReflexTokens.IconSm)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                // Weekday Labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    days.forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                // Days Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(daysInGrid) { cell ->
                        val isSelected = cell.date == tempSelectedDate

                        val bgColor = when {
                            isSelected -> CopperPrimary
                            else -> Color.Transparent
                        }

                        val textColor = when {
                            isSelected -> ActionPillOnWhite
                            cell.isToday -> CopperPrimary
                            cell.isCurrentMonth -> MaterialTheme.colorScheme.onSurface
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        }

                        val cellBorder = when {
                            isSelected -> null
                            cell.isToday -> BorderStroke(ReflexTokens.BorderThin, CopperPrimary)
                            else -> null
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .then(
                                    if (cellBorder != null) Modifier.border(cellBorder, CircleShape)
                                    else Modifier
                                )
                                .background(bgColor)
                                .clickable {
                                    tempSelectedDate = cell.date
                                }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cell.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected || cell.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)
                ) {
                    ReflexButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        variant = ReflexButtonVariant.SECONDARY
                    )

                    ReflexButton(
                        text = "Select",
                        onClick = {
                            onDateSelected(tempSelectedDate)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        variant = ReflexButtonVariant.PRIMARY
                    )
                }
            }
        }
    }
}

private fun buildDatePickerGrid(month: YearMonth): List<DatePickerDayCell> {
    val list = mutableListOf<DatePickerDayCell>()
    val firstDay = month.atDay(1)
    val dayOfWeekVal = firstDay.dayOfWeek.value // 1 (Mon) to 7 (Sun)
    val leadingEmpty = dayOfWeekVal - 1

    val prevMonth = month.minusMonths(1)
    val prevMonthLength = prevMonth.lengthOfMonth()

    for (i in (prevMonthLength - leadingEmpty + 1)..prevMonthLength) {
        val date = prevMonth.atDay(i)
        list.add(DatePickerDayCell(date, isCurrentMonth = false, isToday = date == LocalDate.now()))
    }

    for (i in 1..month.lengthOfMonth()) {
        val date = month.atDay(i)
        list.add(DatePickerDayCell(date, isCurrentMonth = true, isToday = date == LocalDate.now()))
    }

    val totalCells = if (list.size > 35) 42 else 35
    val trailingEmpty = totalCells - list.size
    val nextMonth = month.plusMonths(1)
    for (i in 1..trailingEmpty) {
        val date = nextMonth.atDay(i)
        list.add(DatePickerDayCell(date, isCurrentMonth = false, isToday = date == LocalDate.now()))
    }

    return list
}
