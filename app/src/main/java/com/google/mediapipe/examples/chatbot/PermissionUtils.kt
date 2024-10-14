// PermissionUtils.kt
package com.google.mediapipe.examples.chatbot

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

object PermissionUtils {

    /**
     * Checks if the PACKAGE_USAGE_STATS permission is granted.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Opens the Usage Access Settings screen.
     */
    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionUtils", "Error opening Usage Access Settings", e)
        }
    }
}
