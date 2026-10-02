package com.reflex.app.data

data class FocusPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val workDurationMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    val autoStartNextPhase: Boolean = false,
    val isDefault: Boolean = false
) {
    fun toFocusSettings(soundEnabled: Boolean = true, vibrationEnabled: Boolean = true, blockingMode: BlockingMode = BlockingMode.OFF, selectedPackages: String = ""): FocusSettings {
        return FocusSettings(
            workDurationMin = workDurationMin,
            shortBreakMin = shortBreakMin,
            longBreakMin = longBreakMin,
            sessionsBeforeLongBreak = sessionsBeforeLongBreak,
            autoStartNextPhase = autoStartNextPhase,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            blockingMode = blockingMode,
            selectedPackages = selectedPackages
        )
    }
}
