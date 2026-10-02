package com.reflex.app.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.Priority
import com.reflex.app.util.OnboardingManager
import com.reflex.app.util.ParsedTaskInput
import com.reflex.app.util.SettingsRepository
import com.reflex.app.util.TaskParser
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

data class TourPermissionStatus(
    val notifAllowed: Boolean = false,
    val exactAlarmAllowed: Boolean = false,
    val batUnrestricted: Boolean = false
) {
    val allowedCount: Int
        get() = (if (notifAllowed) 1 else 0) + (if (exactAlarmAllowed) 1 else 0) + (if (batUnrestricted) 1 else 0)
    val allAllowed: Boolean
        get() = allowedCount == 3
}

class TourViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    // --- Page 0: Welcome & User Name ---
    var userName by mutableStateOf("")
        private set

    val greetingPrefix: String
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when {
                hour < 12 -> "Good morning"
                hour < 18 -> "Good afternoon"
                else -> "Good evening"
            }
        }

    val greetingText: String
        get() = "$greetingPrefix, ${userName.trim().ifEmpty { "there" }}."

    val completionGreeting: String
        get() {
            val name = userName.trim()
            return if (name.isNotEmpty()) "$greetingPrefix, $name" else greetingPrefix
        }

    fun updateUserName(name: String) {
        val trimmed = name.take(20)
        userName = trimmed
        SettingsRepository.setUserName(context, trimmed)
    }

    // --- Page 1: Routines Demo (Auto-play loop) ---
    var routineStep by mutableIntStateOf(0)
        private set
    var routineStreakPopped by mutableStateOf(false)
        private set
    private var routineJob: Job? = null

    fun startRoutineDemo() {
        if (routineJob?.isActive == true) return
        routineStep = 0
        routineStreakPopped = false
        routineJob = viewModelScope.launch {
            while (isActive) {
                delay(1100)
                val next = (routineStep + 1) % 10
                routineStreakPopped = (next == 6)
                routineStep = next
            }
        }
    }

    fun stopRoutineDemo() {
        routineJob?.cancel()
        routineJob = null
    }

    // --- Page 2: Lightning Task Capture Demo ---
    var captureInput by mutableStateOf("")
        private set
    var parsedResult by mutableStateOf(TaskParser.parse(""))
        private set
    var extractedChips by mutableStateOf<List<String>>(emptyList())
        private set
    private var typewriterJob: Job? = null

    fun setCaptureText(text: String) {
        typewriterJob?.cancel()
        captureInput = text
        val res = TaskParser.parse(text)
        parsedResult = res
        extractedChips = computeChips(text, res)
    }

    fun startTypewriter() {
        typewriterJob?.cancel()
        captureInput = ""
        parsedResult = TaskParser.parse("")
        extractedChips = emptyList()

        val fullText = "team sync tmrw 10am 45m !!"
        typewriterJob = viewModelScope.launch {
            delay(500)
            for (i in 1..fullText.length) {
                if (!isActive) break
                val sub = fullText.substring(0, i)
                captureInput = sub
                val res = TaskParser.parse(sub)
                parsedResult = res
                extractedChips = computeChips(sub, res)
                delay(70)
            }
        }
    }

    fun stopTypewriter() {
        typewriterJob?.cancel()
        typewriterJob = null
    }

    private fun computeChips(raw: String, parsed: ParsedTaskInput): List<String> {
        val chips = mutableListOf<String>()
        val today = LocalDate.now()

        parsed.dueDate?.let { epochMillis ->
            val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            val dateLabel = when {
                date == today -> "Today"
                date == today.plusDays(1) -> "Tomorrow"
                date == today.plusWeeks(1) -> "Next week"
                else -> date.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH))
            }
            chips.add(dateLabel)
        }

        parsed.dueTime?.let { timeMillis ->
            try {
                val time = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                chips.add(time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)).lowercase())
            } catch (_: Exception) {
            }
        }

        when (parsed.priority) {
            Priority.HIGH -> chips.add("High priority")
            Priority.MEDIUM -> chips.add("Medium priority")
            Priority.LOW -> chips.add("Low priority")
            Priority.NONE -> Unit
        }

        return chips
    }

    // --- Page 3: Habits Demo (Interactive in-memory) ---
    var habitMeditateDone by mutableStateOf(false)
        private set
    var habitWaterGlasses by mutableIntStateOf(5)
        private set
    var habitScreenTimeHours by mutableFloatStateOf(2.0f)
        private set
    var habitStreakPopped by mutableStateOf(false)
        private set

    val habitDoneList: List<Boolean>
        get() = listOf(
            habitMeditateDone,
            habitWaterGlasses >= 8,
            habitScreenTimeHours > 0f && habitScreenTimeHours <= 3.0f
        )

    val habitDoneCount: Int
        get() = habitDoneList.count { it }

    val habitStreakCount: Int
        get() = 5 + (if (habitDoneCount == 3) 1 else 0)

    fun resetHabitsDemo() {
        habitMeditateDone = false
        habitWaterGlasses = 5
        habitScreenTimeHours = 2.0f
        habitStreakPopped = false
    }

    fun toggleMeditate() {
        val prevAll = habitDoneCount == 3
        habitMeditateDone = !habitMeditateDone
        checkHabitStreakPop(prevAll)
    }

    fun adjustWater(delta: Int) {
        val prevAll = habitDoneCount == 3
        habitWaterGlasses = (habitWaterGlasses + delta).coerceIn(0, 24)
        checkHabitStreakPop(prevAll)
    }

    fun adjustScreenTime(delta: Float) {
        val prevAll = habitDoneCount == 3
        habitScreenTimeHours = ((habitScreenTimeHours + delta) * 10).toInt() / 10f
        habitScreenTimeHours = habitScreenTimeHours.coerceIn(0f, 12f)
        checkHabitStreakPop(prevAll)
    }

    private fun checkHabitStreakPop(prevAll: Boolean) {
        val nowAll = habitDoneCount == 3
        if (!prevAll && nowAll) {
            habitStreakPopped = true
        }
    }

    // --- Page 4: Focus & App Blocking Demo ---
    var appBlockingEnabled by mutableStateOf(true)
        private set

    fun toggleAppBlocking() {
        appBlockingEnabled = !appBlockingEnabled
    }

    fun startFocusDemo() {
        // Continuous draining is managed in TourFocusPage via infinite transition
    }

    fun stopFocusDemo() {
    }

    // --- Page 5: Permissions Live State ---
    var permissionStatus by mutableStateOf(TourPermissionStatus())
        private set

    fun refreshPermissions() {
        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            true
        }

        val exactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: true
        } else {
            true
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val bat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } else {
            true
        }

        permissionStatus = TourPermissionStatus(
            notifAllowed = notif,
            exactAlarmAllowed = exactAlarm,
            batUnrestricted = bat
        )
    }

    // Fast simulator for "Set up now in settings" when user taps it in Tour
    fun simulateAllowAll(onFinished: () -> Unit) {
        viewModelScope.launch {
            permissionStatus = permissionStatus.copy(notifAllowed = true)
            delay(350)
            permissionStatus = permissionStatus.copy(exactAlarmAllowed = true)
            delay(350)
            permissionStatus = permissionStatus.copy(batUnrestricted = true)
            delay(350)
            onFinished()
        }
    }

    // --- Finish Tour / Completion ---
    var showCompletionScreen by mutableStateOf(false)
        private set

    fun showDoneOverlay() {
        stopRoutineDemo()
        stopTypewriter()
        stopFocusDemo()
        showCompletionScreen = true
    }

    fun replayTour() {
        showCompletionScreen = false
        resetHabitsDemo()
        appBlockingEnabled = true
    }

    fun completeOnboarding(onFinished: () -> Unit) {
        OnboardingManager.setOnboardingCompleted(context, true)
        onFinished()
    }

    init {
        userName = SettingsRepository.getUserName(context)
        refreshPermissions()
    }
}
