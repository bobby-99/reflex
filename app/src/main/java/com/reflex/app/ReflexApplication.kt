package com.reflex.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.ReflexRepository
import com.reflex.app.util.NotificationHelper

class ReflexApplication : Application() {

    val database: ReflexDatabase by lazy { ReflexDatabase.getDatabase(this) }

    val repository: ReflexRepository by lazy {
        ReflexRepository(
            routineDao = database.routineDao(),
            stepDao = database.stepDao(),
            completionLogDao = database.completionLogDao(),
            taskDao = database.taskDao(),
            focusSettingsDao = database.focusSettingsDao(),
            focusSessionDao = database.focusSessionDao(),
            habitDao = database.habitDao(),
            focusTagDao = database.focusTagDao(),
            blockedAppEventDao = database.blockedAppEventDao()
        )
    }

    companion object {
        @Volatile
        var isAppInForeground: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        com.reflex.app.util.ThemePreferenceRepository.init(this)
        com.reflex.app.util.CalendarPreferenceRepository.init(this)
        com.reflex.app.util.FocusPresetRepository.init(this)
        com.reflex.app.util.UserProfileRepository.init(this)
        com.reflex.app.util.SettingsRepository.init(this)
        NotificationHelper.createNotificationChannels(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            private var startedCount = 0

            override fun onActivityStarted(activity: Activity) {
                startedCount++
                isAppInForeground = true
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount--
                if (startedCount <= 0) {
                    startedCount = 0
                    isAppInForeground = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
