package com.reflex.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reflex.app.ReflexApplication
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    AlarmScheduler.rescheduleAllAlarms(context)
                } catch (e: Exception) {
                    AppLog.e("BootReceiver", "Failed to reschedule alarms on boot", e)
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }
}
