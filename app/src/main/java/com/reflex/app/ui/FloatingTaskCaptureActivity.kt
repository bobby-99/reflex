package com.reflex.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.reflex.app.data.Priority
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.Task
import com.reflex.app.ui.components.MicPermissionRationaleDialog
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexTextField
import com.reflex.app.ui.theme.ActionPillWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.CopperSubtle
import com.reflex.app.ui.theme.ReflexTheme
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.SpeechRecognitionHelper
import com.reflex.app.util.TaskHighlightVisualTransformation
import com.reflex.app.util.TaskParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FloatingTaskCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = ReflexDatabase.getDatabase(applicationContext)
        val repository = ReflexRepository(db.routineDao(), db.stepDao(), db.completionLogDao(), db.taskDao())

        setContent {
            ReflexTheme {
                FloatingTaskCaptureScreen(
                    onDismiss = { finish() },
                    onSave = { taskTitle ->
                        if (taskTitle.isNotBlank()) {
                            val parsed = TaskParser.parse(taskTitle)
                            val taskToInsert = Task(
                                title = parsed.title.ifBlank { "Untitled Task" },
                                dueDate = parsed.dueDate,
                                dueTime = parsed.dueTime,
                                priority = parsed.priority
                            )

                            lifecycleScope.launch(Dispatchers.IO) {
                                val insertedId = repository.insertTask(taskToInsert)
                                val savedTask = taskToInsert.copy(id = insertedId)
                                if (savedTask.dueTime != null) {
                                    AlarmScheduler.scheduleTaskReminder(applicationContext, savedTask)
                                }
                            }
                            Toast.makeText(applicationContext, "Task captured: ${parsed.title}", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun FloatingTaskCaptureScreen(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var showMicRationale by remember { mutableStateOf(false) }

    val speechHelper = remember(context) {
        SpeechRecognitionHelper(
            context = context,
            onPartialResult = { text ->
                if (text.isNotBlank()) {
                    inputText = text
                }
            },
            onFinalResult = { text ->
                if (text.isNotBlank()) {
                    inputText = text
                }
                isListening = false
            },
            onError = { err ->
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                isListening = false
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            speechHelper.startListening()
        } else {
            Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechHelper.stopListening()
        }
    }

    val parsedResult = remember(inputText) {
        if (inputText.isNotBlank()) TaskParser.parse(inputText) else null
    }

    // Translucent outer backdrop dismissible on tap outside
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {},
            shape = ReflexTokens.ShapeDialog,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ReflexTokens.SpaceXl)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(ReflexTokens.ShapeChip)
                                .background(CopperSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = CopperPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(ReflexTokens.SpaceSm))
                        Text(
                            text = "Quick task capture",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                // Input Field with Live Highlighting & Mic Button
                ReflexTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = if (isListening) "Listening..." else "e.g. 'team sync tmrw 10am 45m !!'",
                    visualTransformation = TaskHighlightVisualTransformation(textColor = MaterialTheme.colorScheme.onSurface, highlightColor = CopperPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isListening) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    if (isListening) {
                                        speechHelper.stopListening()
                                        isListening = false
                                    } else {
                                        val hasPerm = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPerm) {
                                            isListening = true
                                            speechHelper.startListening()
                                        } else {
                                            showMicRationale = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Input",
                                tint = if (isListening) Color.Black else CopperPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )

                // Parsed Feedback Banner
                if (parsedResult != null) {
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                    val dateStr = parsedResult.dueDate?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                    } ?: "No date"

                    val timeStr = parsedResult.dueTime?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
                            .format(DateTimeFormatter.ofPattern("h:mm a"))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ReflexTokens.ShapeChip)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant), ReflexTokens.ShapeChip)
                            .padding(horizontal = ReflexTokens.SpaceSm, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed: $dateStr${if (timeStr != null) " at $timeStr" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CopperPrimary,
                                fontSize = 13.sp
                            )

                            if (parsedResult.priority != Priority.NONE) {
                                Text(
                                    text = parsedResult.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CopperPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

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
                        text = "Save task",
                        onClick = { onSave(inputText) },
                        modifier = Modifier.weight(1f),
                        variant = ReflexButtonVariant.PRIMARY
                    )
                }
            }
        }
    }

    if (showMicRationale) {
        MicPermissionRationaleDialog(
            onDismiss = { showMicRationale = false },
            onGrant = {
                showMicRationale = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        )
    }
}
