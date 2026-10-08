package com.neon.gamebooster.ui

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.neon.gamebooster.R
import com.neon.gamebooster.utils.CpuBoosterManager

class AnimationSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animation_settings)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        val switchAnimMaster = findViewById<SwitchCompat>(R.id.switchAnimMaster)
        val spinnerWindow = findViewById<Spinner>(R.id.spinnerWindowAnim)
        val spinnerTransition = findViewById<Spinner>(R.id.spinnerTransitionAnim)
        val spinnerDuration = findViewById<Spinner>(R.id.spinnerDurationAnim)

        val scales = arrayOf("0.1x", "0.2x", "0.3x", "0.5x", "0.7x", "1.0x")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, scales)
        
        spinnerWindow.adapter = adapter
        spinnerTransition.adapter = adapter
        spinnerDuration.adapter = adapter

        switchAnimMaster.isChecked = prefs.getBoolean("enable_anim_master", true)
        
        switchAnimMaster.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_anim_master", isChecked).apply()
            if (!isChecked) {
                CpuBoosterManager.resetAnimationsToNormal()
                Toast.makeText(this, "Animations Reset to Normal (1.0x)", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
