package com.neon.gamebooster.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.neon.gamebooster.utils.CpuBoosterManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppAutoCleanerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var notificationManager: NotificationManager? = null

    companion object {
        const val ACTION_START = "com.neon.gamebooster.ACTION_START_CLEANER"
        const val ACTION_STOP = "com.neon.gamebooster.ACTION_STOP_CLEANER"
        private const val NOTIFICATION_ID = 103
        private const val CHANNEL_ID = "cleaner_service_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        startForegroundNotification("Monitoring Background Memory...")
        startAutoCleaningLoop()
    }

    private fun startForegroundNotification(statusText: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Cleaner Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Game memory optimization & cache trimming"
            }
            notificationManager?.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Neon Memory Cleaner")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Android 14+ (API 34) aur Android 15 compatibility fix
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(cleanedMb: Long) {
        val message = if (cleanedMb > 0) {
            "Freed $cleanedMb MB Memory & Cache"
        } else {
            "RAM & Cache Optimized for Smooth Gaming"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Neon Memory Cleaner")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager?.notify(NOTIFICATION_ID, notification)
    }

    private fun startAutoCleaningLoop() {
        serviceScope.launch {
            while (isActive) {
                val freedMem = performCleaning()
                if (freedMem > 0) {
                    updateNotification(freedMem)
                }
                // Har 45 seconds me background apps clean karega
                delay(45000)
            }
        }
    }

    private fun performCleaning(): Long {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memBefore = getAvailableMemoryMb(am)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val isBgKillerEnabled = prefs.getBoolean("enable_bg_killer", true)
        val selectedGames = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

        if (isBgKillerEnabled) {
            // 1. Shizuku Privileged Cache Trim (Safe execution through CpuBoosterManager)
            try {
                if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                    CpuBoosterManager.executeShizukuCommand("pm trim-caches 4096M")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Non-system apps kill (Game aur apna app safe rakhte hue)
            try {
                val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                for (app in packages) {
                    if (app.packageName != packageName && 
                        !selectedGames.contains(app.packageName) && 
                        (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0) {
                        try {
                            am.killBackgroundProcesses(app.packageName)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // System GC trigger
            System.gc()
            Runtime.getRuntime().gc()
        }

        val memAfter = getAvailableMemoryMb(am)
        val freed = memAfter - memBefore
        return if (freed > 0) freed else 0
    }

    private fun getAvailableMemoryMb(activityManager: ActivityManager): Long {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
