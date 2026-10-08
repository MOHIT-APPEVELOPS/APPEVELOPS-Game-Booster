package com.neon.gamebooster.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.neon.gamebooster.R

class DndSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dnd_settings)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        val switchDndService = findViewById<SwitchCompat>(R.id.switchDndService)
        val switchMsgSilent = findViewById<SwitchCompat>(R.id.switchMsgSilent)
        val switchCallDnd = findViewById<SwitchCompat>(R.id.switchCallDnd)
        val switchFloatingLine = findViewById<SwitchCompat>(R.id.switchFloatingLine)

        switchDndService.isChecked = prefs.getBoolean("dnd_service_on", true)
        switchMsgSilent.isChecked = prefs.getBoolean("msg_silent_on", true)
        switchCallDnd.isChecked = prefs.getBoolean("call_dnd_on", true)
        switchFloatingLine.isChecked = prefs.getBoolean("floating_line_on", true)

        switchDndService.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dnd_service_on", isChecked).apply()
        }
        switchMsgSilent.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("msg_silent_on", isChecked).apply()
        }
        switchCallDnd.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("call_dnd_on", isChecked).apply()
        }
        switchFloatingLine.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("floating_line_on", isChecked).apply()
        }
    }
}
