package com.neon.gamebooster.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neon.gamebooster.R

class DndSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dnd_settings)

        // Top-left Back Arrow Button click listener
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        // XML ke 4 switches (Master Switch + 3 Sub-options)
        val switchDndMaster = findViewById<Switch>(R.id.switchDndMaster)
        val switchBlockCalls = findViewById<Switch>(R.id.switchBlockCalls)
        val switchBlockNotifications = findViewById<Switch>(R.id.switchBlockNotifications)
        val switchAutoReject = findViewById<Switch>(R.id.switchAutoReject)

        // 1. Load saved states
        switchDndMaster?.isChecked = prefs.getBoolean("dnd_master_enabled", true)
        switchBlockCalls?.isChecked = prefs.getBoolean("dnd_block_calls", true)
        switchBlockNotifications?.isChecked = prefs.getBoolean("dnd_block_notifs", true)
        switchAutoReject?.isChecked = prefs.getBoolean("dnd_auto_reject", false)

        // 2. Master Switch (DND Services)
        switchDndMaster?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_master_enabled", isChecked).apply()
            if (isChecked) {
                checkAndPromptDndPermissions()
            }
            Toast.makeText(this, "DND Service: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        // 3. Call DND Switch
        switchBlockCalls?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_block_calls", isChecked).apply()
            if (isChecked) {
                checkAndPromptDndPermissions()
            }
            Toast.makeText(this, "Call DND: $isChecked", Toast.LENGTH_SHORT).show()
        }

        // 4. Message Silent Switch
        switchBlockNotifications?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_block_notifs", isChecked).apply()
            if (isChecked) {
                checkAndPromptNotificationListener()
            }
            Toast.makeText(this, "Message Silent: $isChecked", Toast.LENGTH_SHORT).show()
        }

        // 5. Floating Line / Call Alert
        switchAutoReject?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_auto_reject", isChecked).apply()
            Toast.makeText(this, "Call Alert: $isChecked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndPromptDndPermissions() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !nm.isNotificationPolicyAccessGranted) {
            Toast.makeText(this, "Allow 'Do Not Disturb' Access for Call Blocking", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkAndPromptNotificationListener() {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        if (!enabledListeners.contains(packageName)) {
            Toast.makeText(this, "Allow Notification Access to Silent Messages", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
