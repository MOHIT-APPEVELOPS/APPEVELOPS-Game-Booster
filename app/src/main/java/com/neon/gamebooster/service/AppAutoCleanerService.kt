package com.neon.gamebooster.service

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import com.neon.gamebooster.utils.CpuBoosterManager

class AppAutoCleanerService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val isBgKillerEnabled = prefs.getBoolean("enable_bg_killer", true)

        Thread {
            try {
                if (isBgKillerEnabled) {
                    killNonSystemApps()
                }
                // Memory GC
                System.gc()
                Runtime.getRuntime().gc()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            stopSelf()
        }.start()

        return START_NOT_STICKY
    }

    private fun killNonSystemApps() {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            if (app.packageName != packageName && (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                try {
                    am.killBackgroundProcesses(app.packageName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
