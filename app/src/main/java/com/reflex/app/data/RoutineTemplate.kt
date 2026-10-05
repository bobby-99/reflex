package com.reflex.app.data

import java.time.DayOfWeek

data class RoutineTemplate(
    val id: String,
    val name: String,
    val description: String,
    val iconKey: String,
    val restBetweenStepsEnabled: Boolean = false,
    val restDurationSeconds: Int = 15,
    val scheduledDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
    ),
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
        ),
        RoutineTemplate(
            id = "deep_work_launch",
            name = "Deep Work Launch",
            description = "Get clear, remove distractions, and enter deep focus in 5 minutes.",
            iconKey = "WORK",
            steps = listOf(
                StepTemplate("Phone out of reach", StepType.CHECK_OFF, emoji = "📵"),
                StepTemplate("Water filled, desk clear", StepType.CHECK_OFF, emoji = "💧"),
                StepTemplate("Brain dump (write down everything on your mind)", StepType.TIMED, durationSeconds = 120, notes = "Write down everything on your mind", emoji = "📝"),
                StepTemplate("Pick the one task, in one sentence", StepType.CHECK_OFF, emoji = "🎯"),
                StepTemplate("Define what \"done\" looks like", StepType.CHECK_OFF, emoji = "✅"),
                StepTemplate("Box breathing (4-4-4-4)", StepType.TIMED, durationSeconds = 60, emoji = "🧘"),
                StepTemplate("Close everything you don't need", StepType.CHECK_OFF, emoji = "💻")
            )
        ),
        RoutineTemplate(
            id = "desk_break",
            name = "Desk Break",
            description = "Quick 3-minute physical and visual reset for desk workers.",
            iconKey = "MEDITATE",
            restBetweenStepsEnabled = true,
            restDurationSeconds = 10,
            steps = listOf(
                StepTemplate("Look 20 ft away (20-20-20 rule)", StepType.TIMED, durationSeconds = 20, emoji = "👀"),
                StepTemplate("Neck rolls", StepType.TIMED, durationSeconds = 30, emoji = "🔄"),
                StepTemplate("Shoulder rolls", StepType.REPEAT_COUNT, targetCount = 10, emoji = "💪"),
                StepTemplate("Chest opener (hands clasped behind you)", StepType.TIMED, durationSeconds = 30, emoji = "🧘"),
                StepTemplate("Wrist flexor and extensor stretch", StepType.TIMED, durationSeconds = 30, emoji = "✋"),
                StepTemplate("Standing forward fold", StepType.TIMED, durationSeconds = 30, emoji = "🧘‍♂️"),
                StepTemplate("Drink water", StepType.CHECK_OFF, emoji = "💧")
            )
        ),
        RoutineTemplate(
            id = "mobility_flow",
            name = "Mobility Flow",
            description = "Full-body joint mobility and dynamic stretching sequence.",
            iconKey = "RUN",
            restBetweenStepsEnabled = true,
            restDurationSeconds = 15,
            steps = listOf(
                StepTemplate("Wrist prep", StepType.TIMED, durationSeconds = 45, emoji = "✋"),
                StepTemplate("Cat-cow", StepType.TIMED, durationSeconds = 60, emoji = "🐈"),
                StepTemplate("Thoracic rotations (per side)", StepType.REPEAT_COUNT, targetCount = 8, notes = "8 reps per side", emoji = "🔄"),
                StepTemplate("Deep squat hold", StepType.TIMED, durationSeconds = 60, emoji = "🦵"),
                StepTemplate("90/90 hip switch", StepType.TIMED, durationSeconds = 60, emoji = "🧘"),
                StepTemplate("Shoulder dislocates (band or towel)", StepType.REPEAT_COUNT, targetCount = 10, emoji = "💪"),
                StepTemplate("Hamstring fold", StepType.TIMED, durationSeconds = 60, emoji = "🧘‍♂️"),
                StepTemplate("Dead hang", StepType.TIMED, durationSeconds = 30, emoji = "🏋️"),
                StepTemplate("Child's pose", StepType.TIMED, durationSeconds = 60, emoji = "🧘")
            )
        ),
        RoutineTemplate(
            id = "evening_shutdown",
            name = "Evening Shutdown",
            description = "Close open loops and transition cleanly from work to evening.",
            iconKey = "CHECK",
            scheduledDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            ),
            steps = listOf(
                StepTemplate("Capture loose ends and open tabs", StepType.TIMED, durationSeconds = 120, emoji = "📋"),
                StepTemplate("Move unfinished tasks to a new date", StepType.CHECK_OFF, emoji = "📅"),
                StepTemplate("Pick tomorrow's top 3", StepType.CHECK_OFF, emoji = "🎯"),
                StepTemplate("Glance at tomorrow's calendar", StepType.CHECK_OFF, emoji = "🗓️"),
                StepTemplate("Clear desk, close laptop", StepType.CHECK_OFF, emoji = "💻"),
                StepTemplate("Log one win from today", StepType.TIMED, durationSeconds = 60, emoji = "🌟"),
                StepTemplate("Say \"shutdown complete\"", StepType.CHECK_OFF, emoji = "🔒")
            )
        ),
        RoutineTemplate(
            id = "weekly_review",
            name = "Weekly Review",
            description = "GTD-style weekly reflection, cleanup, and next week planning.",
            iconKey = "STAR",
            scheduledDays = setOf(DayOfWeek.SUNDAY),
            steps = listOf(
                StepTemplate("Empty your head", StepType.TIMED, durationSeconds = 180, emoji = "🧠"),
                StepTemplate("Review last week's wins (habits and completed tasks)", StepType.TIMED, durationSeconds = 120, emoji = "🏆"),
                StepTemplate("Handle overdue and missed items", StepType.TIMED, durationSeconds = 180, emoji = "🧹"),
                StepTemplate("Scan the next 7 days of calendar", StepType.TIMED, durationSeconds = 120, emoji = "📅"),
                StepTemplate("Set 3 priorities for the week", StepType.TIMED, durationSeconds = 180, emoji = "🎯"),
                StepTemplate("Schedule your focus blocks", StepType.CHECK_OFF, emoji = "⏱️"),
                StepTemplate("Pick one habit to tighten or drop", StepType.CHECK_OFF, emoji = "🌱"),
                StepTemplate("Prep tomorrow (bag, clothes)", StepType.CHECK_OFF, emoji = "🎒")
            )
        )
    )
}
