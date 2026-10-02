package com.reflex.app.data

data class RoutineTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconKey: String,
    val restBetweenStepsEnabled: Boolean = false,
    val restDurationSeconds: Int = 15,
    val steps: List<StepTemplate>
)

data class StepTemplate(
    val name: String,
    val stepType: StepType,
    val durationSeconds: Int? = null,
    val targetCount: Int? = null,
    val notes: String? = null,
    val emoji: String? = null
)

object RoutineTemplates {

    val templates: List<RoutineTemplate> = listOf(
        RoutineTemplate(
            id = "morning_routine",
            name = "Morning Routine",
            description = "Start your day with hydration, sunlight, movement, and focus.",
            iconKey = "SUNNY",
            steps = listOf(
                StepTemplate("Drink a glass of water", StepType.CHECK_OFF, emoji = "💧"),
                StepTemplate("Step outside / get sunlight", StepType.TIMED, durationSeconds = 300, emoji = "☀️"),
                StepTemplate("Stretch", StepType.TIMED, durationSeconds = 300, emoji = "🧘"),
                StepTemplate("Breathe / meditate", StepType.TIMED, durationSeconds = 300, emoji = "🧘‍♂️"),
                StepTemplate("Plan today's top 3 priorities", StepType.CHECK_OFF, notes = "Write down the 3 things that matter most today", emoji = "📝"),
                StepTemplate("Make your bed", StepType.CHECK_OFF, emoji = "🛏️")
            )
        ),
        RoutineTemplate(
            id = "night_wind_down",
            name = "Night Wind-Down",
            description = "10-3-2-1-0 ADHD-friendly calm wind-down structure before bed.",
            iconKey = "BEDTIME",
            steps = listOf(
                StepTemplate("Set out tomorrow's clothes/bag", StepType.CHECK_OFF, notes = "Removes decisions from tomorrow morning", emoji = "🎒"),
                StepTemplate("Screens off, phone away", StepType.CHECK_OFF, emoji = "📵"),
                StepTemplate("Light stretch or deep breathing", StepType.TIMED, durationSeconds = 300, emoji = "🧘"),
                StepTemplate("One-line journal / brain dump", StepType.TIMED, durationSeconds = 180, notes = "Write down anything on your mind — no editing, just dump it", emoji = "📓"),
                StepTemplate("Read or low-stimulation activity", StepType.TIMED, durationSeconds = 600, emoji = "📖"),
                StepTemplate("Lights down, into bed", StepType.CHECK_OFF, emoji = "🌙")
            )
        ),
        RoutineTemplate(
            id = "focus_pomodoro",
            name = "Focus Pomodoro",
            description = "Classic Pomodoro cycles (25m focus / 5m break) with long break.",
            iconKey = "TIMER",
            steps = listOf(
                StepTemplate("Focus session", StepType.TIMED, durationSeconds = 1500, emoji = "💻"),
                StepTemplate("Short break", StepType.TIMED, durationSeconds = 300, emoji = "☕"),
                StepTemplate("Focus session", StepType.TIMED, durationSeconds = 1500, emoji = "💻"),
                StepTemplate("Short break", StepType.TIMED, durationSeconds = 300, emoji = "☕"),
                StepTemplate("Long break", StepType.TIMED, durationSeconds = 900, emoji = "🌴")
            )
        ),
        RoutineTemplate(
            id = "workout_circuit",
            name = "Workout",
            description = "7-minute-style interval circuit with automatic 15s rest breaks.",
            iconKey = "FITNESS",
            restBetweenStepsEnabled = true,
            restDurationSeconds = 15,
            steps = listOf(
                StepTemplate("Jumping jacks", StepType.TIMED, durationSeconds = 30, emoji = "🏃"),
                StepTemplate("Wall sit", StepType.TIMED, durationSeconds = 30, emoji = "🦵"),
                StepTemplate("Push-ups", StepType.REPEAT_COUNT, targetCount = 15, emoji = "💪"),
                StepTemplate("Ab crunches", StepType.REPEAT_COUNT, targetCount = 20, emoji = "🏋️"),
                StepTemplate("Squats", StepType.REPEAT_COUNT, targetCount = 20, emoji = "🦵"),
                StepTemplate("Step-ups / high knees", StepType.TIMED, durationSeconds = 30, emoji = "🏃‍♂️")
            )
        ),
        RoutineTemplate(
            id = "daily_reset",
            name = "Daily Reset",
            description = "Midday or evening reset to clear clutter and refocus.",
            iconKey = "BOLT",
            steps = listOf(
                StepTemplate("Tidy your space", StepType.TIMED, durationSeconds = 300, emoji = "🧹"),
                StepTemplate("Drink water", StepType.CHECK_OFF, emoji = "💧"),
                StepTemplate("Review today's tasks", StepType.CHECK_OFF, emoji = "📋"),
                StepTemplate("Quick walk or stretch", StepType.TIMED, durationSeconds = 300, emoji = "🚶"),
                StepTemplate("Set tomorrow's top priority", StepType.CHECK_OFF, emoji = "🎯")
            )
        )
    )
}
