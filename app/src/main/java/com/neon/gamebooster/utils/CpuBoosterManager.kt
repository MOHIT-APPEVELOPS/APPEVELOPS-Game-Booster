package com.neon.gamebooster.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object CpuBoosterManager {

    // 1. Shizuku Permission Check Helper
    fun isShizukuAvailableAndGranted(): Boolean {
        return try {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            false
        }
    }

    // 2. CPU Performance Mode Apply karne ke liye
    fun applyPerformanceMode() {
        if (isShizukuAvailableAndGranted()) {
            executeShizukuCommand("cmd power set-fixed-performance-mode-enabled true")
        }
    }

    // 3. Shizuku Level Silent Auto Clean & Boost System
    fun autoCleanAndBoostSystem(context: Context, gamePackageName: String) {
        // Step A: System Garbage Collection
        try {
            System.gc()
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step B: Kill Non-Essential Background Apps
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in packages) {
                if (app.packageName != context.packageName &&
                    app.packageName != gamePackageName &&
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    
                    // Normal Framework Kill
                    am.killBackgroundProcesses(app.packageName)
                    
                    // Shizuku Elevated Force Stop Command
                    if (isShizukuAvailableAndGranted()) {
                        executeShizukuCommand("am force-stop ${app.packageName}")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step C: Deep Cache Trim & High Performance Governor
        if (isShizukuAvailableAndGranted()) {
            executeShizukuCommand("pm trim-caches 1000G")
            executeShizukuCommand("am kill-all")
            executeShizukuCommand("cmd power set-fixed-performance-mode-enabled true")
        }
    }

    // Shizuku Command Execution Engine
    private fun executeShizukuCommand(command: String) {
        try {
            val execMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            execMethod.isAccessible = true
            val process = execMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process
            process.waitFor()
        } catch (e: Exception) {
            // Fallback to standard runtime shell if reflection is restricted
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                process.waitFor()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}
