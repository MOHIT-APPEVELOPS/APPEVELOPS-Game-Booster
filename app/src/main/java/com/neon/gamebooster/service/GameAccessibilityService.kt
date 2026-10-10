package com.neon.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.neon.gamebooster.utils.CpuBoosterManager
import rikka.shizuku.Shizuku

class GameAccessibilityService : AccessibilityService() {

    private var activeGamePackage: String? = null
    private val defaultLauncherPackages = HashSet<String>()
    
    // Rapid event execution rokne ke liye throttle mechanism
    private var lastEventTimestamp = 0L
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        fetchLauncherPackages()
    }

    private fun fetchLauncherPackages() {
        try {
            defaultLauncherPackages.clear()
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (info in resolveInfos) {
                info.activityInfo?.packageName?.let {
                    defaultLauncherPackages.add(it)
                }
            }
            // Standard and OEM Launchers
            defaultLauncherPackages.add("com.android.launcher")
            defaultLauncherPackages.add("com.google.android.apps.nexuslauncher")
            defaultLauncherPackages.add("com.sec.android.app.launcher")
            defaultLauncherPackages.add("com.miui.home")
            defaultLauncherPackages.add("com.oppo.launcher")
            defaultLauncherPackages.add("com.coloros.launcher")
            defaultLauncherPackages.add("com.oneplus.launcher")
            defaultLauncherPackages.add("com.transsion.launcher")
            defaultLauncherPackages.add("com.realme.launcher")
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // System UI popups, quick settings aur keyboards par trigger drop karein
        if (packageName == "com.android.systemui" || 
            packageName.contains("inputmethod") || 
            packageName.contains("keyboard")) {
            return
        }

        // 300ms se kam time ke repeated duplicate triggers ko drop karein
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastEventTimestamp < 300) {
            return
        }
        lastEventTimestamp = currentTime

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val selectedGames = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

        // 1. Home Screen Detection -> Instant Cleanup
        if (defaultLauncherPackages.contains(packageName) || packageName.contains("launcher")) {
            if (activeGamePackage != null) {
                activeGamePackage = null
                onGameClosed(prefs)
            }
            return
        }

        // 2. Selected Game Focused
        if (selectedGames.contains(packageName)) {
            if (activeGamePackage != packageName) {
                activeGamePackage = packageName
                onGameOpened(prefs)
            }
        } else {
            // Kisi third-party app (WhatsApp, Browser etc.) par switch hone par pause/stop karein
            if (activeGamePackage != null) {
                activeGamePackage = null
                onGameClosed(prefs)
            }
        }
    }

    private fun onGameOpened(prefs: android.content.SharedPreferences) {
        try {
            prefs.edit().putBoolean("is_game_actively_running", true).apply()

            // 1. DND Optimization
            applyDndMode(true, prefs)

            // 2. Auto Memory Cleaner (Safe launch)
            if (prefs.getBoolean("enable_bg_killer", false) || prefs.getBoolean("enable_cache_clear", false)) {
                val cleanerIntent = Intent(this, AppAutoCleanerService::class.java).apply {
                    action = AppAutoCleanerService.ACTION_START
                }
                safeStartForegroundService(cleanerIntent)
            }

            // 3. Network VPN Ping Isolation
            if (prefs.getBoolean("enable_vpn", false)) {
                val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                    action = GameVpnService.ACTION_START_VPN
                }
                safeStartForegroundService(vpnIntent)
            }

            // 4. Slim Floating Crosshair
            if (prefs.getBoolean("enable_crosshair", false)) {
                val crosshairIntent = Intent(this, CrosshairService::class.java).apply {
                    action = CrosshairService.ACTION_SHOW
                }
                safeStartForegroundService(crosshairIntent)
            }

            // 5. Shizuku Isolation (Only if connected & granted)
            safeExecuteShizukuTweaks(prefs, isEnabling = true)

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun onGameClosed(prefs: android.content.SharedPreferences) {
        try {
            prefs.edit().putBoolean("is_game_actively_running", false).apply()

            // 1. Restore Normal Audio/Notifications
            applyDndMode(false, prefs)

            // 2. Stop Crosshair Immediately
            safeStopCustomService(CrosshairService::class.java, CrosshairService.ACTION_HIDE)

            // 3. Stop VPN
            safeStopCustomService(GameVpnService::class.java, GameVpnService.ACTION_STOP_VPN)

            // 4. Stop Auto Cleaner
            safeStopCustomService(AppAutoCleanerService::class.java, AppAutoCleanerService.ACTION_STOP)

            // 5. Restore Animations via Shizuku if active
            safeExecuteShizukuTweaks(prefs, isEnabling = false)

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Android 7 se lekar 14/15/16 tak har platform ke liye fail-safe foreground start
     */
    private fun safeStartForegroundService(serviceIntent: Intent) {
        mainHandler.post {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(this, serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } catch (e: Throwable) {
                // Background start restriction bypass fallback
                try {
                    startService(serviceIntent)
                } catch (ignored: Throwable) {}
            }
        }
    }

    /**
     * Services ko safely stop aur clean karne ke liye wrapper
     */
    private fun safeStopCustomService(serviceClass: Class<*>, stopAction: String) {
        try {
            val stopIntent = Intent(this, serviceClass).apply {
                action = stopAction
            }
            startService(stopIntent)
            stopService(Intent(this, serviceClass))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    /**
     * Shizuku disconnection par bina crash hue safe execution
     */
    private fun safeExecuteShizukuTweaks(prefs: android.content.SharedPreferences, isEnabling: Boolean) {
        try {
            // Ping binder safe check
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                    if (isEnabling) {
                        CpuBoosterManager.applyPerformanceMode()
                        CpuBoosterManager.setWindowAnimationScale(prefs.getBoolean("enable_window_anim", false))
                        CpuBoosterManager.setTransitionAnimationScale(prefs.getBoolean("enable_transition_anim", false))
                        CpuBoosterManager.setAnimatorDurationScale(prefs.getBoolean("enable_animator_anim", false))
                    } else {
                        CpuBoosterManager.resetAnimationsToNormal()
                    }
                }
            }
        } catch (e: Throwable) {
            // Shizuku band hone par silently ignore karega, crash nahi aane dega
        }
    }

    private fun applyDndMode(enable: Boolean, prefs: android.content.SharedPreferences) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
                if (enable) {
                    val isMasterDndOn = prefs.getBoolean("dnd_master_enabled", false)
                    val isCallDndOn = prefs.getBoolean("dnd_block_calls", false)
                    val isMsgDndOn = prefs.getBoolean("dnd_block_notifs", false)

                    if (isMasterDndOn && (isCallDndOn || isMsgDndOn)) {
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    }
                } else {
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        onGameClosed(prefs)
    }

    override fun onDestroy() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        onGameClosed(prefs)
        super.onDestroy()
    }
}
