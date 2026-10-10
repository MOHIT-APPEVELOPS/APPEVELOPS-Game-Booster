package com.neon.gamebooster.ui

import android.content.Context
import android.os.Bundle
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

        val switchBlockCalls = findViewById<Switch>(R.id.switchBlockCalls)
        val switchBlockNotifications = findViewById<Switch>(R.id.switchBlockNotifications)
        val switchAutoReject = findViewById<Switch>(R.id.switchAutoReject)

        // Load saved states
        switchBlockCalls?.isChecked = prefs.getBoolean("dnd_block_calls", false)
        switchBlockNotifications?.isChecked = prefs.getBoolean("dnd_block_notifs", false)
        switchAutoReject?.isChecked = prefs.getBoolean("dnd_auto_reject", false)

        // Save states on change
        switchBlockCalls?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_block_calls", isChecked).apply()
            Toast.makeText(this, "Block Calls: $isChecked", Toast.LENGTH_SHORT).show()
        }

        switchBlockNotifications?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_block_notifs", isChecked).apply()
            Toast.makeText(this, "Block Notifications: $isChecked", Toast.LENGTH_SHORT).show()
        }

        switchAutoReject?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_auto_reject", isChecked).apply()
            Toast.makeText(this, "Auto Reject: $isChecked", Toast.LENGTH_SHORT).show()
        }
    }
}
