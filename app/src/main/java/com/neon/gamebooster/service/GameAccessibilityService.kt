package com.neon.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat

class GameAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val currentPackageName = event.packageName?.toString() ?: return

            // System UI aur apne app ke events ko ignore karein
            if (currentPackageName == packageName || currentPackageName.contains("systemui")) {
                return
            }

            val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
            val selectedApps = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

            if (selectedApps.isNotEmpty()) {
                if (selectedApps.contains(currentPackageName)) {
                    // Selected game active hai -> Start VPN & Crosshair
                    try {
                        val vpnIntent = Intent(this, GameVpnService::class.java)
                        startService(vpnIntent)

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
                            val crosshairIntent = Intent(this, CrosshairService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                ContextCompat.startForegroundService(this, crosshairIntent)
                            } else {
                                startService(crosshairIntent)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Selected game band hua -> Explicitly stop VPN using ACTION_STOP_VPN & Crosshair
                    try {
                        val stopVpnIntent = Intent(this, GameVpnService::class.java).apply {
                            action = GameVpnService.ACTION_STOP_VPN
                        }
                        startService(stopVpnIntent)

                        stopService(Intent(this, CrosshairService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}
}
