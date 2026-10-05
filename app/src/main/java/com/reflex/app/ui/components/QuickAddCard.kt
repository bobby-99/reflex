package com.reflex.app.ui.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.ui.theme.ReflexTheme
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.actionPill
import com.reflex.app.ui.theme.elevatedSurface
import com.reflex.app.ui.theme.onActionPill
import com.reflex.app.ui.theme.tertiaryText
import com.reflex.app.util.TaskHighlightHelper
import com.reflex.app.util.TaskHighlightVisualTransformation
import com.reflex.app.util.TaskParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * UI state holder for the QuickAddCard.
 * Hoisted via rememberSaveable so draft text and manual overrides survive dismissals.
 */
@Stable
class QuickAddState(
    initialText: String = "",
    initialManualPriority: String? = null,
    initialManualDate: String? = null,
    initialManualRepeat: String? = null,
    initialTuneOpen: Boolean = false,
    initialAddedCount: Int = 0
) {
    var text by mutableStateOf(initialText)
    var manualPriority by mutableStateOf(initialManualPriority)
    var manualDate by mutableStateOf(initialManualDate)
    var manualRepeat by mutableStateOf(initialManualRepeat)
    var isTuneOpen by mutableStateOf(initialTuneOpen)
    var addedCount by mutableIntStateOf(initialAddedCount)
    var confirmationMessage by mutableStateOf<String?>(null)
    var isListening by mutableStateOf(false)
    val focusRequester = FocusRequester()

    val parsed: com.reflex.app.util.ParsedTaskInput
        get() = TaskParser.parse(text)

    val cleanTitle: String
        get() {
            val p = parsed
            return if (p.title.isNotBlank()) {
                p.title.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                }
            } else ""
        }

    val effectiveDate: String?
        get() = manualDate ?: parsed.dueDate?.let { millis ->
            val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            val today = LocalDate.now()
            when (date) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                today.plusDays(7) -> "Next week"
                else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
            }
        }

    val effectiveTime: String?
        get() = parsed.dueTime?.let { millis ->
            val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
            time.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
        }

    val effectiveRepeat: String?
        get() = manualRepeat ?: when (parsed.recurrenceFrequency) {
            RecurrenceFrequency.DAILY -> "Every day"
            RecurrenceFrequency.WEEKLY -> "Every week"
            RecurrenceFrequency.MONTHLY -> "Every month"
            RecurrenceFrequency.YEARLY -> "Every year"
            RecurrenceFrequency.CUSTOM -> "Every custom"
            RecurrenceFrequency.NONE -> null
        }

    val effectivePriority: String?
        get() = manualPriority ?: when (parsed.priority) {
            Priority.HIGH -> "High"
            Priority.MEDIUM -> "Medium"
            Priority.LOW -> "Low"
            Priority.NONE -> null
        }

    fun submitTask(onAddTask: (String) -> Unit) {
        val title = cleanTitle
        if (title.isBlank()) return

        val tokensToAppend = mutableListOf<String>()
        if (manualRepeat != null) tokensToAppend.add(manualRepeat!!.lowercase())
        if (manualPriority != null) tokensToAppend.add("!${manualPriority!!.lowercase()}")

        val submissionText = if (manualDate != null) {
            val timePart = effectiveTime ?: ""
            "$title ${manualDate!!.lowercase()} $timePart ${tokensToAppend.joinToString(" ")}".trim()
        } else if (tokensToAppend.isNotEmpty()) {
            "$text ${tokensToAppend.joinToString(" ")}".trim()
        } else {
            text
        }

        val metaParts = listOfNotNull(
            effectiveDate,
            effectiveTime,
            effectiveRepeat,
            effectivePriority?.let { "$it priority" }
        ).joinToString(" · ")

        val confMessage = if (metaParts.isNotEmpty()) {
            "Added: $title · $metaParts"
        } else {
            "Added: $title"
        }

        onAddTask(submissionText)

        addedCount++
        confirmationMessage = confMessage
        clearDraftAndOverrides()

        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    fun clearDraftAndOverrides() {
        text = ""
        manualPriority = null
        manualDate = null
        manualRepeat = null
    }

    companion object {
        val Saver: Saver<QuickAddState, *> = mapSaver(
            save = {
                mapOf(
                    "text" to it.text,
                    "manualPriority" to it.manualPriority,
                    "manualDate" to it.manualDate,
                    "manualRepeat" to it.manualRepeat,
                    "isTuneOpen" to it.isTuneOpen,
                    "addedCount" to it.addedCount
                )
            },
            restore = { map ->
                QuickAddState(
                    initialText = (map["text"] as? String) ?: "",
                    initialManualPriority = map["manualPriority"] as? String,
                    initialManualDate = map["manualDate"] as? String,
                    initialManualRepeat = map["manualRepeat"] as? String,
                    initialTuneOpen = (map["isTuneOpen"] as? Boolean) ?: false,
                    initialAddedCount = (map["addedCount"] as? Int) ?: 0
                )
            }
        )
    }
}

