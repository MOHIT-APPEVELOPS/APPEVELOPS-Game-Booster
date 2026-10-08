package com.neon.gamebooster.ui

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.neon.gamebooster.R

class AnimationSettingsActivity : AppCompatActivity() {

    private val scales = arrayOf("0.1x", "0.2x", "0.3x", "0.5x", "0.7x", "1.0x")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animation_settings)

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        val switchAnim = findViewById<SwitchCompat>(R.id.switchAnimationEnable)
        val spinnerWindow = findViewById<Spinner>(R.id.spinnerWindowAnim)
        val spinnerTransition = findViewById<Spinner>(R.id.spinnerTransitionAnim)
        val spinnerAnimator = findViewById<Spinner>(R.id.spinnerAnimatorAnim)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, scales)
        spinnerWindow.adapter = adapter
        spinnerTransition.adapter = adapter
        spinnerAnimator.adapter = adapter

        switchAnim.isChecked = prefs.getBoolean("enable_animation_settings", true)

        val savedWin = prefs.getString("scale_window", "0.1x")
        val savedTrans = prefs.getString("scale_transition", "0.1x")
        val savedAnim = prefs.getString("scale_animator", "0.1x")

        spinnerWindow.setSelection(scales.indexOf(savedWin).coerceAtLeast(0))
        spinnerTransition.setSelection(scales.indexOf(savedTrans).coerceAtLeast(0))
        spinnerAnimator.setSelection(scales.indexOf(savedAnim).coerceAtLeast(0))

        switchAnim.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_animation_settings", isChecked).apply()
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val spinnerWindow = findViewById<Spinner>(R.id.spinnerWindowAnim)
        val spinnerTransition = findViewById<Spinner>(R.id.spinnerTransitionAnim)
        val spinnerAnimator = findViewById<Spinner>(R.id.spinnerAnimatorAnim)

        prefs.edit()
            .putString("scale_window", scales[spinnerWindow.selectedItemPosition])
            .putString("scale_transition", scales[spinnerTransition.selectedItemPosition])
            .putString("scale_animator", scales[spinnerAnimator.selectedItemPosition])
            .apply()
    }
}
