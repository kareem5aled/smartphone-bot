// DeviceHealth.kt
package com.google.mediapipe.examples.chatbot

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DeviceHealth(
    private val context: Context
) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // Threshold for low memory (70%)
    private val MEMORY_LOW_THRESHOLD_PERCENTAGE = 80.0f

    /**
     * Retrieves a formatted string indicating RAM usage.
     * Example: "RAM Usage: 1500 MB / 2000 MB"
     */
    fun getMemoryStatus(): String {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024) // MB
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024) // MB
        val usedMemoryMB = totalMemoryMB - availableMemoryMB

        return "RAM Usage: $usedMemoryMB MB / $totalMemoryMB MB"
    }

    /**
     * Determines if the device is experiencing low memory based on a 70% usage threshold.
     * @return True if used memory is greater than 70% of total memory, else False.
     */
    fun isMemoryLow(): Boolean {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val availableMemoryMB = memoryInfo.availMem / (1024 * 1024) // MB
        val totalMemoryMB = memoryInfo.totalMem / (1024 * 1024) // MB
        val usedMemoryMB = totalMemoryMB - availableMemoryMB

        // Calculate the percentage of used memory
        val usedMemoryPercentage = (usedMemoryMB.toFloat() / totalMemoryMB.toFloat()) * 100

        // Log detailed memory information for debugging
        Log.d("MemoryInfo", "Used Memory: $usedMemoryMB MB / $totalMemoryMB MB ($usedMemoryPercentage%)")

        // Determine if used memory exceeds the threshold
        val isLow = usedMemoryPercentage > MEMORY_LOW_THRESHOLD_PERCENTAGE

        Log.d("MemoryStatus", "Is Memory Low (>$MEMORY_LOW_THRESHOLD_PERCENTAGE%): $isLow")

        return isLow
    }

    // Battery Status
    fun getBatteryLevel(): Int {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPct = if (scale > 0) (level * 100 / scale) else 0
        return batteryPct
    }

    fun isBatteryLow(threshold: Int = 20): Boolean {
        return getBatteryLevel() < threshold
    }

    fun isCharging(): Boolean {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    fun getBatteryHealth(): String {
        val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

        return when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
    }

    // Storage Space
    fun getStorageStatus(): String {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val availableBlocks: Long
        val totalBlocks: Long
        val blockSize: Long

        blockSize = statFs.blockSizeLong
        availableBlocks = statFs.availableBlocksLong
        totalBlocks = statFs.blockCountLong

        val availableStorage = (availableBlocks * blockSize) / (1024 * 1024) // MB
        val totalStorage = (totalBlocks * blockSize) / (1024 * 1024) // MB
        val usedStorage = totalStorage - availableStorage

        return "Storage Usage: $usedStorage MB / $totalStorage MB\nAvailable Storage: $availableStorage MB"
    }

    fun isStorageLow(thresholdPercentage: Float = 20.0f): Boolean {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val availableBlocks: Long
        val totalBlocks: Long

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            availableBlocks = statFs.availableBlocksLong
            totalBlocks = statFs.blockCountLong
        } else {
            availableBlocks = statFs.availableBlocks.toLong()
            totalBlocks = statFs.blockCount.toLong()
        }

        val availablePercentage = if (totalBlocks > 0) (availableBlocks.toFloat() / totalBlocks.toFloat()) * 100 else 0f
        return availablePercentage < thresholdPercentage
    }

    fun getTopUsedAppsFromEvents(limit: Int = 5): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        val beginTime = currentTime - TimeUnit.HOURS.toMillis(24) // Last 24 hours
        val regex = Regex("(?<=com\\.).*")

        val usageEvents = usageStatsManager.queryEvents(beginTime, currentTime)
        val packageManager = context.packageManager
        val appUsageMap = mutableMapOf<String, Long>() // PackageName to total foreground time

        val event = UsageEvents.Event()
        val activePackages = mutableSetOf<String>()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val packageName = event.packageName
            Log.d("DeviceHealth", "Event: ${event.eventType}, Package: $packageName")

            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                activePackages.add(packageName)
                // Start time for this app
                appUsageMap[packageName] = appUsageMap.getOrDefault(packageName, 0L) - event.timeStamp
            } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (activePackages.contains(packageName)) {
                    activePackages.remove(packageName)
                    // End time for this app
                    appUsageMap[packageName] = appUsageMap.getOrDefault(packageName, 0L) + event.timeStamp
                }
            }
        }

        // Close any apps still in foreground at the end time
        val endTime = currentTime
        for (packageName in activePackages) {
            // Assume the app was moved to background at the end time
            appUsageMap[packageName] = appUsageMap.getOrDefault(packageName, 0L) + endTime
        }


        // Filter out system apps and apps with zero usage time
        val appUsageInfoList = mutableListOf<AppUsageInfo>()

        for ((packageName, totalTime) in appUsageMap) {

            Log.d("DeviceHealth", "Package: $packageName, Total Time: $totalTime")
            try {
                val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
                // Exclude system apps
                if ((applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    continue
                }

                val appName = packageManager.getApplicationLabel(applicationInfo).toString()
                appUsageInfoList.add(AppUsageInfo(appName, abs(totalTime)))
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("DeviceHealth", "App not found: $packageName")
                val parts = packageName.split(".")
                appUsageInfoList.add(AppUsageInfo(parts.drop(1).joinToString("."), abs(totalTime)))


            }
        }

        // Sort the list by total time (descending)
        appUsageInfoList.sortByDescending { it.totalTimeInForeground }

        // Return the top N apps
        return appUsageInfoList.take(limit)
    }
}

data class AppUsageInfo(
    val appName: String,
    val totalTimeInForeground: Long // in milliseconds
)