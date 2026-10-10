package com.neon.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.neon.gamebooster.utils.CpuBoosterManager

class GameAccessibilityService : AccessibilityService() {

    private var activeGamePackage: String? = null
    private val defaultLauncherPackages = HashSet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        fetchLauncherPackages()
    }

    private fun fetchLauncherPackages() {
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
        // Common OEM Launchers Fallback
        defaultLauncherPackages.add("com.android.launcher")
        defaultLauncherPackages.add("com.google.android.apps.nexuslauncher")
        defaultLauncherPackages.add("com.sec.android.app.launcher")
        defaultLauncherPackages.add("com.miui.home")
        defaultLauncherPackages.add("com.oppo.launcher")
        defaultLauncherPackages.add("com.coloros.launcher")
        defaultLauncherPackages.add("com.oneplus.launcher")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // 1. In-game transient overlays (volume, notification shade, input method) par ignore karein taaki flicker na ho
        if (packageName == "com.android.systemui" || packageName.contains("inputmethod")) {
            return
        }

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val selectedGames = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

        // 2. Agar user direct Home Screen par chala gaya -> Instant Hide & Stop
        if (defaultLauncherPackages.contains(packageName) || packageName.contains("launcher")) {
            if (activeGamePackage != null) {
                activeGamePackage = null
                onGameClosed(prefs)
            }
            return
        }

        // 3. Target game focused hai -> Instant Show
        if (selectedGames.contains(packageName)) {
            if (activeGamePackage != packageName) {
                activeGamePackage = packageName
                onGameOpened(prefs)
            } else {
                // Game ke andar hi hai, crosshair state confirm rakhein bina restart kiye
                if (prefs.getBoolean("enable_crosshair", true)) {
                    val showIntent = Intent(this, CrosshairService::class.java).apply {
                        action = CrosshairService.ACTION_SHOW
                    }
                    startService(showIntent)
                }
            }
        } else {
            // Kisi doosri non-game app (WhatsApp, Chrome etc.) par switch kiya -> Instant Exit
            if (activeGamePackage != null) {
                activeGamePackage = null
                onGameClosed(prefs)
            }
        }
    }

    private fun onGameOpened(prefs: android.content.SharedPreferences) {
        // Active Game Flag for Notification Blocker
        prefs.edit().putBoolean("is_game_actively_running", true).apply()

        // 1. Apply DND System Mode
        applyDndMode(true, prefs)

        // 2. Silent Auto Clean
        if (prefs.getBoolean("enable_bg_killer", true) || prefs.getBoolean("enable_cache_clear", true)) {
            startService(Intent(this, AppAutoCleanerService::class.java))
        }

        // 3. Start VPN
        if (prefs.getBoolean("enable_vpn", true)) {
            val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                action = GameVpnService.ACTION_START_VPN
            }
            startService(vpnIntent)
        }

        // 4. Instant Crosshair Show
        if (prefs.getBoolean("enable_crosshair", true)) {
            val crosshairIntent = Intent(this, CrosshairService::class.java).apply {
                action = CrosshairService.ACTION_SHOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(crosshairIntent)
            } else {
                startService(crosshairIntent)
            }
        }

        // 5. Shizuku Tweaks
        try {
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                CpuBoosterManager.applyPerformanceMode()
                CpuBoosterManager.setWindowAnimationScale(prefs.getBoolean("enable_window_anim", true))
                CpuBoosterManager.setTransitionAnimationScale(prefs.getBoolean("enable_transition_anim", true))
                CpuBoosterManager.setAnimatorDurationScale(prefs.getBoolean("enable_animator_anim", true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun onGameClosed(prefs: android.content.SharedPreferences) {
        // Active Game Flag Off
        prefs.edit().putBoolean("is_game_actively_running", false).apply()

        // 1. Restore Normal Sound & Notifications (Turn OFF DND)
        applyDndMode(false, prefs)

        // 2. Instant Hide & Stop Crosshair
        val stopCrosshairIntent = Intent(this, CrosshairService::class.java).apply {
            action = CrosshairService.ACTION_HIDE
        }
        startService(stopCrosshairIntent)
        stopService(Intent(this, CrosshairService::class.java))

        // 3. Stop VPN
        val stopVpnIntent = Intent(this, GameVpnService::class.java).apply {
            action = GameVpnService.ACTION_STOP_VPN
        }
        startService(stopVpnIntent)

        // 4. Restore Animations
        try {
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                CpuBoosterManager.resetAnimationsToNormal()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyDndMode(enable: Boolean, prefs: android.content.SharedPreferences) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && nm.isNotificationPolicyAccessGranted) {
                if (enable) {
                    val isMasterDndOn = prefs.getBoolean("dnd_master_enabled", true)
                    val isCallDndOn = prefs.getBoolean("dnd_block_calls", true)
                    val isMsgDndOn = prefs.getBoolean("dnd_block_notifs", true)

                    if (isMasterDndOn && (isCallDndOn || isMsgDndOn)) {
                        // Complete Silence: No Calls, No Popups, No Alarms during match
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    }
                } else {
                    // Turn OFF DND, allow all calls and notifications normally
                    nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        onGameClosed(prefs)
    }
}
