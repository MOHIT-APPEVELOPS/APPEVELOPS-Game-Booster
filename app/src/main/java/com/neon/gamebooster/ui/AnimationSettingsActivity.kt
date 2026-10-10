package com.neon.gamebooster.ui

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.neon.gamebooster.R
import com.neon.gamebooster.utils.CpuBoosterManager
import rikka.shizuku.Shizuku

class AnimationSettingsActivity : AppCompatActivity() {

    private val scales = arrayOf("0.1x", "0.2x", "0.3x", "0.5x", "0.7x", "1.0x")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_animation_settings)

        // Top-left Back Arrow Button click listener
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)

        val switchAnim = findViewById<Switch>(R.id.switchAnimationEnable)
        val spinnerWindow = findViewById<Spinner>(R.id.spinnerWindowAnim)
        val spinnerTransition = findViewById<Spinner>(R.id.spinnerTransitionAnim)
        val spinnerAnimator = findViewById<Spinner>(R.id.spinnerAnimatorAnim)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, scales)
        spinnerWindow?.adapter = adapter
        spinnerTransition?.adapter = adapter
        spinnerAnimator?.adapter = adapter

        switchAnim?.isChecked = prefs.getBoolean("enable_animation_settings", true)

        val savedWin = prefs.getString("scale_window", "0.1x")
        val savedTrans = prefs.getString("scale_transition", "0.1x")
        val savedAnim = prefs.getString("scale_animator", "0.1x")

        spinnerWindow?.setSelection(scales.indexOf(savedWin).coerceAtLeast(0))
        spinnerTransition?.setSelection(scales.indexOf(savedTrans).coerceAtLeast(0))
        spinnerAnimator?.setSelection(scales.indexOf(savedAnim).coerceAtLeast(0))

        switchAnim?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("enable_animation_settings", isChecked).apply()
        }
    }

    override fun onPause() {
        super.onPause()
        val prefs = getSharedPreferences("GameBoosterPrefs", Context.MODE_PRIVATE)
        val spinnerWindow = findViewById<Spinner>(R.id.spinnerWindowAnim)
        val spinnerTransition = findViewById<Spinner>(R.id.spinnerTransitionAnim)
        val spinnerAnimator = findViewById<Spinner>(R.id.spinnerAnimatorAnim)

        if (spinnerWindow != null && spinnerTransition != null && spinnerAnimator != null) {
            val winScaleStr = scales[spinnerWindow.selectedItemPosition]
            val transScaleStr = scales[spinnerTransition.selectedItemPosition]
            val animScaleStr = scales[spinnerAnimator.selectedItemPosition]

            prefs.edit()
                .putString("scale_window", winScaleStr)
                .putString("scale_transition", transScaleStr)
                .putString("scale_animator", animScaleStr)
                .apply()

            // Apply scales via Shizuku if enabled
            val isAnimEnabled = prefs.getBoolean("enable_animation_settings", true)
            if (isAnimEnabled) {
                val winFloat = winScaleStr.replace("x", "").toFloatOrNull() ?: 1.0f
                val transFloat = transScaleStr.replace("x", "").toFloatOrNull() ?: 1.0f
                val animFloat = animScaleStr.replace("x", "").toFloatOrNull() ?: 1.0f
                
                applyScalesViaShizuku(winFloat, transFloat, animFloat)
            }
        }
    }

    private fun applyScalesViaShizuku(window: Float, transition: Float, animator: Float) {
        Thread {
            try {
                if (CpuBoosterManager.isShizukuAvailableAndGranted()) {
                    executeShizukuCommand("settings put global window_animation_scale $window")
                    executeShizukuCommand("settings put global transition_animation_scale $transition")
                    executeShizukuCommand("settings put global animator_duration_scale $animator")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun executeShizukuCommand(command: String) {
        try {
            // Using Reflection to bypass Shizuku's private newProcess visibility check
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(
                null,
                arrayOf("sh", "-c", command),
                null,
                null
            ) as? Process
            process?.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
