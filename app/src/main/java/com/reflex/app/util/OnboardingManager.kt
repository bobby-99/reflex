package com.reflex.app.util

import android.content.Context

object OnboardingManager {
    private const val PREFS_NAME = "reflex_prefs"
    private const val KEY_HAS_COMPLETED_ONBOARDING = "has_completed_onboarding"

    fun hasCompletedOnboarding(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_COMPLETED_ONBOARDING, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_ONBOARDING, completed).apply()
    }

    fun resetOnboarding(context: Context) {
        setOnboardingCompleted(context, false)
    }
}
