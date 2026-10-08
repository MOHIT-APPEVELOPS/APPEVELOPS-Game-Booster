package com.neon.gamebooster.ui

import android.content.Context
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.neon.gamebooster.R

class DndSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dnd_settings)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        val switchBlockCalls = findViewById<Switch>(R.id.switchBlockCalls)
        val switchBlockNotifications = findViewById<Switch>(R.id.switchBlockNotifications)
        val switchAutoReject = findViewById<Switch>(R.id.switchAutoReject)

        switchBlockCalls?.isChecked = prefs.getBoolean("dnd_block_calls", false)
        switchBlockNotifications?.isChecked = prefs.getBoolean("dnd_block_notifs", false)
        switchAutoReject?.isChecked = prefs.getBoolean("dnd_auto_reject", false)

        switchBlockCalls?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_block_calls", isChecked).apply()
        }

        switchBlockNotifications?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_block_notifs", isChecked).apply()
        }

        switchAutoReject?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_auto_reject", isChecked).apply()
        }
    }
}
