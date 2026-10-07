package com.neon.gamebooster.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

class GameAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val currentPackageName = event.packageName?.toString() ?: return

            // Apne app ya System UI ke events ko ignore karein
            if (currentPackageName == packageName || currentPackageName.contains("systemui")) {
                return
            }

            val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
            val selectedApps = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

            if (selectedApps.isNotEmpty()) {
                if (selectedApps.contains(currentPackageName)) {
                    // Selected game Screen par active hai -> Start VPN
                    try {
                        val vpnIntent = Intent(this, GameVpnService::class.java)
                        startService(vpnIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Selected game band ho gaya ya background mein gaya -> Disconnect VPN
                    try {
                        val vpnIntent = Intent(this, GameVpnService::class.java)
                        stopService(vpnIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}
}
