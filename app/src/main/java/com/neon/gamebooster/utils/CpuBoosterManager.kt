package com.neon.gamebooster.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object CpuBoosterManager {

    // 1. Shizuku Status and Permission Check
    fun isShizukuAvailableAndGranted(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    // 2. CPU Performance Mode Execution
    fun applyPerformanceMode() {
        if (isShizukuAvailableAndGranted()) {
            executeShizukuCommand("cmd power set-fixed-performance-mode-enabled true")
        } else {
            executeShellCommand("cmd power set-fixed-performance-mode-enabled true")
        }
    }

    // 3. Silent Auto Clean & Boost System
    fun autoCleanAndBoostSystem(context: Context, gamePackageName: String) {
        // Step A: Memory Garbage Collection
        try {
            System.gc()
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step B: Kill Background Apps
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in packages) {
                if (app.packageName != context.packageName &&
                    app.packageName != gamePackageName &&
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    
                    am.killBackgroundProcesses(app.packageName)
                    
                    if (isShizukuAvailableAndGranted()) {
                        executeShizukuCommand("am force-stop ${app.packageName}")
                    } else {
                        executeShellCommand("am force-stop ${app.packageName}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step C: Storage & Cache Optimization
        if (isShizukuAvailableAndGranted()) {
            executeShizukuCommand("pm trim-caches 1000G")
            executeShizukuCommand("am kill-all")
            executeShizukuCommand("cmd power set-fixed-performance-mode-enabled true")
        } else {
            executeShellCommand("pm trim-caches 1000G")
        }
    }

    // Official Shizuku Process Command Execution
    private fun executeShizukuCommand(command: String) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            process.waitFor()
        } catch (e: Exception) {
            executeShellCommand(command)
        }
    }

    // Standard Fallback Shell Command
    private fun executeShellCommand(command: String) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
