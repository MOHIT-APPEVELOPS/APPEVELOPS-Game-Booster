package com.neon.gamebooster.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import java.io.OutputStream

object CpuBoosterManager {

    fun autoCleanAndBoostSystem(context: Context, gamePackageName: String) {
        // 1. Android Internal Garbage Collection & Memory Trim
        try {
            System.gc()
            Runtime.getRuntime().gc()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Kill Background Processes via ActivityManager (Non-Root Standard)
        try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (app in packages) {
                // Own app, system apps aur selected game ko close mat karo
                if (app.packageName != context.packageName &&
                    app.packageName != gamePackageName &&
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                    am.killBackgroundProcesses(app.packageName)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. High-Level Shizuku Commands (Aggressive RAM & Cache Cleanup if Shizuku is granted)
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

    private fun executeShizukuCommand(command: String) {
        try {
            val process = Shizuku.newProcess(arrayOf("sh"), null, null)
            val os: OutputStream = process.outputStream
            os.write("$command\nexit\n".toByteArray())
            os.flush()
            os.close()
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
