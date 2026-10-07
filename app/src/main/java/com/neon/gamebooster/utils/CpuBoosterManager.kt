package com.neon.gamebooster.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object CpuBoosterManager {

    // 1. CPU Performance Mode Apply karne ke liye
    fun applyPerformanceMode() {
        try {
            executeShellCommand("cmd power set-fixed-performance-mode-enabled true")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. Auto RAM, Cache Cleanup & Background Apps Killer
    fun autoCleanAndBoostSystem(context: Context, gamePackageName: String) {
        // System Garbage Collection
        try {
            System.gc()
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Background Apps Close karna (Except system apps, own app, and target game)
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in packages) {
                if (app.packageName != context.packageName &&
                    app.packageName != gamePackageName &&
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    am.killBackgroundProcesses(app.packageName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Safe Shell Level Deep Storage & Memory Cleanup
        try {
            executeShellCommand("pm trim-caches 1000G")
            executeShellCommand("cmd power set-fixed-performance-mode-enabled true")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Standard Shell Execution (Fixes private 'Shizuku.newProcess' error)
    private fun executeShellCommand(command: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
