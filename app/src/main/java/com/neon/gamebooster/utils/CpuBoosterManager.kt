package com.neon.gamebooster.utils

import android.app.ActivityManager
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

    // Individual Animation Scale Control Methods (Boolean compatibility)
    fun setWindowAnimationScale(enable: Boolean) {
        val value = if (enable) "0.1" else "1.0"
        executeCommand("settings put global window_animation_scale $value")
    }

    fun setTransitionAnimationScale(enable: Boolean) {
        val value = if (enable) "0.1" else "1.0"
        executeCommand("settings put global transition_animation_scale $value")
    }

    fun setAnimatorDurationScale(enable: Boolean) {
        val value = if (enable) "0.1" else "1.0"
        executeCommand("settings put global animator_duration_scale $value")
    }

    // Dynamic String Animation Scale Control (For Spinner / Custom Value Settings like 0.1x, 0.2x, etc.)
    fun setWindowAnimationScale(scale: String) {
        executeCommand("settings put global window_animation_scale $scale")
    }

    fun setTransitionAnimationScale(scale: String) {
        executeCommand("settings put global transition_animation_scale $scale")
    }

    fun setAnimatorDurationScale(scale: String) {
        executeCommand("settings put global animator_duration_scale $scale")
    }

    // Reset all animations back to normal (1.0x) when exiting game
    fun resetAnimationsToNormal() {
        val commands = arrayOf(
            "settings put global window_animation_scale 1.0",
            "settings put global transition_animation_scale 1.0",
            "settings put global animator_duration_scale 1.0"
        )
        if (isShizukuAvailableAndGranted()) {
            try {
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true
                val process = newProcessMethod.invoke(
                    null,
                    arrayOf("sh", "-c", commands.joinToString(" && ")),
                    null,
                    null
                ) as Process
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Command Dispatcher (Shizuku + Shell Fallback)
    private fun executeCommand(command: String) {
        if (isShizukuAvailableAndGranted()) {
            executeShizukuCommand(command)
        } else {
            executeShellCommand(command)
        }
    }

    // 2. CPU Performance Mode Execution
    fun applyPerformanceMode() {
        executeCommand("cmd power set-fixed-performance-mode-enabled true")
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
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in packages) {
                if (app.packageName != context.packageName &&
                    app.packageName != gamePackageName &&
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    
                    am.killBackgroundProcesses(app.packageName)
                    executeCommand("am force-stop ${app.packageName}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Step C: Cache & Performance
        executeCommand("pm trim-caches 1000G")
        executeCommand("am kill-all")
        executeCommand("cmd power set-fixed-performance-mode-enabled true")
    }

    // 4. Device Hardware & Performance Profiling Helper (Zero Permissions Required)
    fun getDevicePerformanceInfo(context: Context): String {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val totalRamGB = memoryInfo.totalMem / (1024 * 1024 * 1024.toLong())
            val availRamGB = memoryInfo.availMem / (1024 * 1024 * 1024.toLong())
            val cpuCores = Runtime.getRuntime().availableProcessors()

            return "RAM: ${availRamGB}GB / ${totalRamGB}GB | Cores: $cpuCores"
        } catch (e: Exception) {
            return "Performance Info Unavailable"
        }
    }

    // Safe Shizuku Process Execution via Reflection
    private fun executeShizukuCommand(command: String) {
        try {
            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as Process
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
