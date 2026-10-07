package com.neon.gamebooster.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object CpuBoosterManager {

    // 1. CPU Performance Mode Apply karne ke liye (Shizuku)
    fun applyPerformanceMode() {
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            try {
                // Fixed performance mode enable karna
                executeShizukuCommand("cmd power set-fixed-performance-mode-enabled true")
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

        // Shizuku Level Deep Storage & Memory Cleanup
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            try {
                executeShizukuCommand("pm trim-caches 1000G")
                executeShizukuCommand("am kill-all")
                executeShizukuCommand("cmd power set-fixed-performance-mode-enabled true")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Fixed Helper method to execute Shizuku shell commands
    private fun executeShizukuCommand(command: String) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
