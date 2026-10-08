package com.neon.gamebooster.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.neon.gamebooster.R // Yeh correct import hona zaroori hai

class SelectedAppsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selected_apps)

        val btnLaunchGame = findViewById<Button>(R.id.btnLaunchSelectedApp)
        
        btnLaunchGame?.setOnClickListener {
            // Target game launch logic (jaise BGMI ya selected game)
            launchTargetGame("com.tencent.ig") 
        }
    }

    private fun launchTargetGame(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
