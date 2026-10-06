package com.reflex.app.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.telecom.TelecomManager
import android.provider.Telephony

object AppBlockPermissionHelper {

    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            data = Uri.parse("package:${context.packageName}")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val fallbackIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        }
    }

    fun openOverlaySettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }

    fun getHardcodedExemptPackages(context: Context): Set<String> {
        val set = mutableSetOf<String>()

        // 1. Reflex itself
        set.add(context.packageName)

        // 2. Default Dialer / Phone app
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val defaultDialer = telecomManager?.defaultDialerPackage
            if (defaultDialer != null && defaultDialer.isNotEmpty()) {
                set.add(defaultDialer)
            }
        } catch (e: Exception) {
            AppLog.w("AppBlockPermissionHelper", "Could not query default dialer package", e)
        }
        set.add("com.google.android.dialer")
        set.add("com.android.dialer")
        set.add("com.samsung.android.dialer")

        // 3. Default SMS app
        try {
            val defaultSms = Telephony.Sms.getDefaultSmsPackage(context)
            if (defaultSms != null && defaultSms.isNotEmpty()) {
                set.add(defaultSms)
            }
        } catch (e: Exception) {
            AppLog.w("AppBlockPermissionHelper", "Could not query default SMS package", e)
        }
        set.add("com.google.android.apps.messaging")
        set.add("com.android.mms")

        // 4. System Settings app
        set.add("com.android.settings")
        set.add("com.google.android.settings")

        // 5. System UI & Launcher
        set.add("com.android.systemui")

        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = context.packageManager.resolveActivity(launcherIntent, 0)
            val launcherPkg = resolveInfo?.activityInfo?.packageName
            if (launcherPkg != null && launcherPkg.isNotEmpty()) {
                set.add(launcherPkg)
            }
        } catch (e: Exception) {
            AppLog.w("AppBlockPermissionHelper", "Could not query default launcher package", e)
        }

        return set
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
}
