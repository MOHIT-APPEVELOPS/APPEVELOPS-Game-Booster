package com.neon.gamebooster.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.neon.main.R // Apne package ke mutabiq adjust karein

class SelectedAppsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_apps) // Nayi XML layout window

        val btnLaunchGame = findViewById<Button>(R.id.btnLaunchSelectedApp)
        
        btnLaunchGame?.setOnClickListener {
            // 1. Saari enabled services ko background mein trigger karein
            // 2. Uske baad target game/app ko package manager ke zariye launch karein
            launchTargetGame("com.tencent.ig") // Example package for BGMI
        }
    }

    private fun launchTargetGame(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            } else {
                // Agar app installed nahi hai
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
