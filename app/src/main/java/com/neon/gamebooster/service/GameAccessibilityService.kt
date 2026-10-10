package com.neon.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.neon.gamebooster.utils.CpuBoosterManager

class GameAccessibilityService : AccessibilityService() {

    private var activeGamePackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Ignore system UI or launcher to prevent flickering
            if (packageName == "com.android.systemui" || packageName.contains("launcher")) {
                return
            }

            val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
            val selectedGames = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

            // Check if current focused app is one of the booster games
            if (selectedGames.contains(packageName)) {
                if (activeGamePackage != packageName) {
                    activeGamePackage = packageName
                    onGameOpened(prefs)
                }
            } else {
                // Agar user game se bahar aa gaya hai
                if (activeGamePackage != null) {
                    onGameClosed(prefs)
                    activeGamePackage = null
                }
            }
        }
    }

    private fun onGameOpened(prefs: android.content.SharedPreferences) {
        // 1. Silent Auto Clean & Boost trigger
        val isBgKillerEnabled = prefs.getBoolean("enable_bg_killer", true)
        val isCacheClearEnabled = prefs.getBoolean("enable_cache_clear", true)
        if (isBgKillerEnabled || isCacheClearEnabled) {
            val cleanIntent = Intent(this, AppAutoCleanerService::class.java)
            startService(cleanIntent)
        }

        // 2. Start VPN if enabled in prefs
        if (prefs.getBoolean("enable_vpn", true)) {
            val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                action = GameVpnService.ACTION_START_VPN
            }
            startService(vpnIntent)
        }

        // 3. Start Crosshair if enabled in prefs
        if (prefs.getBoolean("enable_crosshair", true)) {
            val crosshairIntent = Intent(this, CrosshairService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(crosshairIntent)
            } else {
                startService(crosshairIntent)
            }
        }

        // 4. Apply Shizuku Animation Scales (0.1x) & Performance Mode
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
        // Stop VPN
        val stopVpnIntent = Intent(this, GameVpnService::class.java).apply {
            action = GameVpnService.ACTION_STOP_VPN
        }
        startService(stopVpnIntent)

        // Stop Crosshair
        stopService(Intent(this, CrosshairService::class.java))

        // Restore Normal Phone Animations (Scale 1.0)
        try {
            if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                CpuBoosterManager.resetAnimationsToNormal()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        onGameClosed(prefs)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}
