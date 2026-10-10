package com.neon.gamebooster.service

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
        val isCacheClearEnabled = prefs.getBoolean("enable_cache_clear", true)

        Thread {
            try {
                if (isBgKillerEnabled) {
                    killNonSystemApps()
                }
                if (isCacheClearEnabled && CpuBoosterManager.isShizukuAvailableAndGranted()) {
                    CpuBoosterManager.executeShizukuCommand("pm trim-caches 1000G")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            stopSelf()
        }.start()

        return START_NOT_STICKY
    }

    private fun killNonSystemApps() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in packages) {
            if (app.packageName != packageName && (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                    CpuBoosterManager.killBackgroundApp(app.packageName)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
