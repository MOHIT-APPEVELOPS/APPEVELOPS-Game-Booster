package com.neon.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.neon.gamebooster.utils.CpuBoosterManager

class GameAccessibilityService : AccessibilityService() {

    private var activeGamePackage: String? = null
    private val handler = Handler(Looper.getMainLooper())
    private var closeGameRunnable: Runnable? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // System overlays, keyboards aur notifications shade par flicker hone se bachayein
        if (packageName == "com.android.systemui" || 
            packageName.contains("inputmethod") || 
            packageName.contains("launcher")) {
            return
        }

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val selectedGames = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

        if (selectedGames.contains(packageName)) {
            // Agar pehle se koi close timer chal raha tha to cancel karein (User wapas game me aaya)
            closeGameRunnable?.let { handler.removeCallbacks(it) }
            closeGameRunnable = null

            if (activeGamePackage != packageName) {
                activeGamePackage = packageName
                onGameOpened(prefs)
            } else {
                // Game dubara foreground mein aaya: Crosshair show karein
                if (prefs.getBoolean("enable_crosshair", true)) {
                    val showCrosshairIntent = Intent(this, CrosshairService::class.java).apply {
                        action = CrosshairService.ACTION_SHOW
                    }
                    startService(showCrosshairIntent)
                }
            }
        } else {
            // User game se bahar aaya - 1 second ka delay (debounce) taaki quick switch mein crosshair crash na ho
            if (activeGamePackage != null && closeGameRunnable == null) {
                closeGameRunnable = Runnable {
                    onGameClosed(prefs)
                    activeGamePackage = null
                    closeGameRunnable = null
                }
                handler.postDelayed(closeGameRunnable!!, 1000)
            }
        }
    }

    private fun onGameOpened(prefs: android.content.SharedPreferences) {
        // 1. Silent Auto Clean
        if (prefs.getBoolean("enable_bg_killer", true) || prefs.getBoolean("enable_cache_clear", true)) {
            startService(Intent(this, AppAutoCleanerService::class.java))
        }

        // 2. Start VPN
        if (prefs.getBoolean("enable_vpn", true)) {
            val vpnIntent = Intent(this, GameVpnService::class.java).apply {
                action = GameVpnService.ACTION_START_VPN
            }
            startService(vpnIntent)
        }

        // 3. Start Crosshair (Safe call)
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

        // 4. Apply Shizuku Tweaks
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

        // Hide/Stop Crosshair
        val stopCrosshairIntent = Intent(this, CrosshairService::class.java).apply {
            action = CrosshairService.ACTION_STOP
        }
        startService(stopCrosshairIntent)

        // Restore Animations
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
}
