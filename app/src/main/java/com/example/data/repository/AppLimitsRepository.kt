package com.example.data.repository

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.example.data.local.AppLimitsDao
import com.example.data.model.AppLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class AppLimitsRepository(
    private val context: Context,
    private val appLimitsDao: AppLimitsDao
) {
    val allLimits: Flow<List<AppLimit>> = appLimitsDao.getAllLimits()

    fun isUsagePermissionGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    suspend fun refreshUsageStats() = withContext(Dispatchers.IO) {
        if (!isUsagePermissionGranted()) return@withContext

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return@withContext

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        val now = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startOfDay,
            now
        ) ?: return@withContext

        for (usage in stats) {
            val totalMinutes = (usage.totalTimeInForeground / (1000 * 60)).toInt()
            if (totalMinutes > 0) {
                val existing = appLimitsDao.getLimitByPackage(usage.packageName)
                if (existing != null) {
                    appLimitsDao.updateUsedMinutes(usage.packageName, totalMinutes)
                }
            }
        }
    }

    suspend fun getInstalledApps(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            packages
                .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || isKnownPopularApp(it.packageName) }
                .map { appInfo ->
                    val name = pm.getApplicationLabel(appInfo).toString()
                    Pair(appInfo.packageName, name)
                }
                .sortedBy { it.second.lowercase() }
        } catch (_: Exception) {
            listOf(
                Pair("com.instagram.android", "Instagram"),
                Pair("com.google.android.youtube", "YouTube"),
                Pair("com.reddit.frontpage", "Reddit"),
                Pair("com.zhiliaoapp.musically", "TikTok"),
                Pair("com.twitter.android", "X (Twitter)"),
                Pair("com.snapchat.android", "Snapchat"),
                Pair("com.discord", "Discord")
            )
        }
    }

    private fun isKnownPopularApp(pkg: String): Boolean {
        return pkg.contains("youtube") || pkg.contains("chrome") || pkg.contains("instagram")
    }

    suspend fun saveLimit(limit: AppLimit) = withContext(Dispatchers.IO) {
        appLimitsDao.insertOrUpdateLimit(limit)
    }

    suspend fun deleteLimit(limit: AppLimit) = withContext(Dispatchers.IO) {
        appLimitsDao.deleteLimit(limit)
    }

    suspend fun simulateUsageIncrement(packageName: String, additionalMinutes: Int) = withContext(Dispatchers.IO) {
        val current = appLimitsDao.getLimitByPackage(packageName)
        if (current != null) {
            val updated = current.usedMinutesToday + additionalMinutes
            appLimitsDao.updateUsedMinutes(packageName, updated)
        }
    }

    suspend fun resetTodayUsage(packageName: String) = withContext(Dispatchers.IO) {
        appLimitsDao.updateUsedMinutes(packageName, 0)
    }
}