@Composable
fun rememberQuickAddState(): QuickAddState {
    return rememberSaveable(saver = QuickAddState.Saver) {
        QuickAddState()
    }
}

enum class ChipField {
    DATE,
    TIME,
    REPEAT,
    PRIORITY
}

data class TokenChipData(
    val field: ChipField,
    val label: String,
    val value: String,
    val icon: ImageVector,
    val isManual: Boolean,
    val isHighPriority: Boolean = false,
    val isLowPriority: Boolean = false
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddCard(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onAddTask: (String) -> Unit,
    onMicClick: () -> Unit,
    state: QuickAddState = rememberQuickAddState(),
    modifier: Modifier = Modifier,
    initialDate: LocalDate? = null
) {
    if (!isOpen) return

    BackHandler(onBack = onDismiss)

    val coroutineScope = rememberCoroutineScope()
    var isInputFocused by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    // Prefill initial date if provided
    LaunchedEffect(isOpen, initialDate) {
        if (isOpen && initialDate != null && state.manualDate == null) {
            state.manualDate = initialDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
        }
    }

    // Auto-focus input when opened
    LaunchedEffect(isOpen) {
        if (isOpen) {
            delay(150)
            try {
                state.focusRequester.requestFocus()
            } catch (_: Exception) {}
        }
    }

    // Auto-dismiss confirmation pill after 3.2 seconds
    LaunchedEffect(state.confirmationMessage) {
        if (state.confirmationMessage != null) {
            delay(3200)
            state.confirmationMessage = null
        }
    }

    val cleanTitle by remember(state.text) {
        derivedStateOf { state.cleanTitle }
    }

    val effectiveDate by remember(state.text, state.manualDate) {
        derivedStateOf { state.effectiveDate }
    }

    val effectiveTime by remember(state.text) {
        derivedStateOf { state.effectiveTime }
    }

    val effectiveRepeat by remember(state.text, state.manualRepeat) {
        derivedStateOf { state.effectiveRepeat }
    }

    val effectivePriority by remember(state.text, state.manualPriority) {
        derivedStateOf { state.effectivePriority }
    }

    // Derived list of chips in strict order: date, time, repeat, priority
    val chips by remember(effectiveDate, effectiveTime, effectiveRepeat, effectivePriority, state.manualDate, state.manualRepeat, state.manualPriority) {
        derivedStateOf {
            val list = mutableListOf<TokenChipData>()
            effectiveDate?.let {
                list.add(
                    TokenChipData(
                        field = ChipField.DATE,
                        label = "date",
                        value = it,
                        icon = Icons.Default.CalendarMonth,
                        isManual = state.manualDate != null
                    )
                )
            }
            effectiveTime?.let {
                list.add(
                    TokenChipData(
                        field = ChipField.TIME,
                        label = "time",
                        value = it,
                        icon = Icons.Default.Schedule,
                        isManual = false
                    )
                )
            }
            effectiveRepeat?.let {
                list.add(
                    TokenChipData(
                        field = ChipField.REPEAT,
                        label = "repeat",
                        value = it,
                        icon = Icons.Default.Repeat,
                        isManual = state.manualRepeat != null
                    )
                )
            }
            effectivePriority?.let {
                list.add(
                    TokenChipData(
                        field = ChipField.PRIORITY,
                        label = "priority",
                        value = "$it priority",
                        icon = Icons.Default.Flag,
                        isManual = state.manualPriority != null,
                        isHighPriority = it.equals("High", ignoreCase = true),
                        isLowPriority = it.equals("Low", ignoreCase = true)
                    )
                )
            }
            list
        }
    }

    fun submitTask() {
        state.submitTask(onAddTask)
    }

    fun removeChip(chip: TokenChipData) {
        when (chip.field) {
            ChipField.DATE -> {
                if (state.manualDate != null) {
                    state.manualDate = null
                } else {
                    val dateRegex = Regex("(?i)\\b(?:today|tonight|tomorrow|tmrw|next week|day after tomorrow|(?:next\\s+)?(?:sunday|monday|tuesday|wednesday|thursday|friday|saturday|mon|tue|wed|thu|fri|sat|sun))\\b")
                    state.text = state.text.replaceFirst(dateRegex, " ").replace(Regex("\\s+"), " ").trim()
                }
            }
            ChipField.TIME -> {
                val timeRegex = Regex("(?i)\\b(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b")
                state.text = state.text.replaceFirst(timeRegex, " ").replace(Regex("\\s+"), " ").trim()
            }
            ChipField.REPEAT -> {
                if (state.manualRepeat != null) {
                    state.manualRepeat = null
                } else {
                    val repeatRegex = Regex("(?i)\\b(?:every\\s+(?:day|week|month|weekday|mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)|daily|weekly|monthly)\\b")
                    state.text = state.text.replaceFirst(repeatRegex, " ").replace(Regex("\\s+"), " ").trim()
                }
            }
            ChipField.PRIORITY -> {
                if (state.manualPriority != null) {
                    state.manualPriority = null
                } else {
                    val priRegex = Regex("(?i)\\b(?:!high|!!!|urgent|high priority|p1|!med(?:ium)?|!!|medium priority|p2|!low|low priority|p3)\\b")
                    state.text = state.text.replaceFirst(priRegex, " ").replace(Regex("\\s+"), " ").trim()
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Scrim covering content only
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        val density = LocalDensity.current
        val slideOffsetPx = with(density) { 40.dp.roundToPx() }

        // Floating QuickAddCard Container
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInVertically(
                initialOffsetY = { slideOffsetPx },
                animationSpec = tween(280, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(280, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(
                targetOffsetY = { slideOffsetPx },
                animationSpec = tween(200, easing = FastOutLinearInEasing)
            ) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            val isImeVisible = WindowInsets.isImeVisible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 10.dp)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = if (isImeVisible) 10.dp else 96.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(max = 398.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                        .shadow(
                            elevation = 16.dp,
                            shape = ReflexTokens.ShapeCard,
                            ambientColor = Color.Black.copy(alpha = 0.28f),
                            spotColor = Color.Black.copy(alpha = 0.28f)
                        )
                        .semantics { contentDescription = "Quick add task" },
                    shape = ReflexTokens.ShapeCard,
                    color = MaterialTheme.colorScheme.elevatedSurface,
                    border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 10.dp)
                    ) {
                        // 1. Grab Handle (40x5dp pill, border color, margin bottom 10dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 5.dp)
                                    .clip(ReflexTokens.ShapeChip)
                                    .background(MaterialTheme.colorScheme.outline)
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragEnd = {
                                                if (dragOffsetY > 60.dp.toPx()) {
                                                    onDismiss()
                                                }
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                dragOffsetY = 0f
                                            },
                                            onVerticalDrag = { _, dragAmount ->
                                                dragOffsetY = maxOf(0f, dragOffsetY + dragAmount)
                                            }
                                        )
                                    }
                            )
                        }

                        // 2. Header: "Quick add task" (19sp SemiBold), "N added" (13sp secondary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick add task",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 23.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (state.addedCount > 0) {
                                Text(
                                    text = "${state.addedCount} added",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 3. Confirmation Pill
                        AnimatedVisibility(
                            visible = state.confirmationMessage != null,
                            enter = fadeIn(tween(250)) + expandVertically(tween(250)),
                            exit = fadeOut(tween(200)) + shrinkVertically(tween(200))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .clip(ReflexTokens.ShapeChip)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = state.confirmationMessage ?: "",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 4. Input Field (56dp tall, 24dp radius, SurfaceInput bg, 1.5dp border, mic trailing)
                        val inputBorder = if (isInputFocused) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                        val highlightPillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                        val highlightRanges by remember(state.text) {
                            derivedStateOf { TaskHighlightHelper.findHighlightRanges(state.text) }
                        }
                        var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(ReflexTokens.ShapeInput)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(BorderStroke(1.5.dp, inputBorder), ReflexTokens.ShapeInput)
                                .padding(start = 18.dp, end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (state.text.isEmpty()) {
                                    Text(
                                        text = com.reflex.app.util.QuickAddHelperData.PLACEHOLDER_TEXT,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                        color = MaterialTheme.colorScheme.tertiaryText
                                    )
                                }
                                BasicTextField(
                                    value = state.text,
                                    onValueChange = { state.text = it },
                                    onTextLayout = { textLayoutResult = it },
                                    visualTransformation = remember {
                                        TaskHighlightVisualTransformation(
                                            textColor = Color.Unspecified,
                                            highlightColor = Color.Transparent,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(state.focusRequester)
                                        .onFocusChanged { isInputFocused = it.isFocused }
                                        .drawBehind {
                                            textLayoutResult?.let { layout ->
                                                if (state.text.isNotEmpty()) {
                                                    val textLen = state.text.length
                                                    for (range in highlightRanges) {
                                                        val s = range.first.coerceIn(0, textLen)
                                                        val e = (range.last + 1).coerceIn(0, textLen)
                                                        if (s < e && s < layout.layoutInput.text.length && e <= layout.layoutInput.text.length) {
                                                            val startLine = layout.getLineForOffset(s)
                                                            val endLine = layout.getLineForOffset((e - 1).coerceAtLeast(s))
                                                            for (line in startLine..endLine) {
                                                                val lineStart = layout.getLineStart(line)
                                                                val lineEnd = layout.getLineEnd(line)
                                                                val segStart = maxOf(s, lineStart)
                                                                val segEnd = minOf(e, lineEnd)
                                                                if (segStart < segEnd) {
                                                                    val left = layout.getHorizontalPosition(segStart, true)
                                                                    val right = layout.getHorizontalPosition(segEnd, true)
                                                                    val top = layout.getLineTop(line)
                                                                    val bottom = layout.getLineBottom(line)
                                                                    val minX = minOf(left, right)
                                                                    val maxX = maxOf(left, right)
                                                                    if (maxX > minX) {
                                                                        val hPad = 3.dp.toPx()
                                                                        val vPad = 1.dp.toPx()
                                                                        drawRoundRect(
                                                                            color = highlightPillColor,
                                                                            topLeft = Offset(minX - hPad, top + vPad),
                                                                            size = Size(
                                                                                (maxX - minX) + (hPad * 2),
                                                                                (bottom - top) - (vPad * 2)
                                                                            ),
                                                                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { submitTask() })
                                )
                            }

                            // 40dp Circular Mic Button
                            MicButton(
                                isListening = state.isListening,
                                onClick = onMicClick
                            )
                        }

                        // 5. Token Chips (12dp above, wrap, 8dp gaps, min height 34dp)
                        if (chips.isNotEmpty() || !state.isTuneOpen) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 34.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (chips.isEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Type naturally or tap an example to start:",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                                            color = MaterialTheme.colorScheme.tertiaryText
                                        )
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            com.reflex.app.util.QuickAddHelperData.EXAMPLES.take(3).forEach { ex ->
                                                Surface(
                                                    modifier = Modifier
                                                        .clip(CircleShape)
                                                        .clickable {
                                                            state.text = ex.exampleText
                                                        },
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant)
                                                ) {
                                                    Text(
                                                        text = ex.exampleText,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        chips.forEach { chipData ->
                                            TokenChip(
                                                chip = chipData,
                                                onRemove = { removeChip(chipData) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6. Tune Panel (Animated height, SurfaceVariant bg, 26dp radius, 14dp padding)
                        AnimatedVisibility(
                            visible = state.isTuneOpen,
                            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                            exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                                    .clip(ReflexTokens.ShapeInnerTile)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Priority Group
                                TuneOptionGroup(
                                    label = "Priority",
                                    options = listOf("Low", "Medium", "High"),
                                    selectedOption = effectivePriority,
                                    onOptionClick = { opt ->
                                        state.manualPriority = if (state.manualPriority == opt) null else opt
                                    }
                                )

                                // Date Group
                                TuneOptionGroup(
                                    label = "Date",
                                    options = listOf("Today", "Tomorrow", "Next week"),
                                    selectedOption = effectiveDate,
                                    onOptionClick = { opt ->
                                        state.manualDate = if (state.manualDate == opt) null else opt
                                    }
                                )

                                // Repeat Group
                                TuneOptionGroup(
                                    label = "Repeat",
                                    options = listOf("Every day", "Every week"),
                                    selectedOption = effectiveRepeat,
                                    onOptionClick = { opt ->
                                        state.manualRepeat = if (state.manualRepeat == opt) null else opt
                                    }
                                )
                            }
                        }

                        // 7. Bottom Action Row (14dp above)
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Tune Button (48dp tall)
                            val tuneBg = if (state.isTuneOpen) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                            val tuneFg = if (state.isTuneOpen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Row(
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(ReflexTokens.ShapeChip)
                                    .background(tuneBg)
                                    .clickable { state.isTuneOpen = !state.isTuneOpen }
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Tune task options",
                                    tint = tuneFg,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Tune",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = tuneFg
                                )
                            }

                            // Add Task Button (48dp, min 136dp, ActionPillWhite, press scale .97)
                            val canAdd = cleanTitle.isNotBlank()
                            val addInteractionSource = remember { MutableInteractionSource() }
                            val isAddPressed by addInteractionSource.collectIsPressedAsState()
                            val addScale by animateFloatAsState(
                                targetValue = if (isAddPressed && canAdd) ReflexTokens.PressedScale else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                                label = "add_scale"
                            )

                            Box(
                                modifier = Modifier
                                    .height(48.dp)
                                    .defaultMinSize(minWidth = 136.dp)
                                    .graphicsLayer {
                                        scaleX = addScale
                                        scaleY = addScale
                                        alpha = if (canAdd) 1f else 0.35f
                                    }
                                    .clip(ReflexTokens.ShapeChip)
                                    .background(MaterialTheme.colorScheme.actionPill)
                                    .clickable(
                                        enabled = canAdd,
                                        interactionSource = addInteractionSource,
                                        indication = null,
                                        onClick = { submitTask() }
                                    )
                                    .padding(horizontal = 22.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Add task",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.onActionPill
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val ringTransition = rememberInfiniteTransition(label = "mic_pulse")
    val ringScale by ringTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    val ringAlpha by ringTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isListening) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = ringAlpha
                    }
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isListening) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice input",
                modifier = Modifier.size(20.dp),
                tint = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TokenChip(
    chip: TokenChipData,
    onRemove: () -> Unit
) {
    val (bg, fg) = when {
        chip.isHighPriority -> Pair(
            MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.error
        )
        chip.isLowPriority -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.85f, animationSpec = tween(250)),
        exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.85f, animationSpec = tween(200))
    ) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .clip(ReflexTokens.ShapeChip)
                .background(bg)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = chip.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = fg
            )
            Text(
                text = chip.value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = fg
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .semantics { contentDescription = "Remove ${chip.label}, ${chip.value}" }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = fg.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TuneOptionGroup(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                val isSelected = selectedOption?.equals(opt, ignoreCase = true) == true
                val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                val border = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(ReflexTokens.ShapeChip)
                        .background(bg)
                        .border(BorderStroke(ReflexTokens.BorderHairline, border), ReflexTokens.ShapeChip)
                        .clickable { onOptionClick(opt) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = fg
                    )
                }
            }
        }
    }
}

// =========================================================================
// PREVIEWS (Dark and Light)
// =========================================================================

@Preview(name = "Card Empty - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Card Empty - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun QuickAddCardEmptyPreview() {
    ReflexTheme {
        val state = rememberQuickAddState()
        QuickAddCard(
            isOpen = true,
            onDismiss = {},
            onAddTask = {},
            onMicClick = {},
            state = state
        )
    }
}

@Preview(name = "Card With Chips - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Card With Chips - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun QuickAddCardWithChipsPreview() {
    ReflexTheme {
        val state = remember {
            QuickAddState(
                initialText = "Call mom tomorrow 5pm !high",
                initialAddedCount = 2
            ).apply {
                confirmationMessage = "Added: Sprint review · Today · 3:00 pm"
            }
        }
        QuickAddCard(
            isOpen = true,
            onDismiss = {},
            onAddTask = {},
            onMicClick = {},
            state = state
        )
    }
}

@Preview(name = "Card Tune Open - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Card Tune Open - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun QuickAddCardTuneOpenPreview() {
    ReflexTheme {
        val state = remember {
            QuickAddState(
                initialText = "Plan sprint review",
                initialTuneOpen = true,
                initialManualPriority = "High",
                initialManualDate = "Today"
            )
        }
        QuickAddCard(
            isOpen = true,
            onDismiss = {},
            onAddTask = {},
            onMicClick = {},
            state = state
        )
    }
}

@Preview(name = "Card Listening - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Card Listening - Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun QuickAddCardListeningPreview() {
    ReflexTheme {
        val state = remember {
            QuickAddState(initialText = "").apply {
                isListening = true
            }
        }
        QuickAddCard(
            isOpen = true,
            onDismiss = {},
            onAddTask = {},
            onMicClick = {},
            state = state
        )
    }
}
