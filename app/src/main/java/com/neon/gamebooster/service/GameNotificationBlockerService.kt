package com.neon.gamebooster.service

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class GameNotificationBlockerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val isGameRunning = prefs.getBoolean("is_game_actively_running", false)
        val isMasterDndOn = prefs.getBoolean("dnd_master_enabled", true)
        val isMsgDndOn = prefs.getBoolean("dnd_block_notifs", true)

        // Jab game chal raha ho aur Message Silent on ho -> incoming popups dismiss karein
        if (isGameRunning && isMasterDndOn && isMsgDndOn) {
            val pkg = sbn.packageName
            val selectedGames = prefs.getStringSet("selected_apps", emptySet()) ?: emptySet()

            // Game aur booster ko chhodkar baki third-party apps ke heads-up notification cancel honge
            if (!selectedGames.contains(pkg) && pkg != packageName) {
                try {
                    cancelNotification(sbn.key)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
