package com.neon.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.neon.gamebooster.utils.CpuBoosterManager

class GameAccessibilityService : AccessibilityService() {

    private var activeGamePackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

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
        // 1. Start VPN if enabled in prefs
        if (prefs.getBoolean("enable_vpn", true)) {
            val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                action = GameVpnService.ACTION_START_VPN
            }
            startService(vpnIntent)
        }

        // 2. Start Crosshair if enabled in prefs
        if (prefs.getBoolean("enable_crosshair", true)) {
            startService(Intent(this, CrosshairService::class.java))
        }

        // 3. Apply Shizuku Animation Scales (0.1x) & Performance Mode
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
                // Reset scales to normal (1.0x) when game exits
                CpuBoosterManager.resetAnimationsToNormal()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        // Cleanup if service interrupted
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        onGameClosed(prefs)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}
